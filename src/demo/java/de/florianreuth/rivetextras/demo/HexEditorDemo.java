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

package de.florianreuth.rivetextras.demo;

import de.florianreuth.rivetextras.hex.HexEditor;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.AssetLoader;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.PaddedContainer;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.layout.dock.DockLayout;
import net.lenni0451.rivet.layout.dock.DockPosition;
import net.lenni0451.rivet.layout.list.HorizontalListLayout;
import net.lenni0451.rivet.text.model.TextOrigin;

import java.awt.Font;
import java.nio.charset.StandardCharsets;

public class HexEditorDemo extends AWTApplication {

    public static void main(final String[] args) {
        new HexEditorDemo().run();
    }

    public HexEditorDemo() {
        super("rivet-extras - Hex Editor Demo", 900, 600);
    }

    @Override
    public AWTFont createFont(final AssetLoader assetLoader) {
        return new AWTFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
    }

    @Override
    public void init(final Rivet rivet) {
        final HexEditor editor = new HexEditor(createData()).baseAddress(0x7FF6_A000L);
        final Label status = new Label("Click a byte and type hex digits, Tab switches to the ASCII column");
        editor.byteChangeListener().add((offset, oldValue, newValue) -> status.text(String.format("Changed 0x%X from %02X to %02X", editor.baseAddress().get() + offset, oldValue & 0xFF, newValue & 0xFF)));

        final Container toolbar = new Container(new HorizontalListLayout(5, true));
        toolbar.add(new Button("Toggle read only", () -> {
            editor.readOnly(!editor.readOnly().get());
            status.text("Read only: " + (editor.readOnly().get() ? "on" : "off"));
        }));
        toolbar.add(new Button("8 bytes per row", () -> editor.bytesPerRow(8)));
        toolbar.add(new Button("16 bytes per row", () -> editor.bytesPerRow(16)));
        toolbar.add(new Button("Select header", () -> editor.select(0, 16)));
        toolbar.add(new Button("Reset data", () -> editor.data(createData())));

        final Container content = new Container(new DockLayout(5));
        content.add(toolbar, c -> c.layoutOptions(DockPosition.TOP));
        content.add(status, s -> s.horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT).layoutOptions(DockPosition.BOTTOM));
        content.add(new ScrollContainer(editor), s -> s.layoutOptions(DockPosition.CENTER));
        rivet.root().add(new PaddedContainer(10, content));
    }

    private static byte[] createData() {
        final byte[] text = "rivet-extras hex editor - every byte value is listed below. ".getBytes(StandardCharsets.US_ASCII);
        final byte[] data = new byte[text.length + 1024];
        System.arraycopy(text, 0, data, 0, text.length);
        for (int i = 0; i < 256; i++) {
            data[text.length + i] = (byte) i;
        }
        return data;
    }

}
