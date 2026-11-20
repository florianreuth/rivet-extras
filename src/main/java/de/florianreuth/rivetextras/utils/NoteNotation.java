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
 * Naming systems for MIDI note numbers.
 */
public enum NoteNotation {

    /**
     * English names with sharps and octave numbers, 60 is C4.
     */
    SCIENTIFIC(new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"}, OctaveStyle.SCIENTIFIC),
    /**
     * English names with flats and octave numbers, 60 is C4, 61 is Db4.
     */
    SCIENTIFIC_FLAT(new String[]{"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"}, OctaveStyle.SCIENTIFIC),
    /**
     * German names with sharps in Helmholtz octaves, 48 is c, 60 is c1, 36 is C, 24 is C1.
     */
    HELMHOLTZ_SHARP(new String[]{"c", "cis", "d", "dis", "e", "f", "fis", "g", "gis", "a", "ais", "h"}, OctaveStyle.HELMHOLTZ),
    /**
     * German names with flats in Helmholtz octaves, 48 is c, 61 is des1, 70 is b1.
     */
    HELMHOLTZ_FLAT(new String[]{"c", "des", "d", "es", "e", "f", "ges", "g", "as", "a", "b", "h"}, OctaveStyle.HELMHOLTZ),
    /**
     * Fixed-do solfège with sharps and Franco-Belgian octave numbers, 60 is Do3, 69 is La3.
     */
    SOLFEGE(new String[]{"Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si"}, OctaveStyle.FRANCO_BELGIAN);

    private final String[] names;
    private final OctaveStyle octaveStyle;

    NoteNotation(final String[] names, final OctaveStyle octaveStyle) {
        this.names = names;
        this.octaveStyle = octaveStyle;
    }

    /**
     * @param note the MIDI note, not range checked
     * @return the name of the note in this notation
     */
    public String noteName(final int note) {
        final String name = this.names[Math.floorMod(note, 12)];
        final int octave = Math.floorDiv(note, 12) - 1;
        return switch (this.octaveStyle) {
            case SCIENTIFIC -> name + octave;
            case FRANCO_BELGIAN -> name + (octave - 1);
            case HELMHOLTZ -> helmholtzName(name, octave);
        };
    }

    private static String helmholtzName(final String name, final int octave) {
        // Octave 3 is the small octave (c), 2 the great octave (C), the octaves further out are counted from these
        if (octave >= 3) {
            return octave == 3 ? name : name + (octave - 3);
        }
        final String upperName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return octave == 2 ? upperName : upperName + (2 - octave);
    }

    private enum OctaveStyle {
        SCIENTIFIC,
        FRANCO_BELGIAN,
        HELMHOLTZ
    }

}
