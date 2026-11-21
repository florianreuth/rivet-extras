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

import de.florianreuth.rivetextras.graph.Graph;
import de.florianreuth.rivetextras.graph.GraphSeries;
import de.florianreuth.rivetextras.xml.ExtrasXml;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.AssetLoader;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.xml.XmlUi;

import java.awt.Font;

public class XmlGraphDemo extends AWTApplication {

    public static void main(final String[] args) {
        new XmlGraphDemo().run();
    }

    public XmlGraphDemo() {
        super("rivet-extras - XML Graph Demo", 900, 400);
    }

    @Override
    public AWTFont createFont(final AssetLoader assetLoader) {
        return new AWTFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
    }

    @Override
    public void init(final Rivet rivet) {
        final XmlUi ui = ExtrasXml.loader().load(XmlGraphDemo.class.getResourceAsStream("/graph_demo.xml")).loadUi();

        // Ids are only registered for components, the series are found by their position
        final Graph graph = ui.require("graph", Graph.class);
        final GraphSeries sine = graph.series().get(0);
        final GraphSeries cosine = graph.series().get(1);
        for (int i = 0; i <= 200; i++) {
            final double x = i * Math.PI / 50;
            sine.add(x, Math.sin(x));
            cosine.add(x, Math.cos(x));
        }
        rivet.root().add(ui.root());
    }

}
