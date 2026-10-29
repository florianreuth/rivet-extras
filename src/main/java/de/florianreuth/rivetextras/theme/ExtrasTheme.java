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
import net.lenni0451.rivet.animation.AnimationConfig;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.theme.DefaultTheme;
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

    public static class PianoKeyboard {
        // Geometry & Layout - the width and height are ideal sizes, the keys stretch to fill the component
        public static final ThemeKey<Float> WHITE_KEY_WIDTH = Theme.register("piano_keyboard.white_key_width", Float.class, r -> roundMin(r.backend().font().height() * 1.5F, 1));
        public static final ThemeKey<Float> KEY_HEIGHT = Theme.register("piano_keyboard.key_height", Float.class, r -> roundMin(r.backend().font().height() * 6F, 1));
        public static final ThemeKey<Float> BLACK_KEY_WIDTH_RATIO = Theme.register("piano_keyboard.black_key_width_ratio", Float.class, r -> 0.6F);
        public static final ThemeKey<Float> BLACK_KEY_HEIGHT_RATIO = Theme.register("piano_keyboard.black_key_height_ratio", Float.class, r -> 0.62F);
        public static final ThemeKey<Float> CORNER_RADIUS = Theme.register("piano_keyboard.corner_radius", Float.class, r -> roundMin(r.backend().font().height() / 8F, 0));
        public static final ThemeKey<Float> SEPARATOR_WIDTH = Theme.register("piano_keyboard.separator_width", Float.class, r -> roundMin(r.backend().font().height() / 8F, 1));

        // 3D Effect - ratios are relative to the key height
        public static final ThemeKey<Float> PRESS_DEPTH_RATIO = Theme.register("piano_keyboard.press_depth_ratio", Float.class, r -> 0.03F);
        public static final ThemeKey<Float> FACE_HEIGHT_RATIO = Theme.register("piano_keyboard.face_height_ratio", Float.class, r -> 0.07F);
        public static final ThemeKey<Float> FACE_SHADE = Theme.register("piano_keyboard.face_shade", Float.class, r -> 0.25F);
        public static final ThemeKey<AnimationConfig> PRESS_ANIMATION = Theme.register("piano_keyboard.press_animation", AnimationConfig.class, r -> DefaultTheme.CLICK_ANIMATION);

        // Labels - scales are relative to the font size
        public static final ThemeKey<Float> LABEL_SCALE = Theme.register("piano_keyboard.label_scale", Float.class, r -> 0.75F);
        public static final ThemeKey<Float> BLACK_KEY_LABEL_SCALE = Theme.register("piano_keyboard.black_key_label_scale", Float.class, r -> 0.6F);

        // Colors - White keys
        public static final ThemeKey<Color> WHITE_KEY_COLOR = Theme.register("piano_keyboard.white_key_color", Color.class, r -> Color.fromRGB(240, 240, 240));
        public static final ThemeKey<Color> WHITE_KEY_HOVER_COLOR = Theme.register("piano_keyboard.white_key_hover_color", Color.class, r -> Color.fromRGB(215, 215, 220));
        public static final ThemeKey<Color> WHITE_KEY_PRESSED_COLOR = Theme.register("piano_keyboard.white_key_pressed_color", Color.class, r -> Color.fromRGB(0, 122, 204));

        // Colors - Black keys
        public static final ThemeKey<Color> BLACK_KEY_COLOR = Theme.register("piano_keyboard.black_key_color", Color.class, r -> Color.fromRGB(30, 30, 30));
        public static final ThemeKey<Color> BLACK_KEY_HOVER_COLOR = Theme.register("piano_keyboard.black_key_hover_color", Color.class, r -> Color.fromRGB(65, 65, 70));
        public static final ThemeKey<Color> BLACK_KEY_PRESSED_COLOR = Theme.register("piano_keyboard.black_key_pressed_color", Color.class, r -> Color.fromRGB(0, 90, 160));

        // Colors - Separators, Shadows & Labels
        public static final ThemeKey<Color> SEPARATOR_COLOR = Theme.register("piano_keyboard.separator_color", Color.class, r -> Color.fromRGB(150, 150, 155));
        public static final ThemeKey<Color> SHADOW_COLOR = Theme.register("piano_keyboard.shadow_color", Color.class, r -> Color.fromRGBA(0, 0, 0, 90));
        public static final ThemeKey<Color> LABEL_COLOR = Theme.register("piano_keyboard.label_color", Color.class, r -> Color.fromRGB(110, 110, 115));
        public static final ThemeKey<Color> BLACK_KEY_LABEL_COLOR = Theme.register("piano_keyboard.black_key_label_color", Color.class, r -> Color.fromRGB(170, 170, 175));
        public static final ThemeKey<Color> PRESSED_LABEL_COLOR = Theme.register("piano_keyboard.pressed_label_color", Color.class, r -> Color.WHITE);
    }

}
