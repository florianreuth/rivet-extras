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

import de.florianreuth.rivetextras.table.Table;
import de.florianreuth.rivetextras.table.TableColumn;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.AWTAssetLoader;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.PaddedContainer;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.layout.dock.DockLayout;
import net.lenni0451.rivet.layout.dock.DockPosition;
import net.lenni0451.rivet.layout.list.HorizontalListLayout;
import net.lenni0451.rivet.text.model.TextOrigin;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TableDemo extends AWTApplication {

    private static final String[] NAMES = {"Alex", "Steve", "Notch", "Jeb", "Dinnerbone", "Grumm", "Herobrine", "Lenni", "Florian", "Kat"};
    private static final String[] COUNTRIES = {"Germany", "Austria", "Switzerland", "Sweden", "United States", "Japan"};

    public static void main(final String[] args) {
        new TableDemo();
    }

    private static final Random RANDOM = new Random();

    public TableDemo() {
        super("rivet-extras - Table Demo", 900, 600);
    }

    @Override
    protected AWTFont createFont(final AWTAssetLoader assetLoader) {
        return new AWTFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
    }

    @Override
    protected void initRivet(final Rivet rivet) {
        // Called from the super constructor, instance fields of this class are not initialized yet
        final List<Player> players = new ArrayList<>();
        final AtomicInteger nextId = new AtomicInteger();
        for (int i = 0; i < 200; i++) {
            players.add(randomPlayer(nextId.getAndIncrement()));
        }

        final TableColumn<Player> name = new TableColumn<Player>("Name", p -> new Label(p.name).horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT))
                .comparator(Comparator.comparing(p -> p.name))
                .width(0.25F);
        final TableColumn<Player> level = new TableColumn<Player>("Level", p -> new Label(String.valueOf(p.level)))
                .comparator(Comparator.comparingInt(p -> p.level))
                .width(0.1F);
        final TableColumn<Player> playtime = new TableColumn<Player>("Playtime", p -> new Label(p.playtime / 60 + "h " + p.playtime % 60 + "m"))
                .comparator(Comparator.comparingInt(p -> p.playtime))
                .width(0.15F);
        final TableColumn<Player> country = new TableColumn<Player>("Country", p -> new Label(p.country))
                .comparator(Comparator.comparing(p -> p.country));
        final TableColumn<Player> id = new TableColumn<Player>("ID", p -> new Label("#" + p.id))
                .resizable(false)
                .width(0.1F);
        final Table<Player> table = new Table<>(players, List.of(name, level, playtime, country, id));

        final Label status = new Label("");
        final Map<TableColumn<Player>, String> columnNames = Map.of(name, "Name", level, "Level", playtime, "Playtime", country, "Country", id, "ID");
        final Runnable updateStatus = () -> status.text("Sorted by " + (table.sortColumn().get() == null ? "nothing" : columnNames.get(table.sortColumn().get()) + " " + table.sortOrder().get().name().toLowerCase()));
        table.sortColumn().syncListener().add(column -> updateStatus.run());
        table.sortOrder().syncListener().add(order -> updateStatus.run());
        updateStatus.run();

        final Container toolbar = new Container(new HorizontalListLayout(5, true));
        toolbar.add(new Button("Add", () -> players.add(randomPlayer(nextId.getAndIncrement()))));
        toolbar.add(new Button("Remove random", () -> {
            if (!players.isEmpty()) {
                players.remove(RANDOM.nextInt(players.size()));
            }
        }));
        toolbar.add(new Button("Shuffle", () -> Collections.shuffle(players)));
        toolbar.add(new Button("Level up random", () -> {
            if (!players.isEmpty()) {
                final Player player = players.get(RANDOM.nextInt(players.size()));
                player.level++;
                table.refresh(player);
            }
        }));
        toolbar.add(new Button("Unsort", table::unsort));

        final Container content = new Container(new DockLayout(5));
        content.add(toolbar, c -> c.layoutOptions(DockPosition.TOP));
        content.add(status, s -> s.horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT).layoutOptions(DockPosition.BOTTOM));
        content.add(table, t -> t.layoutOptions(DockPosition.CENTER));
        rivet.root().add(new PaddedContainer(10, content));
    }

    private static Player randomPlayer(final int id) {
        final String name = NAMES[RANDOM.nextInt(NAMES.length)] + RANDOM.nextInt(1000);
        final String country = COUNTRIES[RANDOM.nextInt(COUNTRIES.length)];
        return new Player(id, name, 1 + RANDOM.nextInt(100), RANDOM.nextInt(10_000), country);
    }


    private static final class Player {
        private final int id;
        private final String name;
        private int level;
        private final int playtime;
        private final String country;

        private Player(final int id, final String name, final int level, final int playtime, final String country) {
            this.id = id;
            this.name = name;
            this.level = level;
            this.playtime = playtime;
            this.country = country;
        }
    }

}
