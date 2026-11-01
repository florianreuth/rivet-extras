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

package de.florianreuth.rivetextras.xml;

import de.florianreuth.rivetextras.piano.PianoKeyboard;
import de.florianreuth.rivetextras.table.Table;
import de.florianreuth.rivetextras.table.TableColumn;
import de.florianreuth.rivetextras.theme.ExtrasTheme;
import de.florianreuth.rivetextras.xml.parser.PianoKeyboardParser;
import de.florianreuth.rivetextras.xml.parser.TableColumnParser;
import de.florianreuth.rivetextras.xml.parser.TableParser;
import net.lenni0451.rivet.parser.ParserRegistry;
import net.lenni0451.rivet.xml.XmlLoader;
import net.lenni0451.rivet.xml.parser.XmlParserRegistry;

public final class ExtrasXml {

    private ExtrasXml() {
    }

    /**
     * Registers the parsers of all rivet-extras components.
     *
     * @param registry the registry to register the parsers in
     * @return the given registry
     */
    public static XmlParserRegistry register(final XmlParserRegistry registry) {
        // Theme references (?{...}) are resolved by key name, so the keys have to be registered before parsing
        ExtrasTheme.init();
        registry.register("PianoKeyboard", PianoKeyboard.class, new PianoKeyboardParser());
        registry.register("Table", Table.class, new TableParser());
        registry.register("TableColumn", TableColumn.class, new TableColumnParser());
        return registry;
    }

    /**
     * @return a loader supporting the standard rivet and all rivet-extras components
     */
    public static XmlLoader loader() {
        return new XmlLoader(register(XmlParserRegistry.standard()), ParserRegistry.standard());
    }

}
