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

import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.impl.Arrow;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.layout.dock.DockLayout;
import net.lenni0451.rivet.layout.dock.DockPosition;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.ArrayList;
import java.util.List;

final class TableHeader<E> extends Container {

    private final Table<E> table;
    private final List<Arrow> sortArrows = new ArrayList<>();
    private int hoveredColumn = -1;
    private int hoveredHandle = -1;
    private int pressedColumn = -1;
    private int resizingColumn = -1;
    private float resizeStartX;
    private float[] resizeStartWidths;
    private int lastHandleClickColumn = -1;
    private long lastHandleClick;

    TableHeader(final Table<E> table) {
        super(new TableRowLayout(table));
        this.table = table;
        for (final TableColumn<E> column : table.columns()) {
            final Arrow sortArrow = new Arrow();
            sortArrow.layoutOptions(DockPosition.RIGHT);
            this.sortArrows.add(sortArrow);

            final Container cell = new Container(DockLayout.DEFAULT);
            // The arrow has to be added first, the centered header takes all remaining space
            cell.add(sortArrow);
            cell.add(column.headerFactory().get(), header -> header.layoutOptions(DockPosition.CENTER));
            this.add(cell);
        }
        this.updateSortIndicators();
    }

    void updateSortIndicators() {
        final List<TableColumn<E>> columns = this.table.columns();
        for (int i = 0; i < columns.size(); i++) {
            final Arrow sortArrow = this.sortArrows.get(i);
            final boolean sorted = this.table.sortColumn().get() == columns.get(i);
            sortArrow.maxSize(sorted ? Size.INFINITE : new Size(0, Float.MAX_VALUE));
            sortArrow.progress(this.table.sortOrder().get() == SortOrder.ASCENDING ? 1 : 0);
        }
    }

    @Override
    protected void onRemovedInternal() {
        super.onRemovedInternal();
        this.resetState();
    }

    @Override
    protected void onDisabledInternal() {
        super.onDisabledInternal();
        this.resetState();
    }

    @Override
    protected void onMouseLeaveInternal() {
        super.onMouseLeaveInternal();
        this.hoveredColumn = -1;
        this.hoveredHandle = -1;
    }

    @Override
    protected boolean onMouseDownInternal(final MouseButtonEvent event, final Size size) {
        if (event.button().equals(MouseButton.LEFT)) {
            final float[] widths = this.table.columnWidths(size.width());
            final int handle = this.handleAt(event.x(), widths);
            if (handle >= 0) {
                final long now = System.currentTimeMillis();
                if (handle == this.lastHandleClickColumn && now - this.lastHandleClick < this.table.doubleClickTime().value()) {
                    this.table.autoSizeColumn(this.table.columns().get(handle));
                    this.lastHandleClickColumn = -1;
                } else {
                    this.resizingColumn = handle;
                    this.resizeStartX = event.x();
                    this.resizeStartWidths = widths;
                    this.lastHandleClickColumn = handle;
                    this.lastHandleClick = now;
                }
            } else {
                final int column = this.columnAt(event.x(), widths);
                if (column >= 0 && this.table.columns().get(column).sortable()) {
                    this.pressedColumn = column;
                }
            }
        }
        return true;
    }

    @Override
    protected boolean onMouseUpInternal(final MouseButtonEvent event, final Size size) {
        if (event.button().equals(MouseButton.LEFT)) {
            if (this.pressedColumn >= 0 && this.pressedColumn == this.columnAt(event.x(), this.table.columnWidths(size.width()))) {
                this.table.cycleSort(this.table.columns().get(this.pressedColumn));
            }
            this.pressedColumn = -1;
            this.resizingColumn = -1;
        }
        return true;
    }

    @Override
    protected boolean onMouseMoveInternal(final MouseMoveEvent event, final Size size) {
        if (this.resizingColumn >= 0) {
            this.table.resizeColumn(this.resizingColumn, this.resizeStartWidths, this.resizeStartWidths[this.resizingColumn] + event.x() - this.resizeStartX);
            this.lastHandleClickColumn = -1;
            return true;
        }
        final float[] widths = this.table.columnWidths(size.width());
        this.hoveredHandle = this.handleAt(event.x(), widths);
        this.hoveredColumn = this.hoveredHandle >= 0 ? -1 : this.columnAt(event.x(), widths);
        return true;
    }

    @Override
    protected void renderInternal(final Renderer renderer, final Size size, final Rectangle visibleArea) {
        final float[] widths = this.table.columnWidths(size.width());
        renderer.fillRect(0, 0, size.width(), size.height(), this.table.headerBackgroundColor().value());
        if (this.resizingColumn < 0 && this.hoveredColumn >= 0 && this.table.columns().get(this.hoveredColumn).sortable()) {
            renderer.fillRect(this.columnX(this.hoveredColumn, widths), 0, widths[this.hoveredColumn], size.height(), this.table.headerHoverBackgroundColor().value());
        }
        this.table.renderCells(renderer, this, visibleArea);
        this.table.renderGrid(renderer, size, true);

        final int handle = this.resizingColumn >= 0 ? this.resizingColumn : this.hoveredHandle;
        if (handle >= 0) {
            final float handleWidth = this.table.resizeHandleWidth().value();
            final float handleX = this.columnX(handle, widths) + widths[handle];
            renderer.fillRect(handleX - handleWidth / 2F, 0, handleWidth, size.height(), this.table.resizeHandleColor().value());
        }
    }

    private void resetState() {
        this.hoveredColumn = -1;
        this.hoveredHandle = -1;
        this.pressedColumn = -1;
        this.resizingColumn = -1;
    }

    private float columnX(final int column, final float[] widths) {
        float x = 0;
        for (int i = 0; i < column; i++) {
            x += widths[i];
        }
        return x;
    }

    private int columnAt(final float x, final float[] widths) {
        float columnX = 0;
        for (int i = 0; i < widths.length; i++) {
            if (x >= columnX && x < columnX + widths[i]) {
                return i;
            }

            columnX += widths[i];
        }
        return -1;
    }

    private int handleAt(final float x, final float[] widths) {
        final float halfHandleWidth = this.table.resizeHandleWidth().value() / 2F;
        float columnX = 0;
        for (int i = 0; i < widths.length; i++) {
            columnX += widths[i];
            if (this.table.resizeNeighbour(i) >= 0 && Math.abs(x - columnX) <= halfHandleWidth) {
                return i;
            }
        }
        return -1;
    }

}
