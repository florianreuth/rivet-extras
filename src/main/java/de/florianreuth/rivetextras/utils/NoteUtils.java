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

/**
 * Helpers for MIDI note numbers.
 */
public final class NoteUtils {

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    // Number of white keys below each note of an octave, starting at C
    private static final int[] WHITE_KEYS_BELOW = {0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6, 6};
    // Distance of each white key of an octave from C
    private static final int[] WHITE_KEY_OFFSETS = {0, 2, 4, 5, 7, 9, 11};

    private NoteUtils() {
    }

    public static boolean isBlackKey(final int note) {
        return switch (Math.floorMod(note, 12)) {
            case 1, 3, 6, 8, 10 -> true;
            default -> false;
        };
    }

    /**
     * @return the scientific pitch name, 60 is C4
     */
    public static String noteName(final int note) {
        return NOTE_NAMES[Math.floorMod(note, 12)] + (Math.floorDiv(note, 12) - 1);
    }

    /**
     * @return the number of white keys below the note, counted from MIDI note 0
     */
    public static int absoluteWhiteIndex(final int note) {
        return note / 12 * 7 + WHITE_KEYS_BELOW[note % 12];
    }

    /**
     * @param whiteIndex the number of white keys below the note, counted from MIDI note 0
     * @return the note of the white key, inverse of {@link #absoluteWhiteIndex(int)}
     */
    public static int whiteKeyNote(final int whiteIndex) {
        return whiteIndex / 7 * 12 + WHITE_KEY_OFFSETS[whiteIndex % 7];
    }

    /**
     * @throws IllegalArgumentException if the note is outside the MIDI range 0 - 127
     */
    public static void checkNote(final int note) {
        if (note < 0 || note > 127) {
            throw new IllegalArgumentException("Note " + note + " is outside the MIDI range 0 - 127");
        }
    }

}
