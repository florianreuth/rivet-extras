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
import net.lenni0451.rivet.property.FloatProperty;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Column definition of a {@link Table}, should be fully configured before the table is attached.
 *
 * @param <E> the row type
 */
public class TableColumn<E> {

    private final String title;
    private final Function<E, Component> cellFactory;
    private final FloatProperty width = new FloatProperty(-1);
    private float minWidth = -1;
    private boolean resizable = true;
    private Comparator<E> comparator;

    /**
     * @param title       the text shown in the header
     * @param cellFactory creates the cell component of a row, called once per row
     */
    public TableColumn(final String title, final Function<E, Component> cellFactory) {
        this.title = title;
        this.cellFactory = cellFactory;
        this.width.addValidator(width -> {
            if (width > 1) {
                throw new IllegalArgumentException("Width of column '" + title + "' is a fraction of the table width and must not exceed 1, got " + width);
            }
            return width;
        });
    }

    public final String title() {
        return this.title;
    }

    public final Function<E, Component> cellFactory() {
        return this.cellFactory;
    }

    /**
     * @return the width as fraction of the table width, a negative value shares the remaining space with other unset columns
     */
    public final FloatProperty width() {
        return this.width;
    }

    /**
     * @param width the width as fraction of the table width (0.2 = 20%), a negative value shares the remaining space
     */
    public final TableColumn<E> width(final float width) {
        this.width.set(width);
        return this;
    }

    public final float minWidth() {
        return this.minWidth;
    }

    /**
     * @param minWidth the minimum width in pixels, a negative value uses the theme default
     */
    public final TableColumn<E> minWidth(final float minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    public final boolean resizable() {
        return this.resizable;
    }

    public final TableColumn<E> resizable(final boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    /**
     * @return the comparator used for ascending sorting, or {@code null} if the column is not sortable
     */
    public final Comparator<E> comparator() {
        return this.comparator;
    }

    /**
     * @param comparator the comparator used for ascending sorting, {@code null} disables sorting
     */
    public final TableColumn<E> comparator(final Comparator<E> comparator) {
        this.comparator = comparator;
        return this;
    }

    public final boolean sortable() {
        return this.comparator != null;
    }

}
