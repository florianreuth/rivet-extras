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
package de.florianreuth.rivetextras.xml.parser;

import de.florianreuth.rivetextras.graph.Graph;
import de.florianreuth.rivetextras.graph.GraphSeries;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.xml.parser.XmlParser;
import net.lenni0451.rivet.xml.parser.XmlParserContext;
import net.lenni0451.rivet.xml.parser.impl.ElementParserBuilder;
import net.lenni0451.rivet.xml.parser.impl.NodeContext;
import org.w3c.dom.Node;

import java.util.Optional;
import java.util.function.DoubleFunction;

public class GraphParser implements XmlParser<Graph> {

    private final XmlParser<Graph> parser = ElementParserBuilder.createComponent(ctx -> {
                final Graph graph = new Graph();
                graph.xRange(range(ctx, "xMin", "xMax"));
                graph.yRange(range(ctx, "yMin", "yMax"));
                for (final GraphSeries series : ctx.children(GraphSeries.class)) {
                    graph.add(series);
                }
                return graph;
            })
            .attribute("showLegend", boolean.class, Graph::showLegend)
            .referenceAttribute("xLabelFormatter", DoubleFunction.class, Graph::xLabelFormatter)
            .referenceAttribute("yLabelFormatter", DoubleFunction.class, Graph::yLabelFormatter)
            .themeOption("width", Float.class, Graph::width)
            .themeOption("height", Float.class, Graph::height)
            .themeOption("padding", Padding.class, Graph::padding)
            .themeOption("innerPadding", Padding.class, Graph::innerPadding)
            .themeOption("tickLength", Float.class, Graph::tickLength)
            .themeOption("tickSpacing", Float.class, Graph::tickSpacing)
            .themeOption("lineWidth", Float.class, Graph::lineWidth)
            .themeOption("gridWidth", Float.class, Graph::gridWidth)
            .themeOption("pointRadius", Float.class, Graph::pointRadius)
            .themeOption("hoverRadius", Float.class, Graph::hoverRadius)
            .themeOption("backgroundColor", Color.class, Graph::backgroundColor)
            .themeOption("plotBackgroundColor", Color.class, Graph::plotBackgroundColor)
            .themeOption("gridColor", Color.class, Graph::gridColor)
            .themeOption("axisColor", Color.class, Graph::axisColor)
            .themeOption("labelColor", Color.class, Graph::labelColor)
            .themeOption("textColor", Color.class, Graph::textColor)
            .themeOption("legendBackgroundColor", Color.class, Graph::legendBackgroundColor)
            .themeOption("tooltipBackgroundColor", Color.class, Graph::tooltipBackgroundColor)
            .build();

    @Override
    public Graph parse(final XmlParserContext context, final Node node) {
        return this.parser.parse(context, node);
    }

    private static Graph.Range range(final NodeContext ctx, final String minName, final String maxName) {
        final Optional<Double> min = ctx.attribute(minName, double.class);
        final Optional<Double> max = ctx.attribute(maxName, double.class);
        if (min.isPresent() != max.isPresent()) {
            throw new IllegalArgumentException("Either both '" + minName + "' and '" + maxName + "' must be provided, or neither");
        }
        return min.map(value -> new Graph.Range(value, max.get())).orElse(null);
    }

}
