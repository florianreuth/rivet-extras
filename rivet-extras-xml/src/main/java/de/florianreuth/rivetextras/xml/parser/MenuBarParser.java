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

package de.florianreuth.rivetextras.xml.parser;

import de.florianreuth.rivetextras.menu.MenuBar;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.xml.parser.XmlParser;
import net.lenni0451.rivet.xml.parser.XmlParserContext;
import net.lenni0451.rivet.xml.parser.impl.ElementParserBuilder;
import org.w3c.dom.Node;

public class MenuBarParser implements XmlParser<MenuBar> {

    private final XmlParser<MenuBar> parser = ElementParserBuilder.createComponent(MenuBar::new)
            .child(MenuBar.Menu.class, (menuBar, menu) -> menuBar.add(menu.title(), menu.content()))
            .themeOption("titlePadding", Padding.class, MenuBar::titlePadding)
            .themeOption("popupPadding", Padding.class, MenuBar::popupPadding)
            .themeOption("minPopupWidth", Float.class, MenuBar::minPopupWidth)
            .themeOption("outlineWidth", Float.class, MenuBar::outlineWidth)
            .themeOption("backgroundColor", Color.class, MenuBar::backgroundColor)
            .themeOption("titleHoverBackgroundColor", Color.class, MenuBar::titleHoverBackgroundColor)
            .themeOption("titleOpenBackgroundColor", Color.class, MenuBar::titleOpenBackgroundColor)
            .themeOption("popupBackgroundColor", Color.class, MenuBar::popupBackgroundColor)
            .themeOption("popupOutlineColor", Color.class, MenuBar::popupOutlineColor)
            .build();

    @Override
    public MenuBar parse(final XmlParserContext context, final Node node) {
        return this.parser.parse(context, node);
    }

}
