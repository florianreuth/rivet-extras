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

package de.florianreuth.rivetextras.theme;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeKey;

import static net.lenni0451.rivet.utils.MathUtils.roundMin;

/**
 * Theme keys of all rivet-extras components, registered in the global rivet {@link Theme} key registry.
 */
public final class ExtrasTheme {

    static {
        try {
            for (Class<?> declaredClass : ExtrasTheme.class.getDeclaredClasses()) {
                Class.forName(declaredClass.getName(), true, declaredClass.getClassLoader());
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError("Failed to initialize theme keys: " + t.getMessage());
        }
    }

    private ExtrasTheme() {
    }

    /**
     * Registers all keys, required before loading a theme file that contains rivet-extras keys.
     */
    public static void init() {
    }


    public static class Table {
        // Geometry & Layout
        public static final ThemeKey<Padding> CELL_PADDING = Theme.register("table.cell_padding", Padding.class, r -> {
            float textHeight = r.backend().font().height();
            return new Padding(roundMin(textHeight / 3F, 0), roundMin(textHeight / 10F, 0));
        });
        public static final ThemeKey<Float> GRID_WIDTH = Theme.register("table.grid_width", Float.class, r -> roundMin(r.backend().font().height() / 25F, 1));
        public static final ThemeKey<Float> RESIZE_HANDLE_WIDTH = Theme.register("table.resize_handle_width", Float.class, r -> roundMin(r.backend().font().height() / 4F, 1));
        public static final ThemeKey<Float> MIN_COLUMN_WIDTH = Theme.register("table.min_column_width", Float.class, r -> roundMin(r.backend().font().height() * 1.5F, 1));

        // Colors - Header
        public static final ThemeKey<Color> HEADER_BACKGROUND_COLOR = Theme.register("table.header_background_color", Color.class, r -> Color.fromRGB(45, 45, 48));
        public static final ThemeKey<Color> HEADER_HOVER_BACKGROUND_COLOR = Theme.register("table.header_hover_background_color", Color.class, r -> Color.fromRGB(65, 65, 70));

        // Colors - Rows
        public static final ThemeKey<Color> ROW_BACKGROUND_COLOR = Theme.register("table.row_background_color", Color.class, r -> Color.fromRGB(30, 30, 30));
        public static final ThemeKey<Color> ROW_ALTERNATE_BACKGROUND_COLOR = Theme.register("table.row_alternate_background_color", Color.class, r -> Color.fromRGB(37, 37, 38));
        public static final ThemeKey<Color> ROW_HOVER_BACKGROUND_COLOR = Theme.register("table.row_hover_background_color", Color.class, r -> Color.fromRGB(50, 50, 55));

        // Colors - Grid & Handles
        public static final ThemeKey<Color> GRID_COLOR = Theme.register("table.grid_color", Color.class, r -> Color.fromRGB(65, 65, 70));
        public static final ThemeKey<Color> RESIZE_HANDLE_COLOR = Theme.register("table.resize_handle_color", Color.class, r -> Color.fromRGB(0, 122, 204));

        // Behaviors & Settings
        public static final ThemeKey<Long> DOUBLE_CLICK_TIME = Theme.register("table.double_click_time", Long.class, r -> 250L);
    }

}
