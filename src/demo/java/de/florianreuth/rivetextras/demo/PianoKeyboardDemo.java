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

import de.florianreuth.rivetextras.piano.PianoKeyboard;
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

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;
import java.awt.Font;
import java.util.stream.Collectors;

public class PianoKeyboardDemo extends AWTApplication {

    // Ode to Joy, -1 is a rest
    private static final int[] MELODY = {64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 64, -1, 62, 62, -1};
    private static final int NOTE_DURATION = 300;

    public static void main(final String[] args) {
        new PianoKeyboardDemo();
    }

    public PianoKeyboardDemo() {
        super("rivet-extras - Piano Keyboard Demo", 900, 400);
    }

    @Override
    protected AWTFont createFont(final AWTAssetLoader assetLoader) {
        return new AWTFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
    }

    @Override
    protected void initRivet(final Rivet rivet) {
        // Called from the super constructor, instance fields of this class are not initialized yet
        final PianoKeyboard piano = new PianoKeyboard(48, 84);
        final Label status = new Label("");

        final MidiChannel channel = openMidiChannel();
        if (channel != null) {
            piano.notePressListener().add(note -> channel.noteOn(note, 90));
            piano.noteReleaseListener().add(channel::noteOff);
        }
        final Runnable updateStatus = () -> status.text((channel == null ? "No MIDI synthesizer available - " : "") + "Pressed: " + (piano.pressedNotes().isEmpty() ? "nothing" : piano.pressedNotes().stream().sorted().map(PianoKeyboard::noteName).collect(Collectors.joining(", "))));
        piano.notePressListener().add(note -> updateStatus.run());
        piano.noteReleaseListener().add(note -> updateStatus.run());
        updateStatus.run();

        // Swing timers run on the event dispatch thread, the same thread the mouse input is handled on
        final Timer melody = new Timer(NOTE_DURATION, null);
        final int[] position = {0};
        melody.addActionListener(event -> {
            if (position[0] > 0 && MELODY[position[0] - 1] >= 0) {
                piano.release(MELODY[position[0] - 1]);
            }
            if (position[0] >= MELODY.length) {
                melody.stop();
                return;
            }
            if (MELODY[position[0]] >= 0) {
                piano.press(MELODY[position[0]]);
            }
            position[0]++;
        });

        final Container toolbar = new Container(new HorizontalListLayout(5, true));
        toolbar.add(new Button("Play melody", () -> {
            if (!melody.isRunning()) {
                position[0] = 0;
                melody.start();
            }
        }));
        toolbar.add(new Button("Play C major chord", () -> piano.press(60).press(64).press(67)));
        toolbar.add(new Button("Release all", () -> {
            melody.stop();
            piano.releaseAll();
        }));
        toolbar.add(new Button("Toggle labels", () -> piano.showLabels(!piano.showLabels().get())));

        final Container content = new Container(new DockLayout(5));
        content.add(toolbar, c -> c.layoutOptions(DockPosition.TOP));
        content.add(status, s -> s.horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT).layoutOptions(DockPosition.BOTTOM));
        content.add(piano, p -> p.layoutOptions(DockPosition.CENTER));
        rivet.root().add(new PaddedContainer(10, content));
    }

    private static MidiChannel openMidiChannel() {
        try {
            final Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            return synthesizer.getChannels()[0];
        } catch (final MidiUnavailableException e) {
            return null;
        }
    }

}
