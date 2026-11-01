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

import de.florianreuth.rivetextras.piano.PianoKeyboard;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.animation.AnimationConfig;
import net.lenni0451.rivet.xml.parser.XmlParser;
import net.lenni0451.rivet.xml.parser.XmlParserContext;
import net.lenni0451.rivet.xml.parser.impl.ElementParserBuilder;
import org.w3c.dom.Node;

import java.util.Optional;

public class PianoKeyboardParser implements XmlParser<PianoKeyboard> {

    private final XmlParser<PianoKeyboard> parser = ElementParserBuilder.createComponent(ctx -> {
                final Optional<Integer> firstNote = ctx.attribute("firstNote", int.class);
                final Optional<Integer> lastNote = ctx.attribute("lastNote", int.class);
                if (firstNote.isPresent() != lastNote.isPresent()) {
                    throw new IllegalArgumentException("Either both 'firstNote' and 'lastNote' must be provided, or neither");
                }
                if (firstNote.isPresent()) {
                    return new PianoKeyboard(firstNote.get(), lastNote.get());
                } else {
                    return new PianoKeyboard();
                }
            })
            .attribute("showLabels", boolean.class, PianoKeyboard::showLabels)
            .themeOption("whiteKeyWidth", Float.class, PianoKeyboard::whiteKeyWidth)
            .themeOption("keyHeight", Float.class, PianoKeyboard::keyHeight)
            .themeOption("blackKeyWidthRatio", Float.class, PianoKeyboard::blackKeyWidthRatio)
            .themeOption("blackKeyHeightRatio", Float.class, PianoKeyboard::blackKeyHeightRatio)
            .themeOption("cornerRadius", Float.class, PianoKeyboard::cornerRadius)
            .themeOption("separatorWidth", Float.class, PianoKeyboard::separatorWidth)
            .themeOption("pressDepthRatio", Float.class, PianoKeyboard::pressDepthRatio)
            .themeOption("faceHeightRatio", Float.class, PianoKeyboard::faceHeightRatio)
            .themeOption("faceShade", Float.class, PianoKeyboard::faceShade)
            .themeOption("labelScale", Float.class, PianoKeyboard::labelScale)
            .themeOption("blackKeyLabelScale", Float.class, PianoKeyboard::blackKeyLabelScale)
            .themeOption("pressAnimation", AnimationConfig.class, PianoKeyboard::pressAnimation)
            .themeOption("whiteKeyColor", Color.class, PianoKeyboard::whiteKeyColor)
            .themeOption("whiteKeyHoverColor", Color.class, PianoKeyboard::whiteKeyHoverColor)
            .themeOption("whiteKeyPressedColor", Color.class, PianoKeyboard::whiteKeyPressedColor)
            .themeOption("blackKeyColor", Color.class, PianoKeyboard::blackKeyColor)
            .themeOption("blackKeyHoverColor", Color.class, PianoKeyboard::blackKeyHoverColor)
            .themeOption("blackKeyPressedColor", Color.class, PianoKeyboard::blackKeyPressedColor)
            .themeOption("separatorColor", Color.class, PianoKeyboard::separatorColor)
            .themeOption("shadowColor", Color.class, PianoKeyboard::shadowColor)
            .themeOption("labelColor", Color.class, PianoKeyboard::labelColor)
            .themeOption("blackKeyLabelColor", Color.class, PianoKeyboard::blackKeyLabelColor)
            .themeOption("pressedLabelColor", Color.class, PianoKeyboard::pressedLabelColor)
            .build();

    @Override
    public PianoKeyboard parse(final XmlParserContext context, final Node node) {
        return this.parser.parse(context, node);
    }

}
