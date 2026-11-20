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

import de.florianreuth.rivetextras.menu.MenuBar;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.AssetLoader;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.PaddedContainer;
import net.lenni0451.rivet.component.impl.Checkbox;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.component.impl.Separator;
import net.lenni0451.rivet.layout.dock.DockLayout;
import net.lenni0451.rivet.layout.dock.DockPosition;
import net.lenni0451.rivet.layout.list.HorizontalListLayout;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.text.model.TextOrigin;

import java.awt.Font;

public class MenuBarDemo extends AWTApplication {

    public static void main(final String[] args) {
        new MenuBarDemo().run();
    }

    public MenuBarDemo() {
        super("rivet-extras - Menu Bar Demo", 900, 400);
    }

    @Override
    public AWTFont createFont(final AssetLoader assetLoader) {
        return new AWTFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
    }

    @Override
    public void init(final Rivet rivet) {
        final Label status = new Label("Last action: none");
        final MenuBar menuBar = new MenuBar();

        final Button save = item(menuBar, "Save", () -> status.text("Last action: Save"));
        final Container file = new Container(new VerticalListLayout(0, true));
        file.add(item(menuBar, "New", () -> status.text("Last action: New")));
        file.add(item(menuBar, "Open...", () -> status.text("Last action: Open")));
        file.add(save);
        file.add(new Separator());
        file.add(item(menuBar, "Exit", () -> System.exit(0)));

        final Container edit = new Container(new VerticalListLayout(0, true));
        edit.add(item(menuBar, "Undo", () -> status.text("Last action: Undo")));
        edit.add(item(menuBar, "Redo", () -> status.text("Last action: Redo")));
        edit.add(new Separator());
        edit.add(new Checkbox("Word wrap"));

        menuBar.add("File", file)
                .add("Edit", edit)
                .add(new Label("Help"), item(menuBar, "About", () -> status.text("Last action: About")));

        final Container toolbar = new Container(new HorizontalListLayout(5, true));
        toolbar.add(new Button("Toggle 'Save' enabled", () -> save.disabled(!save.disabled().get())));
        toolbar.add(new Button("Open 'Edit' menu", () -> menuBar.open(menuBar.menus().get(1))));

        final Container content = new Container(new DockLayout(5));
        content.add(toolbar, c -> c.layoutOptions(DockPosition.TOP));
        content.add(status, s -> s.horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT).layoutOptions(DockPosition.BOTTOM));

        final Container window = new Container(DockLayout.DEFAULT);
        window.add(menuBar, m -> m.layoutOptions(DockPosition.TOP));
        window.add(new PaddedContainer(10, content), c -> c.layoutOptions(DockPosition.CENTER));
        rivet.root().add(window);
    }

    private static Button item(final MenuBar menuBar, final String text, final Runnable action) {
        return new Button(new Label(text).horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT), () -> {
            menuBar.close();
            action.run();
        });
    }

}
