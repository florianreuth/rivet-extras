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

import de.florianreuth.rivetextras.table.Table;
import de.florianreuth.rivetextras.table.TableColumn;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.xml.parser.XmlParser;
import net.lenni0451.rivet.xml.parser.XmlParserContext;
import net.lenni0451.rivet.xml.parser.impl.ElementParserBuilder;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TableParser implements XmlParser<Table<?>> {

    private final XmlParser<Table<?>> parser = ElementParserBuilder.<Table<?>>createComponent(ctx -> {
                final Optional<List> data = ctx.referenceAttribute("data", List.class);
                final Optional<Supplier> dataSupplier = ctx.referenceAttribute("dataSupplier", Supplier.class);
                if (data.isPresent() == dataSupplier.isPresent()) {
                    throw new IllegalArgumentException("Either 'data' or 'dataSupplier' must be provided, but not both");
                }
                // The columns are constructor arguments, so they are parsed here instead of as builder children
                final List<TableColumn> columnChildren = ctx.children(TableColumn.class);
                final Optional<List> columnsReference = ctx.referenceAttribute("columns", List.class);
                if (!columnChildren.isEmpty() && columnsReference.isPresent()) {
                    throw new IllegalArgumentException("Cannot specify both 'columns' attribute and TableColumn children");
                }
                final List columns = columnsReference.orElse(columnChildren);
                if (data.isPresent()) {
                    return new Table<>(data.get(), columns);
                } else {
                    return new Table<>(dataSupplier.get(), columns);
                }
            })
            .themeOption("cellPadding", Padding.class, Table::cellPadding)
            .themeOption("gridWidth", Float.class, Table::gridWidth)
            .themeOption("resizeHandleWidth", Float.class, Table::resizeHandleWidth)
            .themeOption("minColumnWidth", Float.class, Table::minColumnWidth)
            .themeOption("headerBackgroundColor", Color.class, Table::headerBackgroundColor)
            .themeOption("headerHoverBackgroundColor", Color.class, Table::headerHoverBackgroundColor)
            .themeOption("rowBackgroundColor", Color.class, Table::rowBackgroundColor)
            .themeOption("rowAlternateBackgroundColor", Color.class, Table::rowAlternateBackgroundColor)
            .themeOption("rowHoverBackgroundColor", Color.class, Table::rowHoverBackgroundColor)
            .themeOption("gridColor", Color.class, Table::gridColor)
            .themeOption("resizeHandleColor", Color.class, Table::resizeHandleColor)
            .themeOption("doubleClickTime", Long.class, Table::doubleClickTime)
            .build();

    @Override
    public Table<?> parse(final XmlParserContext context, final Node node) {
        return this.parser.parse(context, node);
    }

}
