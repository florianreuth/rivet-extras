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

import de.florianreuth.rivetextras.table.TableColumn;
import net.lenni0451.rivet.xml.parser.XmlParser;
import net.lenni0451.rivet.xml.parser.XmlParserContext;
import net.lenni0451.rivet.xml.parser.impl.ElementParserBuilder;
import org.w3c.dom.Node;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class TableColumnParser implements XmlParser<TableColumn<?>> {

    private final XmlParser<TableColumn<?>> parser = ElementParserBuilder.<TableColumn<?>>create(ctx -> {
                final Function cellFactory = ctx.requireReferenceAttribute("cellFactory", Function.class);
                final Optional<String> title = ctx.attribute("title", String.class);
                final Optional<Supplier> headerFactory = ctx.referenceAttribute("headerFactory", Supplier.class);
                if (title.isPresent() == headerFactory.isPresent()) {
                    throw new IllegalArgumentException("Either 'title' or 'headerFactory' must be provided, but not both");
                }
                if (title.isPresent()) {
                    return new TableColumn<>(title.get(), cellFactory);
                } else {
                    return new TableColumn<>(headerFactory.get(), cellFactory);
                }
            })
            .attribute("width", float.class, TableColumn::width)
            .attribute("minWidth", float.class, TableColumn::minWidth)
            .attribute("resizable", boolean.class, TableColumn::resizable)
            .referenceAttribute("comparator", Comparator.class, TableColumn::comparator)
            .build();

    @Override
    public TableColumn<?> parse(final XmlParserContext context, final Node node) {
        return this.parser.parse(context, node);
    }

}
