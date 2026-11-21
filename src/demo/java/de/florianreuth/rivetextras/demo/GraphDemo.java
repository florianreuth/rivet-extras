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

import de.florianreuth.rivetextras.graph.Graph;
import de.florianreuth.rivetextras.graph.GraphSeries;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.AssetLoader;
import net.lenni0451.rivet.backend.awt.AWTApplication;
import net.lenni0451.rivet.backend.awt.text.AWTFont;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.PaddedContainer;
import net.lenni0451.rivet.layout.dock.DockLayout;
import net.lenni0451.rivet.layout.dock.DockPosition;
import net.lenni0451.rivet.layout.list.HorizontalListLayout;

import javax.swing.Timer;
import java.awt.Font;
import java.util.Locale;
import java.util.Random;

public class GraphDemo extends AWTApplication {

    private static final int UPDATE_INTERVAL = 50;
    private static final int VISIBLE_POINTS = 200;

    private final Random random = new Random(0);
    private double time;

    public static void main(final String[] args) {
        new GraphDemo().run();
    }

    public GraphDemo() {
        super("rivet-extras - Graph Demo", 900, 600);
    }

    @Override
    public AWTFont createFont(final AssetLoader assetLoader) {
        return new AWTFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
    }

    @Override
    public void init(final Rivet rivet) {
        final GraphSeries cpu = new GraphSeries("CPU").maxPoints(VISIBLE_POINTS);
        final GraphSeries memory = new GraphSeries("Memory").maxPoints(VISIBLE_POINTS);
        final Graph graph = new Graph().add(cpu).add(memory)
                .yLabelFormatter(value -> String.format(Locale.ROOT, "%.0f %%", value));
        // Filled up front so the graph does not start empty
        for (int i = 0; i < VISIBLE_POINTS; i++) {
            this.addSample(cpu, memory);
        }

        // Swing timers run on the event dispatch thread, the series can be written from any thread
        final Timer timer = new Timer(UPDATE_INTERVAL, event -> this.addSample(cpu, memory));
        timer.start();

        final Container toolbar = new Container(new HorizontalListLayout(5, true));
        toolbar.add(new Button("Toggle pause", () -> {
            if (timer.isRunning()) {
                timer.stop();
            } else {
                timer.start();
            }
        }));
        toolbar.add(new Button("Toggle 0 - 100 %", () -> graph.yRange(graph.yRange().get() == null ? new Graph.Range(0, 100) : null)));
        toolbar.add(new Button("Toggle legend", () -> graph.showLegend(!graph.showLegend().get())));
        toolbar.add(new Button("Clear", () -> {
            cpu.clear();
            memory.clear();
        }));

        final Container content = new Container(new DockLayout(5));
        content.add(toolbar, c -> c.layoutOptions(DockPosition.TOP));
        content.add(graph, g -> g.layoutOptions(DockPosition.CENTER));
        rivet.root().add(new PaddedContainer(10, content));
    }

    private void addSample(final GraphSeries cpu, final GraphSeries memory) {
        this.time += UPDATE_INTERVAL / 1000D;
        final double cpuLoad = 40 + 25 * Math.sin(this.time * 0.9) + 10 * Math.sin(this.time * 2.7) + this.random.nextGaussian() * 3;
        final double memoryUsage = 55 + 10 * Math.sin(this.time * 0.2) + this.random.nextGaussian();
        cpu.add(this.time, Math.max(0, Math.min(100, cpuLoad)));
        memory.add(this.time, Math.max(0, Math.min(100, memoryUsage)));
    }

}
