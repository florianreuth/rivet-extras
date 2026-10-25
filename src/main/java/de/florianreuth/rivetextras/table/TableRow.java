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

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

final class TableRow<E> extends Container {

    private final Table<E> table;
    private final E item;
    private boolean hovered;

    TableRow(final Table<E> table, final E item) {
        super(new TableRowLayout(table));
        this.table = table;
        this.item = item;
        for (TableColumn<E> column : table.columns()) {
            this.add(column.cellFactory().apply(item));
        }
    }

    @Override
    protected void onRemovedInternal() {
        super.onRemovedInternal();
        this.hovered = false;
    }

    @Override
    protected void onDisabledInternal() {
        super.onDisabledInternal();
        this.hovered = false;
    }

    @Override
    protected void onMouseEnterInternal() {
        this.hovered = true;
    }

    @Override
    protected void onMouseLeaveInternal() {
        super.onMouseLeaveInternal();
        this.hovered = false;
    }

    @Override
    protected boolean onMouseMoveInternal(final MouseMoveEvent event, final Size size) {
        // Cells like labels do not handle mouse moves, the row would never be considered hovered otherwise
        super.onMouseMoveInternal(event, size);
        return true;
    }

    @Override
    protected void renderInternal(final Renderer renderer, final Size size, final Rectangle visibleArea) {
        Color background;
        if (this.hovered) {
            background = this.table.rowHoverBackgroundColor().value();
        } else if (this.table.viewIndex(this.item) % 2 == 1) {
            background = this.table.rowAlternateBackgroundColor().value();
        } else {
            background = this.table.rowBackgroundColor().value();
        }
        renderer.fillRect(0, 0, size.width(), size.height(), background);
        this.table.renderCells(renderer, this, visibleArea);
        // The table has no outer border, a line below the last row would only close off the bottom
        this.table.renderGrid(renderer, size, !this.table.isLastRow(this.item));
    }

}
