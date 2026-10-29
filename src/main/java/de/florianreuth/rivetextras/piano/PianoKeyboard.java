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

package de.florianreuth.rivetextras.piano;

import de.florianreuth.rivetextras.theme.ExtrasTheme;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.animation.AnimationConfig;
import net.lenni0451.rivet.animation.Interpolator;
import net.lenni0451.rivet.animation.Transition;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.Font;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.event.ListenerList;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.property.BooleanProperty;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Piano keyboard spanning a range of MIDI notes, keys can be played with the mouse or pressed programmatically.
 */
public class PianoKeyboard extends Component {

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    // Number of white keys below each note of an octave, starting at C
    private static final int[] WHITE_KEYS_BELOW = {0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6, 6};
    // Distance of each white key of an octave from C
    private static final int[] WHITE_KEY_OFFSETS = {0, 2, 4, 5, 7, 9, 11};

    private final int firstNote;
    private final int lastNote;
    private final int whiteKeyCount;
    // Indexed by note - firstNote
    private final boolean[] pressedNotes;
    private final ListenerList<IntConsumer> notePressListener = new ListenerList<>();
    private final ListenerList<IntConsumer> noteReleaseListener = new ListenerList<>();
    private final BooleanProperty showLabels = new BooleanProperty(true);

    private final ThemeOption<Float> whiteKeyWidth = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.WHITE_KEY_WIDTH);
    private final ThemeOption<Float> keyHeight = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.KEY_HEIGHT);
    private final ThemeOption<Float> blackKeyWidthRatio = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.BLACK_KEY_WIDTH_RATIO);
    private final ThemeOption<Float> blackKeyHeightRatio = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.BLACK_KEY_HEIGHT_RATIO);
    private final ThemeOption<Float> cornerRadius = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.CORNER_RADIUS);
    private final ThemeOption<Float> separatorWidth = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.SEPARATOR_WIDTH);
    private final ThemeOption<Float> pressDepthRatio = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.PRESS_DEPTH_RATIO);
    private final ThemeOption<Float> faceHeightRatio = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.FACE_HEIGHT_RATIO);
    private final ThemeOption<Float> faceShade = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.FACE_SHADE);
    private final ThemeOption<Float> labelScale = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.LABEL_SCALE);
    private final ThemeOption<Float> blackKeyLabelScale = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.BLACK_KEY_LABEL_SCALE);
    private final ThemeOption<AnimationConfig> pressAnimation = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.PRESS_ANIMATION);
    private final ThemeOption<Color> whiteKeyColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.WHITE_KEY_COLOR);
    private final ThemeOption<Color> whiteKeyHoverColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.WHITE_KEY_HOVER_COLOR);
    private final ThemeOption<Color> whiteKeyPressedColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.WHITE_KEY_PRESSED_COLOR);
    private final ThemeOption<Color> blackKeyColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.BLACK_KEY_COLOR);
    private final ThemeOption<Color> blackKeyHoverColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.BLACK_KEY_HOVER_COLOR);
    private final ThemeOption<Color> blackKeyPressedColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.BLACK_KEY_PRESSED_COLOR);
    private final ThemeOption<Color> separatorColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.SEPARATOR_COLOR);
    private final ThemeOption<Color> shadowColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.SHADOW_COLOR);
    private final ThemeOption<Color> labelColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.LABEL_COLOR);
    private final ThemeOption<Color> blackKeyLabelColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.BLACK_KEY_LABEL_COLOR);
    private final ThemeOption<Color> pressedLabelColor = new ThemeOption<>(this, ExtrasTheme.PianoKeyboard.PRESSED_LABEL_COLOR);

    // Indexed by note - firstNote, animates between 0 (released) and 1 (pressed)
    private final List<Transition<Float>> pressProgress = new ArrayList<>();
    // Only accessed from the render thread, indexed by note, pressed labels are stored at note + 128
    private final ShapedText[] labelCache = new ShapedText[256];
    private LabelStyle labelStyle;
    private Font whiteLabelFont;
    private Font blackLabelFont;
    private int hoveredNote = -1;
    private int mouseNote = -1;
    private boolean dragging;

    /**
     * Creates a keyboard from C3 to C6.
     */
    public PianoKeyboard() {
        this(48, 84);
    }

    /**
     * @param firstNote the MIDI note of the leftmost key, 60 is C4 (middle C)
     * @param lastNote  the MIDI note of the rightmost key, inclusive
     * @throws IllegalArgumentException if a note is outside 0-127, the range is empty or starts or ends on a black key
     */
    public PianoKeyboard(final int firstNote, final int lastNote) {
        checkNote(firstNote);
        checkNote(lastNote);
        if (firstNote > lastNote) {
            throw new IllegalArgumentException("First note " + noteName(firstNote) + " is above last note " + noteName(lastNote));
        }
        // A black key at the edge would hang over the keyboard bounds
        if (isBlackKey(firstNote) || isBlackKey(lastNote)) {
            throw new IllegalArgumentException("The keyboard has to start and end on a white key, got " + noteName(firstNote) + " - " + noteName(lastNote));
        }
        this.firstNote = firstNote;
        this.lastNote = lastNote;
        this.whiteKeyCount = this.whiteIndex(lastNote) + 1;
        this.pressedNotes = new boolean[lastNote - firstNote + 1];
        for (int note = firstNote; note <= lastNote; note++) {
            final int key = note;
            this.pressProgress.add(new Transition<>(this, () -> this.isPressed(key) ? 1F : 0F, this.pressAnimation::value, Interpolator.FLOAT));
        }
    }

    public final int firstNote() {
        return this.firstNote;
    }

    public final int lastNote() {
        return this.lastNote;
    }

    /**
     * @return an unmodifiable snapshot of the currently pressed notes in ascending order
     */
    public final Set<Integer> pressedNotes() {
        final Set<Integer> pressedNotes = new LinkedHashSet<>();
        for (int note = this.firstNote; note <= this.lastNote; note++) {
            if (this.isPressed(note)) {
                pressedNotes.add(note);
            }
        }
        return Collections.unmodifiableSet(pressedNotes);
    }

    public final boolean isPressed(final int note) {
        return note >= this.firstNote && note <= this.lastNote && this.pressedNotes[note - this.firstNote];
    }

    /**
     * @return listeners called with the MIDI note when a key goes down, by mouse or {@link #press(int)}
     */
    public final ListenerList<IntConsumer> notePressListener() {
        return this.notePressListener;
    }

    /**
     * @return listeners called with the MIDI note when a key goes up, by mouse or {@link #release(int)}
     */
    public final ListenerList<IntConsumer> noteReleaseListener() {
        return this.noteReleaseListener;
    }

    /**
     * @return whether the keys are labeled with their note name, labels too wide for their key are hidden
     */
    public final BooleanProperty showLabels() {
        return this.showLabels;
    }

    public final PianoKeyboard showLabels(final boolean showLabels) {
        this.showLabels.set(showLabels);
        return this;
    }

    /**
     * @throws IllegalArgumentException if the note is outside the keyboard range
     */
    public final PianoKeyboard press(final int note) {
        this.checkInRange(note);
        if (!this.pressedNotes[note - this.firstNote]) {
            this.pressedNotes[note - this.firstNote] = true;
            this.notePressListener.call(listener -> listener.accept(note));
        }
        return this;
    }

    /**
     * @throws IllegalArgumentException if the note is outside the keyboard range
     */
    public final PianoKeyboard release(final int note) {
        this.checkInRange(note);
        if (this.pressedNotes[note - this.firstNote]) {
            this.pressedNotes[note - this.firstNote] = false;
            this.noteReleaseListener.call(listener -> listener.accept(note));
        }
        return this;
    }

    public final PianoKeyboard releaseAll() {
        for (int note = this.firstNote; note <= this.lastNote; note++) {
            if (this.isPressed(note)) {
                this.release(note);
            }
        }
        return this;
    }

    @Override
    protected void onRemovedInternal() {
        this.resetMouseState();
    }

    @Override
    protected void onDisabledInternal() {
        this.resetMouseState();
    }

    @Override
    protected void onMouseLeaveInternal() {
        this.hoveredNote = -1;
    }

    @Override
    protected boolean onMouseDownInternal(final MouseButtonEvent event, final Size size) {
        if (event.button().equals(MouseButton.LEFT)) {
            this.dragging = true;
            this.moveMouseNote(this.noteAt(event.x(), event.y(), size));
        }
        return true;
    }

    @Override
    protected boolean onMouseUpInternal(final MouseButtonEvent event, final Size size) {
        if (event.button().equals(MouseButton.LEFT)) {
            this.moveMouseNote(-1);
            this.dragging = false;
        }
        return true;
    }

    @Override
    protected boolean onMouseMoveInternal(final MouseMoveEvent event, final Size size) {
        final int note = this.noteAt(event.x(), event.y(), size);
        this.hoveredNote = note;
        if (this.dragging) {
            this.moveMouseNote(note);
        }
        return true;
    }

    private void moveMouseNote(final int note) {
        if (note == this.mouseNote) {
            return;
        }

        if (this.mouseNote >= 0) {
            this.release(this.mouseNote);
        }
        this.mouseNote = note;
        if (note >= 0) {
            this.press(note);
        }
    }

    private void resetMouseState() {
        this.moveMouseNote(-1);
        this.dragging = false;
        this.hoveredNote = -1;
    }

    @Override
    protected void renderInternal(final Renderer renderer, final Size size, final Rectangle visibleArea) {
        final float whiteWidth = size.width() / this.whiteKeyCount;
        final float blackWidth = whiteWidth * this.blackKeyWidthRatio.value();
        final float blackHeight = size.height() * this.blackKeyHeightRatio.value();
        final float pressDepth = size.height() * this.pressDepthRatio.value();
        final float whiteFaceHeight = size.height() * this.faceHeightRatio.value();
        final float blackFaceHeight = blackHeight * this.faceHeightRatio.value();
        final float cornerRadius = this.cornerRadius.value();
        final float separatorWidth = this.separatorWidth.value();
        final float faceShade = this.faceShade.value();
        final Color shadowColor = this.shadowColor.value();
        final Color whiteKeyColor = this.whiteKeyColor.value();
        final Color whiteKeyHoverColor = this.whiteKeyHoverColor.value();
        final Color whiteKeyPressedColor = this.whiteKeyPressedColor.value();
        final Color blackKeyColor = this.blackKeyColor.value();
        final Color blackKeyHoverColor = this.blackKeyHoverColor.value();
        final Color blackKeyPressedColor = this.blackKeyPressedColor.value();
        final boolean showLabels = this.showLabels.get();
        if (showLabels) {
            this.updateLabelStyle();
        }

        for (int note = this.firstNote; note <= this.lastNote; note++) {
            if (isBlackKey(note)) {
                continue;
            }
            final float x = this.whiteIndex(note) * whiteWidth;
            final float progress = this.pressProgress(note);
            final Color color = this.keyColor(note, progress, whiteKeyColor, whiteKeyHoverColor, whiteKeyPressedColor);
            final float faceY = Math.min(size.height() - whiteFaceHeight + pressDepth * progress, size.height());
            renderer.fillRect(x, 0, whiteWidth, faceY, color);
            renderer.fillRoundedRect(x, faceY, whiteWidth, size.height() - faceY, 0, cornerRadius, cornerRadius, 0, Color.interpolate(faceShade, color, Color.BLACK));
            if (progress > 0) {
                // The pressed key sinks below its neighbors, they cast a shadow onto its top
                final Color shadow = shadowColor.multiplyAlpha(progress);
                renderer.fillGradientRect(x, 0, whiteWidth, pressDepth * 2, shadow, shadow.withAlpha(0), shadow.withAlpha(0), shadow);
            }
            if (showLabels) {
                this.renderLabel(renderer, note, progress, this.whiteLabelFont, this.labelStyle.whiteColor(), x, whiteWidth, whiteWidth - separatorWidth, faceY - cornerRadius);
            }
        }
        // Drawn after all white keys, a pressed key would otherwise cover half of its neighbor's separator
        if (separatorWidth > 0) {
            final Color separatorColor = this.separatorColor.value();
            for (int i = 1; i < this.whiteKeyCount; i++) {
                renderer.fillRect(i * whiteWidth - separatorWidth / 2F, 0, separatorWidth, size.height(), separatorColor);
            }
        }
        for (int note = this.firstNote; note <= this.lastNote; note++) {
            if (!isBlackKey(note)) {
                continue;
            }
            final float x = this.whiteIndex(note) * whiteWidth - blackWidth / 2F;
            final float progress = this.pressProgress(note);
            final Color color = this.keyColor(note, progress, blackKeyColor, blackKeyHoverColor, blackKeyPressedColor);
            final float shadowOffset = pressDepth / 2F * (1 - progress);
            if (shadowOffset > 0) {
                renderer.fillRoundedRect(x + shadowOffset, 0, blackWidth, blackHeight + shadowOffset, 0, cornerRadius, cornerRadius, 0, shadowColor);
            }
            final float faceY = Math.min(blackHeight - blackFaceHeight + pressDepth * progress, blackHeight);
            renderer.fillRect(x, 0, blackWidth, faceY, color);
            renderer.fillRoundedRect(x, faceY, blackWidth, blackHeight - faceY, 0, cornerRadius, cornerRadius, 0, Color.interpolate(faceShade, color, Color.WHITE));
            if (showLabels) {
                this.renderLabel(renderer, note, progress, this.blackLabelFont, this.labelStyle.blackColor(), x, blackWidth, blackWidth - separatorWidth, faceY - cornerRadius);
            }
        }
    }

    private float pressProgress(final int note) {
        return this.pressProgress.get(note - this.firstNote).value();
    }

    private Color keyColor(final int note, final float progress, final Color color, final Color hoverColor, final Color pressedColor) {
        if (progress >= 1) {
            return pressedColor;
        }

        final Color baseColor = note == this.hoveredNote && !this.dragging ? hoverColor : color;
        // Only animating keys need a new color, interpolating every key every frame would allocate needlessly
        if (progress <= 0) {
            return baseColor;
        }

        return Color.interpolate(progress, baseColor, pressedColor);
    }

    private void renderLabel(final Renderer renderer, final int note, final float progress, final Font font, final Color color, final float x, final float width, final float maxLabelWidth, final float bottomY) {
        final boolean pressed = progress >= 0.5F;
        // Pressed labels are cached separately, the label color changes with the key color
        final int cacheIndex = pressed ? note + 128 : note;
        ShapedText label = this.labelCache[cacheIndex];
        if (label == null) {
            label = font.shapeText(noteName(note), pressed ? this.labelStyle.pressedColor() : color);
            this.labelCache[cacheIndex] = label;
        }
        if (label.visualBounds().width() <= maxLabelWidth) {
            renderer.text(label, x + width / 2F, bottomY, TextOrigin.Horizontal.VISUAL_CENTER, TextOrigin.Vertical.LOGICAL_BOTTOM);
        }
    }

    private void updateLabelStyle() {
        final LabelStyle style = new LabelStyle(this.rivet().backend().font(), this.labelScale.value(), this.blackKeyLabelScale.value(), this.labelColor.value(), this.blackKeyLabelColor.value(), this.pressedLabelColor.value());
        if (style.equals(this.labelStyle)) {
            return;
        }

        this.labelStyle = style;
        Arrays.fill(this.labelCache, null);
        this.whiteLabelFont = style.font().derive(Math.max(1, Math.round(style.font().size() * style.whiteScale())));
        this.blackLabelFont = style.font().derive(Math.max(1, Math.round(style.font().size() * style.blackScale())));
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(this.whiteKeyCount * this.whiteKeyWidth.value(), this.keyHeight.value());
    }

    /**
     * @return the note under the position, black keys take precedence, or -1 if there is no key
     */
    private int noteAt(final float x, final float y, final Size size) {
        if (x < 0 || x >= size.width() || y < 0 || y >= size.height()) {
            return -1;
        }

        final float whiteWidth = size.width() / this.whiteKeyCount;
        if (y < size.height() * this.blackKeyHeightRatio.value()) {
            final float halfBlackWidth = whiteWidth * this.blackKeyWidthRatio.value() / 2F;
            for (int note = this.firstNote; note <= this.lastNote; note++) {
                if (isBlackKey(note) && Math.abs(x - this.whiteIndex(note) * whiteWidth) < halfBlackWidth) {
                    return note;
                }
            }
        }
        final int whiteKey = absoluteWhiteIndex(this.firstNote) + Math.min((int) (x / whiteWidth), this.whiteKeyCount - 1);
        return whiteKey / 7 * 12 + WHITE_KEY_OFFSETS[whiteKey % 7];
    }

    /**
     * @return the number of white keys left of the note, for a black key this is the index of the white key to its right
     */
    private int whiteIndex(final int note) {
        return absoluteWhiteIndex(note) - absoluteWhiteIndex(this.firstNote);
    }

    private void checkInRange(final int note) {
        if (note < this.firstNote || note > this.lastNote) {
            throw new IllegalArgumentException("Note " + note + " is outside the keyboard range " + this.firstNote + " - " + this.lastNote);
        }
    }

    public final ThemeOption<Float> whiteKeyWidth() {
        return this.whiteKeyWidth;
    }

    public final ThemeOption<Float> keyHeight() {
        return this.keyHeight;
    }

    public final ThemeOption<Float> blackKeyWidthRatio() {
        return this.blackKeyWidthRatio;
    }

    public final ThemeOption<Float> blackKeyHeightRatio() {
        return this.blackKeyHeightRatio;
    }

    public final ThemeOption<Float> cornerRadius() {
        return this.cornerRadius;
    }

    public final ThemeOption<Float> separatorWidth() {
        return this.separatorWidth;
    }

    public final ThemeOption<Color> whiteKeyColor() {
        return this.whiteKeyColor;
    }

    public final ThemeOption<Color> whiteKeyHoverColor() {
        return this.whiteKeyHoverColor;
    }

    public final ThemeOption<Color> whiteKeyPressedColor() {
        return this.whiteKeyPressedColor;
    }

    public final ThemeOption<Color> blackKeyColor() {
        return this.blackKeyColor;
    }

    public final ThemeOption<Color> blackKeyHoverColor() {
        return this.blackKeyHoverColor;
    }

    public final ThemeOption<Color> blackKeyPressedColor() {
        return this.blackKeyPressedColor;
    }

    public final ThemeOption<Color> separatorColor() {
        return this.separatorColor;
    }

    public final ThemeOption<Float> pressDepthRatio() {
        return this.pressDepthRatio;
    }

    public final ThemeOption<Float> faceHeightRatio() {
        return this.faceHeightRatio;
    }

    public final ThemeOption<Float> faceShade() {
        return this.faceShade;
    }

    public final ThemeOption<Float> labelScale() {
        return this.labelScale;
    }

    public final ThemeOption<Float> blackKeyLabelScale() {
        return this.blackKeyLabelScale;
    }

    public final ThemeOption<AnimationConfig> pressAnimation() {
        return this.pressAnimation;
    }

    public final ThemeOption<Color> shadowColor() {
        return this.shadowColor;
    }

    public final ThemeOption<Color> labelColor() {
        return this.labelColor;
    }

    public final ThemeOption<Color> blackKeyLabelColor() {
        return this.blackKeyLabelColor;
    }

    public final ThemeOption<Color> pressedLabelColor() {
        return this.pressedLabelColor;
    }

    public static boolean isBlackKey(final int note) {
        return switch (Math.floorMod(note, 12)) {
            case 1, 3, 6, 8, 10 -> true;
            default -> false;
        };
    }

    /**
     * @return the scientific pitch name, 60 is C4
     */
    public static String noteName(final int note) {
        return NOTE_NAMES[Math.floorMod(note, 12)] + (Math.floorDiv(note, 12) - 1);
    }

    /**
     * @return the number of white keys below the note, counted from MIDI note 0
     */
    private static int absoluteWhiteIndex(final int note) {
        return note / 12 * 7 + WHITE_KEYS_BELOW[note % 12];
    }

    private static void checkNote(final int note) {
        if (note < 0 || note > 127) {
            throw new IllegalArgumentException("Note " + note + " is outside the MIDI range 0 - 127");
        }
    }


    private record LabelStyle(Font font, float whiteScale, float blackScale, Color whiteColor, Color blackColor, Color pressedColor) {
    }

}
