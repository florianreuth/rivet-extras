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

import de.florianreuth.rivetextras.menu.MenuBar;
import de.florianreuth.rivetextras.xml.ExtrasXml;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.AssetLoader;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.xml.XmlUi;

import java.awt.Font;

public class XmlMenuBarDemo extends AWTApplication {

    public static void main(final String[] args) {
        new XmlMenuBarDemo().run();
    }

    public XmlMenuBarDemo() {
        super("rivet-extras - XML Menu Bar Demo", 900, 400);
    }

    @Override
    public AWTFont createFont(final AssetLoader assetLoader) {
        return new AWTFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
    }

    @Override
    public void init(final Rivet rivet) {
        final XmlUi ui = ExtrasXml.loader().load(XmlMenuBarDemo.class.getResourceAsStream("/menu_bar_demo.xml")).loadUi();

        final MenuBar menuBar = ui.require("menuBar", MenuBar.class);
        final Label status = ui.require("status", Label.class);
        for (final String id : new String[]{"new", "open", "undo"}) {
            final Button button = ui.require(id, Button.class);
            button.clickListener().add(event -> {
                menuBar.close();
                status.text("Last action: " + id);
            });
        }
        ui.require("exit", Button.class).clickListener().add(event -> System.exit(0));
        rivet.root().add(ui.root());
    }

}
