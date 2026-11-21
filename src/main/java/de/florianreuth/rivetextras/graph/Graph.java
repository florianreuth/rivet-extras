/*
 * This file is part of rivet-extras - https://github.com/florianreuth/rivet-extras
 * Copyright (C) 2026 Florian Reuth <git@florianreuth.de>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.florianreuth.rivetextras.graph;

import de.florianreuth.rivetextras.theme.ExtrasTheme;
import de.florianreuth.rivetextras.utils.AxisUtils;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.Font;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Point;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.property.BooleanProperty;
import net.lenni0451.rivet.property.ObjectProperty;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleFunction;

import static de.florianreuth.rivetextras.utils.AxisUtils.niceStep;
import static de.florianreuth.rivetextras.utils.AxisUtils.ticks;

/**
 * Line chart of one or more series, with axes, grid, legend and a tooltip for the point under the mouse.
 */
public class Graph extends Component {

    private static final Color[] DEFAULT_SERIES_COLORS = {
            Color.fromRGB(0, 122, 204),
            Color.fromRGB(233, 30, 99),
            Color.fromRGB(76, 175, 80),
            Color.fromRGB(255, 152, 0),
            Color.fromRGB(156, 39, 176),
            Color.fromRGB(0, 188, 212)
    };
    // Labels of live data change every frame, the caches are cleared before they grow unbounded
    private static final int MAX_CACHED_TEXTS = 512;

    private final List<GraphSeries> series = new CopyOnWriteArrayList<>();
    private final ObjectProperty<Range> xRange = new ObjectProperty<>();
    private final ObjectProperty<Range> yRange = new ObjectProperty<>();
    private final ObjectProperty<DoubleFunction<String>> xLabelFormatter = new ObjectProperty<>();
    private final ObjectProperty<DoubleFunction<String>> yLabelFormatter = new ObjectProperty<>();
    private final BooleanProperty showLegend = new BooleanProperty(true);

    private final ThemeOption<Float> width = new ThemeOption<>(this, ExtrasTheme.Graph.WIDTH);
    private final ThemeOption<Float> height = new ThemeOption<>(this, ExtrasTheme.Graph.HEIGHT);
    private final ThemeOption<Padding> padding = new ThemeOption<>(this, ExtrasTheme.Graph.PADDING);
    private final ThemeOption<Padding> innerPadding = new ThemeOption<>(this, ExtrasTheme.Graph.INNER_PADDING);
    private final ThemeOption<Float> tickLength = new ThemeOption<>(this, ExtrasTheme.Graph.TICK_LENGTH);
    private final ThemeOption<Float> tickSpacing = new ThemeOption<>(this, ExtrasTheme.Graph.TICK_SPACING);
    private final ThemeOption<Float> lineWidth = new ThemeOption<>(this, ExtrasTheme.Graph.LINE_WIDTH);
    private final ThemeOption<Float> gridWidth = new ThemeOption<>(this, ExtrasTheme.Graph.GRID_WIDTH);
    private final ThemeOption<Float> pointRadius = new ThemeOption<>(this, ExtrasTheme.Graph.POINT_RADIUS);
    private final ThemeOption<Float> hoverRadius = new ThemeOption<>(this, ExtrasTheme.Graph.HOVER_RADIUS);
    private final ThemeOption<Color> backgroundColor = new ThemeOption<>(this, ExtrasTheme.Graph.BACKGROUND_COLOR);
    private final ThemeOption<Color> plotBackgroundColor = new ThemeOption<>(this, ExtrasTheme.Graph.PLOT_BACKGROUND_COLOR);
    private final ThemeOption<Color> gridColor = new ThemeOption<>(this, ExtrasTheme.Graph.GRID_COLOR);
    private final ThemeOption<Color> axisColor = new ThemeOption<>(this, ExtrasTheme.Graph.AXIS_COLOR);
    private final ThemeOption<Color> labelColor = new ThemeOption<>(this, ExtrasTheme.Graph.LABEL_COLOR);
    private final ThemeOption<Color> textColor = new ThemeOption<>(this, ExtrasTheme.Graph.TEXT_COLOR);
    private final ThemeOption<Color> legendBackgroundColor = new ThemeOption<>(this, ExtrasTheme.Graph.LEGEND_BACKGROUND_COLOR);
    private final ThemeOption<Color> tooltipBackgroundColor = new ThemeOption<>(this, ExtrasTheme.Graph.TOOLTIP_BACKGROUND_COLOR);

    // Written by the input thread, the render thread does the hit testing with the layout of the current frame
    private volatile float mouseX;
    private volatile float mouseY;
    private volatile boolean hovered;
    // Only accessed from the render thread
    private final Map<GraphSeries, GraphSeries.Snapshot> snapshots = new IdentityHashMap<>();
    private final Map<String, ShapedText> labelCache = new HashMap<>();
    private final Map<String, ShapedText> textCache = new HashMap<>();
    private TextStyle textStyle;

    /**
     * @return an unmodifiable view of the series in drawing order
     */
    public final List<GraphSeries> series() {
        return Collections.unmodifiableList(this.series);
    }

    /**
     * @throws IllegalArgumentException if the series is already part of this graph
     */
    public final Graph add(final GraphSeries series) {
        if (this.series.contains(series)) {
            throw new IllegalArgumentException("Series " + series.name() + " is already part of this graph");
        }
        this.series.add(series);
        return this;
    }

    public final Graph remove(final GraphSeries series) {
        this.series.remove(series);
        return this;
    }

    /**
     * @return the visible x range, holds {@code null} to fit the range to the data
     */
    public final ObjectProperty<Range> xRange() {
        return this.xRange;
    }

    /**
     * @param range the visible x range, or null to fit the range to the data
     */
    public final Graph xRange(final Range range) {
        this.xRange.set(range);
        return this;
    }

    /**
     * @return the visible y range, holds {@code null} to fit the range to the data, rounded outwards to the next tick
     */
    public final ObjectProperty<Range> yRange() {
        return this.yRange;
    }

    /**
     * @param range the visible y range, or null to fit the range to the data, rounded outwards to the next tick
     */
    public final Graph yRange(final Range range) {
        this.yRange.set(range);
        return this;
    }

    /**
     * @return the formatter of the x tick labels and tooltip values, holds {@code null} to show as many decimals as the tick step needs
     */
    public final ObjectProperty<DoubleFunction<String>> xLabelFormatter() {
        return this.xLabelFormatter;
    }

    public final Graph xLabelFormatter(final DoubleFunction<String> formatter) {
        this.xLabelFormatter.set(formatter);
        return this;
    }

    /**
     * @return the formatter of the y tick labels and tooltip values, holds {@code null} to show as many decimals as the tick step needs
     */
    public final ObjectProperty<DoubleFunction<String>> yLabelFormatter() {
        return this.yLabelFormatter;
    }

    public final Graph yLabelFormatter(final DoubleFunction<String> formatter) {
        this.yLabelFormatter.set(formatter);
        return this;
    }

    public final BooleanProperty showLegend() {
        return this.showLegend;
    }

    public final Graph showLegend(final boolean showLegend) {
        this.showLegend.set(showLegend);
        return this;
    }

    @Override
    protected void onRemovedInternal() {
        this.hovered = false;
    }

    @Override
    protected void onDisabledInternal() {
        this.hovered = false;
    }

    @Override
    protected void onMouseLeaveInternal() {
        this.hovered = false;
    }

    @Override
    protected boolean onMouseMoveInternal(final MouseMoveEvent event, final Size size) {
        this.mouseX = event.x();
        this.mouseY = event.y();
        this.hovered = true;
        return true;
    }

    @Override
    protected void renderInternal(final Renderer renderer, final Size size, final Rectangle visibleArea) {
        this.updateTextStyle();
        final List<GraphSeries> series = List.copyOf(this.series);
        final List<GraphSeries.Snapshot> data = this.snapshots(series);
        renderer.fillRect(0, 0, size.width(), size.height(), this.backgroundColor.get());

        final Padding padding = this.padding.get();
        final float tickLength = this.tickLength.get();
        final float lineHeight = this.textStyle.font().height();
        // The labels of the outer y ticks are centered on the plot edges and stick out by half a line
        final float top = padding.top() + lineHeight / 2F;
        final float bottom = size.height() - padding.bottom() - lineHeight - tickLength;
        if (bottom <= top) {
            return;
        }

        // The y labels have to be known first, their width decides where the plot starts
        final Axis yAxis = this.yAxis(data, bottom - top);
        float yLabelWidth = 0;
        for (final double tick : yAxis.ticks()) {
            yLabelWidth = Math.max(yLabelWidth, this.label(yAxis.format(tick)).visualBounds().width());
        }
        final float left = padding.left() + yLabelWidth + tickLength;
        final float right = size.width() - padding.right();
        if (right <= left) {
            return;
        }

        final Axis xAxis = this.xAxis(data, right - left);
        final Plot plot = new Plot(left, top, right, bottom, xAxis.range(), yAxis.range());
        renderer.fillRect(left, top, right - left, bottom - top, this.plotBackgroundColor.get());
        this.renderAxes(renderer, size, plot, xAxis, yAxis, tickLength);
        renderer.scissor(left, top, right - left, bottom - top, () -> {
            for (int i = 0; i < data.size(); i++) {
                this.renderSeries(renderer, plot, data.get(i), this.seriesColor(series.get(i), i));
            }
        });
        if (this.showLegend.get() && !series.isEmpty()) {
            this.renderLegend(renderer, plot, series, lineHeight);
        }
        final Hover hover = this.findHover(data, plot);
        if (hover != null) {
            this.renderHover(renderer, size, series.get(hover.series()), hover, data.get(hover.series()), xAxis, yAxis);
        }
    }

    private void renderAxes(final Renderer renderer, final Size size, final Plot plot, final Axis xAxis, final Axis yAxis, final float tickLength) {
        final float gridWidth = this.gridWidth.get();
        final Color gridColor = this.gridColor.get();
        final Color axisColor = this.axisColor.get();
        for (final double tick : yAxis.ticks()) {
            final float y = plot.screenY(tick);
            renderer.fillRect(plot.left(), y - gridWidth / 2F, plot.width(), gridWidth, gridColor);
            renderer.fillRect(plot.left() - tickLength, y - gridWidth / 2F, tickLength, gridWidth, axisColor);
            renderer.text(this.label(yAxis.format(tick)), plot.left() - tickLength, y, TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        }
        for (final double tick : xAxis.ticks()) {
            final float x = plot.screenX(tick);
            renderer.fillRect(x - gridWidth / 2F, plot.top(), gridWidth, plot.height(), gridColor);
            renderer.fillRect(x - gridWidth / 2F, plot.bottom(), gridWidth, tickLength, axisColor);
            final ShapedText label = this.label(xAxis.format(tick));
            final float halfWidth = label.visualBounds().width() / 2F;
            // Labels at the plot edges are moved inwards instead of being cut off by the component bounds
            final float labelX = Math.max(halfWidth, Math.min(x, size.width() - halfWidth));
            renderer.text(label, labelX, plot.bottom() + tickLength, TextOrigin.Horizontal.VISUAL_CENTER, TextOrigin.Vertical.LOGICAL_TOP);
        }
        // Drawn over the grid, the outer grid lines would otherwise cover half of the axes
        renderer.fillRect(plot.left() - gridWidth / 2F, plot.top(), gridWidth, plot.height(), axisColor);
        renderer.fillRect(plot.left(), plot.bottom() - gridWidth / 2F, plot.width(), gridWidth, axisColor);
    }

    private void renderSeries(final Renderer renderer, final Plot plot, final GraphSeries.Snapshot data, final Color color) {
        final double[] xs = data.xs();
        final double[] ys = data.ys();
        if (xs.length == 1) {
            renderer.fillCircle(plot.screenX(xs[0]), plot.screenY(ys[0]), this.pointRadius.get(), color);
        } else if (xs.length > 1) {
            final Point[] points = new Point[xs.length];
            for (int i = 0; i < xs.length; i++) {
                points[i] = new Point(plot.screenX(xs[i]), plot.screenY(ys[i]));
            }
            renderer.polyLine(points, this.lineWidth.get(), color);
        }
    }

    private void renderLegend(final Renderer renderer, final Plot plot, final List<GraphSeries> series, final float lineHeight) {
        final Padding innerPadding = this.innerPadding.get();
        final float swatchSize = lineHeight * 0.6F;
        final float gap = innerPadding.left();
        float nameWidth = 0;
        for (final GraphSeries entry : series) {
            nameWidth = Math.max(nameWidth, this.text(entry.name()).logicalBounds().width());
        }

        final float x = plot.left() + this.tickLength.get();
        final float y = plot.top() + this.tickLength.get();
        final float width = swatchSize + gap + nameWidth + innerPadding.horizontal();
        final float height = series.size() * lineHeight + innerPadding.vertical();
        renderer.fillRect(x, y, width, height, this.legendBackgroundColor.get());
        for (int i = 0; i < series.size(); i++) {
            final float rowY = y + innerPadding.top() + i * lineHeight;
            renderer.fillRect(x + innerPadding.left(), rowY + (lineHeight - swatchSize) / 2F, swatchSize, swatchSize, this.seriesColor(series.get(i), i));
            renderer.text(this.text(series.get(i).name()), x + innerPadding.left() + swatchSize + gap, rowY, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_TOP);
        }
    }

    private void renderHover(final Renderer renderer, final Size size, final GraphSeries series, final Hover hover, final GraphSeries.Snapshot data, final Axis xAxis, final Axis yAxis) {
        final float pointRadius = this.pointRadius.get();
        renderer.fillCircle(hover.x(), hover.y(), pointRadius, this.seriesColor(series, hover.series()));

        // The tooltip shows one decimal more than the ticks, the hovered point is usually between two ticks
        final String text = series.name() + ": " + xAxis.formatPrecise(data.xs()[hover.point()]) + ", " + yAxis.formatPrecise(data.ys()[hover.point()]);
        final ShapedText shapedText = this.text(text);
        final Padding innerPadding = this.innerPadding.get();
        final float width = shapedText.logicalBounds().width() + innerPadding.horizontal();
        final float height = shapedText.logicalBounds().height() + innerPadding.vertical();
        final float x = Math.max(0, Math.min(hover.x() + pointRadius * 2, size.width() - width));
        final float y = Math.max(0, Math.min(hover.y() - pointRadius * 2 - height, size.height() - height));
        renderer.fillRect(x, y, width, height, this.tooltipBackgroundColor.get());
        renderer.text(shapedText, x + innerPadding.left(), y + innerPadding.top(), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_TOP);
    }

    /**
     * @return the point closest to the mouse within the hover radius, or null if there is none
     */
    private Hover findHover(final List<GraphSeries.Snapshot> data, final Plot plot) {
        final float mouseX = this.mouseX;
        final float mouseY = this.mouseY;
        if (!this.hovered || mouseX < plot.left() || mouseX > plot.right() || mouseY < plot.top() || mouseY > plot.bottom()) {
            return null;
        }

        final float hoverRadius = this.hoverRadius.get();
        float closestDistance = hoverRadius * hoverRadius;
        Hover closest = null;
        for (int i = 0; i < data.size(); i++) {
            final double[] xs = data.get(i).xs();
            final double[] ys = data.get(i).ys();
            for (int j = 0; j < xs.length; j++) {
                final float x = plot.screenX(xs[j]);
                final float y = plot.screenY(ys[j]);
                final float distance = (x - mouseX) * (x - mouseX) + (y - mouseY) * (y - mouseY);
                if (distance <= closestDistance) {
                    closestDistance = distance;
                    closest = new Hover(i, j, x, y);
                }
            }
        }
        return closest;
    }

    private Axis yAxis(final List<GraphSeries.Snapshot> data, final float length) {
        final Range fixedRange = this.yRange.get();
        Range range = fixedRange != null ? fixedRange : dataRange(data, false);
        final double step = niceStep(range.span(), maxTicks(length, this.tickSpacing.get()));
        if (fixedRange == null) {
            range = new Range(Math.floor(range.min() / step) * step, Math.ceil(range.max() / step) * step);
        }
        return new Axis(range, step, ticks(range.min(), range.max(), step), this.yLabelFormatter.get());
    }

    private Axis xAxis(final List<GraphSeries.Snapshot> data, final float length) {
        final Range fixedRange = this.xRange.get();
        // The x range is not rounded, live data would otherwise jump between ticks
        final Range range = fixedRange != null ? fixedRange : dataRange(data, true);
        final double step = niceStep(range.span(), maxTicks(length, this.tickSpacing.get()));
        final Axis axis = new Axis(range, step, ticks(range.min(), range.max(), step), this.xLabelFormatter.get());

        // Wide labels like large numbers need more space than the tick spacing, the step is widened until they fit
        float labelWidth = 0;
        for (final double tick : axis.ticks()) {
            labelWidth = Math.max(labelWidth, this.label(axis.format(tick)).visualBounds().width());
        }
        final float minSpacing = labelWidth * 1.5F;
        if (minSpacing <= this.tickSpacing.get() || step / range.span() * length >= minSpacing) {
            return axis;
        }
        final double widerStep = niceStep(range.span(), maxTicks(length, minSpacing));
        return new Axis(range, widerStep, ticks(range.min(), range.max(), widerStep), this.xLabelFormatter.get());
    }

    private List<GraphSeries.Snapshot> snapshots(final List<GraphSeries> series) {
        this.snapshots.keySet().retainAll(series);
        final List<GraphSeries.Snapshot> data = new ArrayList<>(series.size());
        for (final GraphSeries entry : series) {
            GraphSeries.Snapshot snapshot = this.snapshots.get(entry);
            if (snapshot == null || snapshot.version() != entry.version()) {
                snapshot = entry.snapshot();
                this.snapshots.put(entry, snapshot);
            }
            data.add(snapshot);
        }
        return data;
    }

    private Color seriesColor(final GraphSeries series, final int index) {
        final Color color = series.color();
        return color != null ? color : DEFAULT_SERIES_COLORS[index % DEFAULT_SERIES_COLORS.length];
    }

    private ShapedText label(final String text) {
        return shape(this.labelCache, text, this.textStyle.labelColor());
    }

    private ShapedText text(final String text) {
        return shape(this.textCache, text, this.textStyle.textColor());
    }

    private ShapedText shape(final Map<String, ShapedText> cache, final String text, final Color color) {
        ShapedText shapedText = cache.get(text);
        if (shapedText == null) {
            if (cache.size() >= MAX_CACHED_TEXTS) {
                cache.clear();
            }
            shapedText = this.textStyle.font().shapeText(text, color);
            cache.put(text, shapedText);
        }
        return shapedText;
    }

    private void updateTextStyle() {
        final TextStyle style = new TextStyle(this.rivet().backend().font(), this.labelColor.get(), this.textColor.get());
        if (!style.equals(this.textStyle)) {
            this.textStyle = style;
            this.labelCache.clear();
            this.textCache.clear();
        }
    }

    @Override
    protected Size idealSizeInternal(final Size constraints) {
        return new Size(this.width.get(), this.height.get());
    }

    /**
     * @return the range of the x or y values of all points, widened if it would be empty
     */
    private static Range dataRange(final List<GraphSeries.Snapshot> data, final boolean x) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (final GraphSeries.Snapshot snapshot : data) {
            for (final double value : x ? snapshot.xs() : snapshot.ys()) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        if (min > max) {
            return new Range(0, 1);
        }
        if (min == max) {
            final double margin = min == 0 ? 1 : Math.abs(min) * 0.1;
            return new Range(min - margin, max + margin);
        }
        return new Range(min, max);
    }

    private static int maxTicks(final float length, final float spacing) {
        return Math.max(1, (int) (length / Math.max(1, spacing)));
    }

    public final ThemeOption<Float> width() {
        return this.width;
    }

    public final ThemeOption<Float> height() {
        return this.height;
    }

    public final ThemeOption<Padding> padding() {
        return this.padding;
    }

    public final ThemeOption<Padding> innerPadding() {
        return this.innerPadding;
    }

    public final ThemeOption<Float> tickLength() {
        return this.tickLength;
    }

    public final ThemeOption<Float> tickSpacing() {
        return this.tickSpacing;
    }

    public final ThemeOption<Float> lineWidth() {
        return this.lineWidth;
    }

    public final ThemeOption<Float> gridWidth() {
        return this.gridWidth;
    }

    public final ThemeOption<Float> pointRadius() {
        return this.pointRadius;
    }

    public final ThemeOption<Float> hoverRadius() {
        return this.hoverRadius;
    }

    public final ThemeOption<Color> backgroundColor() {
        return this.backgroundColor;
    }

    public final ThemeOption<Color> plotBackgroundColor() {
        return this.plotBackgroundColor;
    }

    public final ThemeOption<Color> gridColor() {
        return this.gridColor;
    }

    public final ThemeOption<Color> axisColor() {
        return this.axisColor;
    }

    public final ThemeOption<Color> labelColor() {
        return this.labelColor;
    }

    public final ThemeOption<Color> textColor() {
        return this.textColor;
    }

    public final ThemeOption<Color> legendBackgroundColor() {
        return this.legendBackgroundColor;
    }

    public final ThemeOption<Color> tooltipBackgroundColor() {
        return this.tooltipBackgroundColor;
    }


    /**
     * @param min the lower bound, inclusive
     * @param max the upper bound, inclusive
     */
    public record Range(double min, double max) {
        /**
         * @throws IllegalArgumentException if a bound is not finite or min is not below max
         */
        public Range {
            if (!Double.isFinite(min) || !Double.isFinite(max) || min >= max) {
                throw new IllegalArgumentException("Invalid range " + min + " - " + max);
            }
        }

        public double span() {
            return this.max - this.min;
        }
    }

    private record TextStyle(Font font, Color labelColor, Color textColor) {
    }

    /**
     * @param series the index of the series
     * @param point  the index of the point in the series
     */
    private record Hover(int series, int point, float x, float y) {
    }

    /**
     * @param formatter the custom label formatter, or null to format with the decimals of the step
     */
    private record Axis(Range range, double step, double[] ticks, DoubleFunction<String> formatter) {
        private String format(final double value) {
            return this.formatter != null ? this.formatter.apply(value) : AxisUtils.format(value, this.step);
        }

        private String formatPrecise(final double value) {
            return this.formatter != null ? this.formatter.apply(value) : AxisUtils.format(value, this.step / 10);
        }
    }

    private record Plot(float left, float top, float right, float bottom, Range x, Range y) {
        private float width() {
            return this.right - this.left;
        }

        private float height() {
            return this.bottom - this.top;
        }

        private float screenX(final double x) {
            return (float) (this.left + (x - this.x.min()) / this.x.span() * this.width());
        }

        private float screenY(final double y) {
            return (float) (this.bottom - (y - this.y.min()) / this.y.span() * this.height());
        }
    }

}
