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

import de.florianreuth.rivetextras.table.Table;
import de.florianreuth.rivetextras.xml.ExtrasXml;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.AWTAssetLoader;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.xml.XmlUi;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class XmlTableDemo extends AWTApplication {

    public static void main(final String[] args) {
        new XmlTableDemo();
    }

    public XmlTableDemo() {
        super("rivet-extras - XML Table Demo", 900, 600);
    }

    @Override
    protected AWTFont createFont(final AWTAssetLoader assetLoader) {
        return new AWTFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
    }

    @Override
    protected void initRivet(final Rivet rivet) {
        final List<Player> players = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            players.add(new Player(i, "Player" + i, 1 + (i * 37) % 100));
        }

        final XmlUi ui = ExtrasXml.loader().load(XmlTableDemo.class.getResourceAsStream("/table_demo.xml"))
                .variable("players", players)
                .variable("nameCell", (Function<Player, Component>) p -> new Label(p.name()).horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT))
                .variable("levelCell", (Function<Player, Component>) p -> new Label(String.valueOf(p.level())))
                .variable("idCell", (Function<Player, Component>) p -> new Label("#" + p.id()))
                .variable("byName", Comparator.comparing(Player::name))
                .variable("byLevel", Comparator.comparingInt(Player::level))
                .loadUi();

        final Table<Player> table = ui.require("table", Table.class);
        final Label status = ui.require("status", Label.class);
        table.rowClickListener().add((player, event) -> status.text("Clicked " + player.name()));
        rivet.root().add(ui.root());
    }


    private record Player(int id, String name, int level) {
    }

}
