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

import de.florianreuth.rivetextras.hex.HexEditor;
import de.florianreuth.rivetextras.xml.ExtrasXml;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.AssetLoader;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.xml.XmlUi;

import java.awt.Font;
import java.nio.charset.StandardCharsets;

public class XmlHexEditorDemo extends AWTApplication {

    public static void main(final String[] args) {
        new XmlHexEditorDemo().run();
    }

    public XmlHexEditorDemo() {
        super("rivet-extras - XML Hex Editor Demo", 900, 400);
    }

    @Override
    public AWTFont createFont(final AssetLoader assetLoader) {
        return new AWTFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
    }

    @Override
    public void init(final Rivet rivet) {
        final XmlUi ui = ExtrasXml.loader().load(XmlHexEditorDemo.class.getResourceAsStream("/hex_editor_demo.xml"))
                .variable("data", "Hello from the rivet-extras XML demo!".getBytes(StandardCharsets.US_ASCII))
                .loadUi();

        final HexEditor editor = ui.require("editor", HexEditor.class);
        final Label status = ui.require("status", Label.class);
        editor.byteChangeListener().add((offset, oldValue, newValue) -> status.text(String.format("Changed offset %d to %02X", offset, newValue & 0xFF)));
        rivet.root().add(ui.root());
    }

}
