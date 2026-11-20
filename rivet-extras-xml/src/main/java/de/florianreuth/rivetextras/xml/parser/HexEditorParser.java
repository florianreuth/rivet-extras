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

import de.florianreuth.rivetextras.hex.HexEditor;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.xml.parser.XmlParser;
import net.lenni0451.rivet.xml.parser.XmlParserContext;
import net.lenni0451.rivet.xml.parser.impl.ElementParserBuilder;
import org.w3c.dom.Node;

public class HexEditorParser implements XmlParser<HexEditor> {

    private final XmlParser<HexEditor> parser = ElementParserBuilder.createComponent(ctx -> new HexEditor())
            .referenceAttribute("data", byte[].class, HexEditor::data)
            .attribute("bytesPerRow", int.class, HexEditor::bytesPerRow)
            .attribute("baseAddress", long.class, HexEditor::baseAddress)
            .attribute("readOnly", boolean.class, HexEditor::readOnly)
            .themeOption("padding", Padding.class, HexEditor::padding)
            .themeOption("columnGap", Float.class, HexEditor::columnGap)
            .themeOption("groupSize", Integer.class, HexEditor::groupSize)
            .themeOption("separatorWidth", Float.class, HexEditor::separatorWidth)
            .themeOption("backgroundColor", Color.class, HexEditor::backgroundColor)
            .themeOption("addressBackgroundColor", Color.class, HexEditor::addressBackgroundColor)
            .themeOption("separatorColor", Color.class, HexEditor::separatorColor)
            .themeOption("addressColor", Color.class, HexEditor::addressColor)
            .themeOption("textColor", Color.class, HexEditor::textColor)
            .themeOption("zeroByteColor", Color.class, HexEditor::zeroByteColor)
            .themeOption("nonPrintableColor", Color.class, HexEditor::nonPrintableColor)
            .themeOption("cursorColor", Color.class, HexEditor::cursorColor)
            .themeOption("selectionColor", Color.class, HexEditor::selectionColor)
            .build();

    @Override
    public HexEditor parse(final XmlParserContext context, final Node node) {
        return this.parser.parse(context, node);
    }

}
