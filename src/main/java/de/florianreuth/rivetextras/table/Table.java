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

package de.florianreuth.rivetextras.table;

import de.florianreuth.rivetextras.theme.ExtrasTheme;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ParentContainer;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.DynamicListContainer;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.property.ObjectProperty;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static net.lenni0451.rivet.utils.MathUtils.relativizeVisibleArea;

/**
 * Table with a sticky header and sortable, resizable columns.
 *
 * @param <E> the row type
 */
public class Table<E> extends ParentContainer {

    private final Supplier<List<E>> dataSupplier;
    private final List<TableColumn<E>> columns;
    private final TableHeader<E> header;
    private final DynamicListContainer<E> rows;
    private final ScrollContainer body;
    private final ObjectProperty<TableColumn<E>> sortColumn = new ObjectProperty<>();
    private final ObjectProperty<SortOrder> sortOrder = new ObjectProperty<>(SortOrder.ASCENDING);

    private final ThemeOption<Padding> cellPadding = new ThemeOption<>(this, ExtrasTheme.Table.CELL_PADDING);
    private final ThemeOption<Float> gridWidth = new ThemeOption<>(this, ExtrasTheme.Table.GRID_WIDTH);
    private final ThemeOption<Float> resizeHandleWidth = new ThemeOption<>(this, ExtrasTheme.Table.RESIZE_HANDLE_WIDTH);
    private final ThemeOption<Float> minColumnWidth = new ThemeOption<>(this, ExtrasTheme.Table.MIN_COLUMN_WIDTH);
    private final ThemeOption<Color> headerBackgroundColor = new ThemeOption<>(this, ExtrasTheme.Table.HEADER_BACKGROUND_COLOR);
    private final ThemeOption<Color> headerHoverBackgroundColor = new ThemeOption<>(this, ExtrasTheme.Table.HEADER_HOVER_BACKGROUND_COLOR);
    private final ThemeOption<Color> rowBackgroundColor = new ThemeOption<>(this, ExtrasTheme.Table.ROW_BACKGROUND_COLOR);
    private final ThemeOption<Color> rowAlternateBackgroundColor = new ThemeOption<>(this, ExtrasTheme.Table.ROW_ALTERNATE_BACKGROUND_COLOR);
    private final ThemeOption<Color> rowHoverBackgroundColor = new ThemeOption<>(this, ExtrasTheme.Table.ROW_HOVER_BACKGROUND_COLOR);
    private final ThemeOption<Color> gridColor = new ThemeOption<>(this, ExtrasTheme.Table.GRID_COLOR);
    private final ThemeOption<Color> resizeHandleColor = new ThemeOption<>(this, ExtrasTheme.Table.RESIZE_HANDLE_COLOR);
    private final ThemeOption<Long> doubleClickTime = new ThemeOption<>(this, ExtrasTheme.Table.DOUBLE_CLICK_TIME);

    private List<E> lastData = List.of();
    private List<E> view = List.of();
    private final Map<E, Integer> viewIndices = new IdentityHashMap<>();
    private boolean viewDirty = true;
    private float layoutWidth;
    private float headerWidth;
    private float headerHeight;

    public Table(final List<E> data, final List<TableColumn<E>> columns) {
        this(() -> data, columns);
    }

    /**
     * @param dataSupplier supplies the rows, polled every frame and compared by identity
     * @param columns      the columns, fixed for the lifetime of the table
     * @throws IllegalArgumentException if no columns are given
     */
    public Table(final Supplier<List<E>> dataSupplier, final List<TableColumn<E>> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("A table requires at least one column");
        }
        this.dataSupplier = dataSupplier;
        this.columns = List.copyOf(columns);
        this.header = new TableHeader<>(this);
        this.rows = new DynamicListContainer<>(new VerticalListLayout(0, true), this::view, item -> new TableRow<>(this, item));
        this.body = new ScrollContainer(this.rows, true, true);

        this.sortColumn.addValidator(column -> {
            if (column != null && !this.columns.contains(column)) {
                throw new IllegalArgumentException("Column '" + column.title() + "' is not part of this table");
            }
            if (column != null && !column.sortable()) {
                throw new IllegalArgumentException("Column '" + column.title() + "' is not sortable");
            }
            return column;
        });
        this.sortOrder.addValidator(Objects::requireNonNull);
        this.sortColumn.updateListener().add(column -> this.onSortChanged());
        this.sortOrder.updateListener().add(order -> this.onSortChanged());

        for (final TableColumn<E> column : this.columns) {
            column.width().updateListener().add(width -> this.requestLayoutRecalculation());
        }
        this.cellPadding.changeListener().add(padding -> this.requestLayoutRecalculation());
        this.minColumnWidth.changeListener().add(width -> this.requestLayoutRecalculation());
    }

    public final List<TableColumn<E>> columns() {
        return this.columns;
    }

    /**
     * @return the scroll container holding the rows, used to control the scroll position
     */
    public final ScrollContainer body() {
        return this.body;
    }

    /**
     * @return the column the rows are sorted by, holds {@code null} if unsorted
     */
    public final ObjectProperty<TableColumn<E>> sortColumn() {
        return this.sortColumn;
    }

    public final ObjectProperty<SortOrder> sortOrder() {
        return this.sortOrder;
    }

    /**
     * @return the rows in display order, rebuilt when the data or the sorting changed
     */
    public final List<E> view() {
        final List<E> data = this.dataSupplier.get();
        if (this.viewDirty || !isSame(data, this.lastData)) {
            this.lastData = new ArrayList<>(data);
            final List<E> view = new ArrayList<>(data);
            final TableColumn<E> sortColumn = this.sortColumn.get();
            if (sortColumn != null) {
                final Comparator<E> comparator = sortColumn.comparator();
                view.sort(this.sortOrder.get() == SortOrder.ASCENDING ? comparator : comparator.reversed());
            }
            this.view = Collections.unmodifiableList(view);
            this.viewIndices.clear();
            for (int i = 0; i < view.size(); i++) {
                this.viewIndices.put(view.get(i), i);
            }
            this.viewDirty = false;
        }
        return this.view;
    }

    /**
     * @param column the column to sort by, {@code null} restores the data order
     * @throws IllegalArgumentException if the column is not part of this table or not sortable
     */
    public final Table<E> sort(final TableColumn<E> column, final SortOrder order) {
        this.sortColumn.set(column);
        this.sortOrder.set(order);
        return this;
    }

    public final Table<E> unsort() {
        this.sortColumn.set(null);
        return this;
    }

    /**
     * Re-applies the sorting, required after sort relevant values changed without the data list changing.
     */
    public final Table<E> refresh() {
        this.viewDirty = true;
        return this;
    }

    /**
     * Recreates the cells of a row, required after displayed values of the row changed.
     */
    public final Table<E> refresh(final E item) {
        this.rows.recreate(item);
        return this.refresh();
    }

    /**
     * Resizes the column to fit its header and all cells, taking the space from the next resizable column.
     *
     * @param column the column to resize, has no effect if it has no resizable column to its right
     * @throws IllegalArgumentException if the column is not part of this table
     */
    public final Table<E> autoSizeColumn(final TableColumn<E> column) {
        final int index = this.columns.indexOf(column);
        if (index < 0) {
            throw new IllegalArgumentException("Column '" + column.title() + "' is not part of this table");
        }
        float width = idealWidth(this.header.children().get(index));
        for (final Component row : this.rows.children()) {
            width = Math.max(width, idealWidth(((TableRow<?>) row).children().get(index)));
        }
        this.resizeColumn(index, this.columnWidths(this.headerWidth), width + this.cellPadding.value().horizontal());
        return this;
    }

    final void cycleSort(final TableColumn<E> column) {
        if (this.sortColumn.get() != column) {
            this.sort(column, SortOrder.ASCENDING);
        } else if (this.sortOrder.get() == SortOrder.ASCENDING) {
            this.sort(column, SortOrder.DESCENDING);
        } else {
            this.unsort();
        }
    }

    private void onSortChanged() {
        this.viewDirty = true;
        this.header.updateSortIndicators();
    }

    /**
     * @return the width of the last layout, 0 before the table was laid out
     */
    final float layoutWidth() {
        return this.layoutWidth;
    }

    final int viewIndex(final E item) {
        return this.viewIndices.getOrDefault(item, -1);
    }

    private float minColumnWidth(final TableColumn<E> column) {
        return column.minWidth() < 0 ? this.minColumnWidth.value() : column.minWidth();
    }

    /**
     * @param index the column whose right edge is dragged
     * @return the column giving or taking the width, or -1 if the edge can not be dragged
     */
    final int resizeNeighbour(final int index) {
        if (!this.columns.get(index).resizable()) {
            return -1;
        }

        for (int i = index + 1; i < this.columns.size(); i++) {
            if (this.columns.get(i).resizable()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Moves the right edge of a column, the width is taken from or given to its resize neighbour so the table width stays the same.
     *
     * @param index  the column to resize
     * @param widths the current pixel widths of all columns
     * @param width  the requested pixel width, clamped to the minimum widths of both columns
     */
    final void resizeColumn(final int index, final float[] widths, final float width) {
        final int neighbour = this.resizeNeighbour(index);
        if (neighbour < 0) {
            return;
        }

        final float pairWidth = widths[index] + widths[neighbour];
        final float minWidth = this.minColumnWidth(this.columns.get(index));
        final float maxWidth = pairWidth - this.minColumnWidth(this.columns.get(neighbour));
        if (maxWidth < minWidth) {
            return;
        }

        final float[] newWidths = widths.clone();
        newWidths[index] = MathUtils.clamp(width, minWidth, maxWidth);
        newWidths[neighbour] = pairWidth - newWidths[index];
        float totalWidth = 0;
        for (final float newWidth : newWidths) {
            totalWidth += newWidth;
        }
        // All columns get a fixed fraction, unset columns would otherwise redistribute the remaining space and shift the other edges
        for (int i = 0; i < newWidths.length; i++) {
            this.columns.get(i).width(newWidths[i] / totalWidth);
        }
    }

    /**
     * @return the share of the row width of every column in column order, sums up to 1
     */
    final float[] columnFractions() {
        final int count = this.columns.size();
        final float[] fractions = new float[count];
        float setFraction = 0;
        int unsetCount = 0;
        for (int i = 0; i < count; i++) {
            fractions[i] = this.columns.get(i).width().get();
            if (fractions[i] < 0) {
                unsetCount++;
            } else {
                setFraction += fractions[i];
            }
        }
        final float unsetFraction = unsetCount == 0 ? 0 : Math.max(0, 1 - setFraction) / unsetCount;
        float totalFraction = 0;
        for (int i = 0; i < count; i++) {
            if (fractions[i] < 0) {
                fractions[i] = unsetFraction;
            }
            totalFraction += fractions[i];
        }
        if (totalFraction <= 0) {
            Arrays.fill(fractions, 1F / count);
            return fractions;
        }
        // Fractions are relative to their total so the columns always fill the row
        for (int i = 0; i < count; i++) {
            fractions[i] /= totalFraction;
        }
        return fractions;
    }

    /**
     * @param availableWidth the width of the row the columns are distributed over
     * @return the pixel width of every column in column order, exceeds the available width only if the minimum widths do not fit
     */
    final float[] columnWidths(final float availableWidth) {
        final int count = this.columns.size();
        final float[] fractions = this.columnFractions();

        // Columns below their minimum are fixed and the rest is redistributed
        final float[] widths = new float[count];
        final boolean[] fixed = new boolean[count];
        float remainingWidth = availableWidth;
        float remainingFraction = 1;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < count; i++) {
                if (fixed[i]) {
                    continue;
                }
                final float minWidth = this.minColumnWidth(this.columns.get(i));
                final float width = remainingFraction <= 0 ? 0 : remainingWidth * fractions[i] / remainingFraction;
                if (width < minWidth) {
                    widths[i] = minWidth;
                    fixed[i] = true;
                    remainingWidth -= minWidth;
                    remainingFraction -= fractions[i];
                    changed = true;
                } else {
                    widths[i] = width;
                }
            }
        }
        return widths;
    }

    final boolean isLastRow(final E item) {
        return this.viewIndex(item) == this.view.size() - 1;
    }

    final void renderGrid(final Renderer renderer, final Size size, final boolean bottomLine) {
        final float gridWidth = this.gridWidth.value();
        if (gridWidth <= 0) {
            return;
        }

        final Color color = this.gridColor.value();
        float x = 0;
        for (final float width : this.columnWidths(size.width())) {
            x += width;
            if (x < size.width()) {
                renderer.fillRect(x - gridWidth / 2F, 0, gridWidth, size.height(), color);
            }
        }
        if (bottomLine) {
            renderer.fillRect(0, size.height() - gridWidth, size.width(), gridWidth, color);
        }
    }

    /**
     * Renders the cells of a row or the header, each clipped to its column.
     *
     * @param row the container holding one cell per column
     */
    final void renderCells(final Renderer renderer, final Container row, final Rectangle visibleArea) {
        for (final Component cell : row.children()) {
            final Rectangle bounds = row.childBounds(cell);
            if (bounds.intersection(visibleArea).equals(Rectangle.EMPTY)) {
                cell.updatePosition();
                continue;
            }
            renderer.translate(bounds.x(), bounds.y(), () -> {
                renderer.scissor(0, 0, bounds.width(), bounds.height(), () -> {
                    cell.render(renderer, bounds.size(), relativizeVisibleArea(visibleArea, bounds));
                });
            });
        }
    }

    @Override
    protected void renderInternal(final Renderer renderer, final Size size, final Rectangle visibleArea) {
        final Rectangle headerBounds = this.childBounds(this.header);
        renderer.scissor(0, 0, size.width(), this.headerHeight, () -> {
            renderer.translate(headerBounds.x(), headerBounds.y(), () -> {
                this.header.render(renderer, headerBounds.size(), relativizeVisibleArea(visibleArea.intersection(new Rectangle(0, 0, size.width(), this.headerHeight)), headerBounds));
            });
        });
        final Rectangle bodyBounds = this.childBounds(this.body);
        renderer.translate(bodyBounds.x(), bodyBounds.y(), () -> {
            renderer.scissor(0, 0, bodyBounds.width(), bodyBounds.height(), () -> {
                this.body.render(renderer, bodyBounds.size(), relativizeVisibleArea(visibleArea, bodyBounds));
            });
        });
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        final Size headerSize = this.header.computeIdealSize(constraints).clamp(this.header);
        final Size bodySize = this.body.computeIdealSize(constraints.minus(0, headerSize.height())).clamp(this.body);
        // The rows only report their minimum widths so they never overflow the body, parents sizing to the ideal width need the width the content fits into
        final float[] fractions = this.columnFractions();
        float fitWidth = this.fitWidth(this.header, fractions);
        for (final Component row : this.rows.children()) {
            fitWidth = Math.max(fitWidth, this.fitWidth((Container) row, fractions));
        }
        return new Size(Math.max(fitWidth, Math.max(headerSize.width(), bodySize.width())), headerSize.height() + bodySize.height());
    }

    private float fitWidth(final Container row, final float[] fractions) {
        final float horizontalPadding = this.cellPadding.value().horizontal();
        float fitWidth = 0;
        final List<Component> cells = row.children();
        for (int i = 0; i < cells.size(); i++) {
            if (fractions[i] > 0) {
                fitWidth = Math.max(fitWidth, (idealWidth(cells.get(i)) + horizontalPadding) / fractions[i]);
            }
        }
        return fitWidth;
    }

    @Override
    public void computeLayout(final Size size) {
        // Has to be set first, the row heights are measured with the column widths of this width
        this.layoutWidth = size.width();
        this.headerHeight = Math.min(size.height(), this.header.computeIdealSize(size).clamp(this.header).height());
        this.body.computeLayout(new Size(size.width(), size.height() - this.headerHeight));
        // The header has to match the row width so the columns line up
        this.headerWidth = this.body.childBounds(this.rows).width();
        this.header.computeLayout(new Size(this.headerWidth, this.headerHeight));
    }

    @Override
    public Size contentSize() {
        return Size.EMPTY;
    }

    @Override
    public List<Component> children() {
        return List.of(this.header, this.body);
    }

    @Override
    public Rectangle childBounds(final Component component) {
        if (component == this.header) {
            return new Rectangle(-this.body.scrollX().get(), 0, this.headerWidth, this.headerHeight);
        } else if (component == this.body) {
            final Size size = this.relativeBounds().size();
            return new Rectangle(0, this.headerHeight, size.width(), size.height() - this.headerHeight);
        }
        return Rectangle.EMPTY;
    }

    public final ThemeOption<Padding> cellPadding() {
        return this.cellPadding;
    }

    public final ThemeOption<Float> gridWidth() {
        return this.gridWidth;
    }

    public final ThemeOption<Float> resizeHandleWidth() {
        return this.resizeHandleWidth;
    }

    public final ThemeOption<Float> minColumnWidth() {
        return this.minColumnWidth;
    }

    public final ThemeOption<Color> headerBackgroundColor() {
        return this.headerBackgroundColor;
    }

    public final ThemeOption<Color> headerHoverBackgroundColor() {
        return this.headerHoverBackgroundColor;
    }

    public final ThemeOption<Color> rowBackgroundColor() {
        return this.rowBackgroundColor;
    }

    public final ThemeOption<Color> rowAlternateBackgroundColor() {
        return this.rowAlternateBackgroundColor;
    }

    public final ThemeOption<Color> rowHoverBackgroundColor() {
        return this.rowHoverBackgroundColor;
    }

    public final ThemeOption<Color> gridColor() {
        return this.gridColor;
    }

    public final ThemeOption<Color> resizeHandleColor() {
        return this.resizeHandleColor;
    }

    public final ThemeOption<Long> doubleClickTime() {
        return this.doubleClickTime;
    }

    private static float idealWidth(final Component component) {
        return component.computeIdealSize(Size.INFINITE).clamp(component).width();
    }

    private static <E> boolean isSame(final List<E> a, final List<E> b) {
        if (a.size() != b.size()) {
            return false;
        }

        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

}
