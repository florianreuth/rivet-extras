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

import java.util.HexFormat;

/**
 * Helpers for showing and parsing bytes as hex digits and ASCII characters.
 */
public final class HexUtils {

    public static final int FIRST_PRINTABLE = 0x20;
    public static final int LAST_PRINTABLE = 0x7E;

    private static final String HEX_DIGITS = "0123456789ABCDEF";

    private HexUtils() {
    }

    /**
     * @return the uppercase hex digit character of the value
     * @throws IndexOutOfBoundsException if the value is outside 0 - 15
     */
    public static char hexChar(final int value) {
        return HEX_DIGITS.charAt(value);
    }

    /**
     * @return the value of the hex digit, case insensitive, or -1 if the code point is not a hex digit
     */
    public static int hexDigit(final int codePoint) {
        return HEX_DIGITS.indexOf(Character.toUpperCase(codePoint));
    }

    /**
     * @param value an unsigned byte value
     * @return whether the value is a printable ASCII character
     */
    public static boolean isPrintable(final int value) {
        return value >= FIRST_PRINTABLE && value <= LAST_PRINTABLE;
    }

    /**
     * @param start the first byte, inclusive
     * @param end   the last byte, exclusive
     * @return the bytes as uppercase hex pairs separated by spaces, e.g. {@code DE AD BE EF}
     */
    public static String toHex(final byte[] data, final int start, final int end) {
        return HexFormat.ofDelimiter(" ").withUpperCase().formatHex(data, start, end);
    }

    /**
     * @param start the first byte, inclusive
     * @param end   the last byte, exclusive
     * @return the bytes as ASCII characters, non-printable bytes are shown as {@code .}
     */
    public static String toAscii(final byte[] data, final int start, final int end) {
        final StringBuilder text = new StringBuilder(end - start);
        for (int offset = start; offset < end; offset++) {
            final int value = data[offset] & 0xFF;
            text.append(isPrintable(value) ? (char) value : '.');
        }
        return text.toString();
    }

    /**
     * @param text hex digit pairs, case insensitive, whitespace is ignored
     * @return the parsed bytes, or null if the text contains other characters or an odd number of digits
     */
    public static byte[] parseHex(final String text) {
        final String digits = text.replaceAll("\\s+", "");
        if (digits.length() % 2 != 0) {
            return null;
        }

        final byte[] bytes = new byte[digits.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            final int high = hexDigit(digits.charAt(i * 2));
            final int low = hexDigit(digits.charAt(i * 2 + 1));
            if (high < 0 || low < 0) {
                return null;
            }
            bytes[i] = (byte) (high << 4 | low);
        }
        return bytes;
    }

}
