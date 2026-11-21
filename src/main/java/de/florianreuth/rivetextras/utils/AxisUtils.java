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

package de.florianreuth.rivetextras.utils;

import java.util.Locale;

/**
 * Helpers for placing and labeling ticks on a chart axis.
 */
public final class AxisUtils {

    // Guards against a degenerate step producing millions of ticks
    private static final int MAX_TICKS = 1000;

    private AxisUtils() {
    }

    /**
     * @param range    the span of the axis, max - min
     * @param maxTicks the maximum number of intervals the range is split into
     * @return 1, 2 or 5 times a power of ten, 1 if the range is not positive
     */
    public static double niceStep(final double range, final int maxTicks) {
        if (!(range > 0) || Double.isInfinite(range)) {
            return 1;
        }

        final double roughStep = range / Math.max(1, maxTicks);
        final double magnitude = Math.pow(10, Math.floor(Math.log10(roughStep)));
        final double fraction = roughStep / magnitude;
        if (fraction <= 1) {
            return magnitude;
        } else if (fraction <= 2) {
            return 2 * magnitude;
        } else if (fraction <= 5) {
            return 5 * magnitude;
        }
        return 10 * magnitude;
    }

    /**
     * @return the multiples of the step between min and max, both inclusive, in ascending order
     */
    public static double[] ticks(final double min, final double max, final double step) {
        if (!(step > 0) || !(max >= min)) {
            return new double[0];
        }

        final long first = (long) Math.ceil(min / step);
        // The epsilon keeps a tick exactly on max that lost precision in the division
        final long last = (long) Math.floor(max / step + 1e-9);
        final int count = (int) Math.min(Math.max(0, last - first + 1), MAX_TICKS);
        final double[] ticks = new double[count];
        for (int i = 0; i < count; i++) {
            // Multiplying instead of summing up the step avoids values like 0.30000000000000004
            ticks[i] = (first + i) * step;
        }
        return ticks;
    }

    /**
     * @return the number of decimal places needed to tell values one step apart
     */
    public static int decimals(final double step) {
        if (!(step > 0) || Double.isInfinite(step)) {
            return 0;
        }
        return Math.max(0, (int) -Math.floor(Math.log10(step) + 1e-9));
    }

    /**
     * @return the value with as many decimal places as the step needs, independent of the default locale
     */
    public static String format(final double value, final double step) {
        // Adding 0.0 turns -0.0 into 0.0, which would otherwise be printed as "-0"
        return String.format(Locale.ROOT, "%." + decimals(step) + "f", value + 0.0);
    }

}
