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

import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.layout.Layout;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Lays out one component per column of the table, in column order.
 */
record TableRowLayout(Table<?> table) implements Layout {

    @Override
    public Size computeIdealSize(final Size constraints, final Collection<Component> components) {
        float[] widths = this.table.columnWidths(0);
        float layoutWidth = this.table.layoutWidth();
        // The minimum widths would make wrapping cells very tall, before the first layout the cells are measured unconstrained
        float[] cellWidths = layoutWidth > 0 ? this.table.columnWidths(layoutWidth) : null;
        Padding padding = this.table.cellPadding().value();
        float width = 0;
        float height = 0;
        int column = 0;
        for (Component component : components) {
            float cellWidth = cellWidths == null ? Float.MAX_VALUE : cellWidths[column] - padding.horizontal();
            Size idealSize = component.computeIdealSize(new Size(cellWidth, constraints.height()));
            height = Math.max(height, this.heightOf(component, idealSize));
            width += widths[column];
            column++;
        }
        return new Size(width, height + padding.vertical());
    }

    @Override
    public void layoutComponents(final Size containerSize, final Collection<Component> components, final BiConsumer<Component, Rectangle> setBounds) {
        float[] widths = this.table.columnWidths(containerSize.width());
        Padding padding = this.table.cellPadding().value();
        float x = 0;
        int column = 0;
        for (Component component : components) {
            float width = this.widthOf(component, widths[column] - padding.horizontal());
            float height = this.heightOf(component, containerSize.height() - padding.vertical());
            setBounds.accept(component, new Rectangle(x + padding.left(), padding.top(), width, height));
            x += widths[column];
            column++;
        }
    }

}
