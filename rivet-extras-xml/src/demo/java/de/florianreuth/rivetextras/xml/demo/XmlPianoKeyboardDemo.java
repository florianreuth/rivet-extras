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

package de.florianreuth.rivetextras.xml.demo;

import de.florianreuth.rivetextras.piano.PianoKeyboard;
import de.florianreuth.rivetextras.utils.NoteUtils;
import de.florianreuth.rivetextras.xml.ExtrasXml;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.AWTAssetLoader;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.xml.XmlUi;

import java.awt.Font;

public class XmlPianoKeyboardDemo extends AWTApplication {

    public static void main(final String[] args) {
        new XmlPianoKeyboardDemo();
    }

    public XmlPianoKeyboardDemo() {
        super("rivet-extras - XML Piano Keyboard Demo", 900, 400);
    }

    @Override
    protected AWTFont createFont(final AWTAssetLoader assetLoader) {
        return new AWTFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
    }

    @Override
    protected void initRivet(final Rivet rivet) {
        final XmlUi ui = ExtrasXml.loader().load(XmlPianoKeyboardDemo.class.getResourceAsStream("/piano_keyboard_demo.xml")).loadUi();

        final PianoKeyboard piano = ui.require("piano", PianoKeyboard.class);
        final Label status = ui.require("status", Label.class);
        piano.notePressListener().add(note -> status.text("Pressed " + NoteUtils.noteName(note)));
        rivet.root().add(ui.root());
    }

}
