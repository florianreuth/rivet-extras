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

package de.florianreuth.rivetextras.hex;

import de.florianreuth.rivetextras.theme.ExtrasTheme;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.Font;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.event.ListenerList;
import net.lenni0451.rivet.input.keyboard.CharEvent;
import net.lenni0451.rivet.input.keyboard.KeyEvent;
import net.lenni0451.rivet.input.keyboard.ModifierKey;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.property.BooleanProperty;
import net.lenni0451.rivet.property.IntegerProperty;
import net.lenni0451.rivet.property.LongProperty;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.ThemeOption;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static de.florianreuth.rivetextras.utils.HexUtils.FIRST_PRINTABLE;
import static de.florianreuth.rivetextras.utils.HexUtils.LAST_PRINTABLE;
import static de.florianreuth.rivetextras.utils.HexUtils.hexChar;
import static de.florianreuth.rivetextras.utils.HexUtils.hexDigit;
import static de.florianreuth.rivetextras.utils.HexUtils.isPrintable;
import static de.florianreuth.rivetextras.utils.HexUtils.parseHex;
import static de.florianreuth.rivetextras.utils.HexUtils.toAscii;
import static de.florianreuth.rivetextras.utils.HexUtils.toHex;

/**
 * Hex editor showing bytes as hex values and ASCII characters side by side, the length of the data is fixed and edits overwrite bytes.
 */
public class HexEditor extends Component {

    private static final int MIN_ADDRESS_DIGITS = 8;

    private volatile byte[] data;
    private final ListenerList<ByteChangeListener> byteChangeListener = new ListenerList<>();
    private final IntegerProperty bytesPerRow = new IntegerProperty(16);
    private final LongProperty baseAddress = new LongProperty(0);
    private final BooleanProperty readOnly = new BooleanProperty(false);

    private final ThemeOption<Padding> padding = new ThemeOption<>(this, ExtrasTheme.HexEditor.PADDING);
    private final ThemeOption<Float> columnGap = new ThemeOption<>(this, ExtrasTheme.HexEditor.COLUMN_GAP);
    private final ThemeOption<Integer> groupSize = new ThemeOption<>(this, ExtrasTheme.HexEditor.GROUP_SIZE);
    private final ThemeOption<Float> separatorWidth = new ThemeOption<>(this, ExtrasTheme.HexEditor.SEPARATOR_WIDTH);
    private final ThemeOption<Color> backgroundColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.BACKGROUND_COLOR);
    private final ThemeOption<Color> addressBackgroundColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.ADDRESS_BACKGROUND_COLOR);
    private final ThemeOption<Color> separatorColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.SEPARATOR_COLOR);
    private final ThemeOption<Color> addressColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.ADDRESS_COLOR);
    private final ThemeOption<Color> textColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.TEXT_COLOR);
    private final ThemeOption<Color> zeroByteColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.ZERO_BYTE_COLOR);
    private final ThemeOption<Color> nonPrintableColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.NON_PRINTABLE_COLOR);
    private final ThemeOption<Color> cursorColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.CURSOR_COLOR);
    private final ThemeOption<Color> selectionColor = new ThemeOption<>(this, ExtrasTheme.HexEditor.SELECTION_COLOR);

    // Written by the render thread, read by the input thread for hit testing and scrolling
    private volatile Metrics metrics;
    private volatile float visibleHeight;
    // Only accessed from the render thread, glyphs are drawn one by one so the columns line up with proportional fonts
    private TextStyle textStyle;
    private ShapedText[] digits;
    private ShapedText[] zeroDigits;
    private ShapedText[] addressDigits;
    private ShapedText[] printableChars;
    private ShapedText nonPrintableChar;
    private float digitWidth;
    private float charWidth;

    private int cursor;
    private int selectionAnchor;
    private boolean lowNibble;
    private boolean asciiColumn;
    private boolean focused;
    private boolean selecting;

    public HexEditor() {
        this(new byte[0]);
    }

    /**
     * @param data the initial data, copied
     */
    public HexEditor(final byte[] data) {
        this.data = data.clone();
        this.bytesPerRow.addValidator(bytesPerRow -> {
            if (bytesPerRow < 1) {
                throw new IllegalArgumentException("Bytes per row must be at least 1, got " + bytesPerRow);
            }
            return bytesPerRow;
        });
        this.baseAddress.addValidator(baseAddress -> {
            if (baseAddress < 0) {
                throw new IllegalArgumentException("Base address must not be negative, got " + baseAddress);
            }
            return baseAddress;
        });
        this.bytesPerRow.syncListener().add(bytesPerRow -> this.invalidateLayout());
        // The address column grows with the highest address
        this.baseAddress.syncListener().add(baseAddress -> this.invalidateLayout());
    }

    /**
     * @return a copy of the current data
     */
    public final byte[] data() {
        return this.data.clone();
    }

    /**
     * Replaces the data without calling the byte change listeners, the cursor is kept if it is still inside the data.
     *
     * @param data the new data, copied
     */
    public final HexEditor data(final byte[] data) {
        this.data = data.clone();
        final int maxOffset = Math.max(0, data.length - 1);
        this.cursor = Math.min(this.cursor, maxOffset);
        this.selectionAnchor = Math.min(this.selectionAnchor, maxOffset);
        this.invalidateLayout();
        return this;
    }

    public final int length() {
        return this.data.length;
    }

    /**
     * @throws IndexOutOfBoundsException if the offset is outside the data
     */
    public final byte get(final int offset) {
        return this.data[offset];
    }

    /**
     * Overwrites a byte, also when the editor is read only, the byte change listeners are only called if the value changes.
     *
     * @throws IndexOutOfBoundsException if the offset is outside the data
     */
    public final HexEditor set(final int offset, final byte value) {
        final byte[] data = this.data;
        final byte oldValue = data[offset];
        if (oldValue != value) {
            data[offset] = value;
            this.byteChangeListener.call(listener -> listener.onByteChange(offset, oldValue, value));
        }
        return this;
    }

    /**
     * @return listeners called when a byte changes, by typing, pasting or {@link #set(int, byte)}
     */
    public final ListenerList<ByteChangeListener> byteChangeListener() {
        return this.byteChangeListener;
    }

    /**
     * @return the number of bytes shown in one row, at least 1
     */
    public final IntegerProperty bytesPerRow() {
        return this.bytesPerRow;
    }

    /**
     * @throws IllegalArgumentException if the value is less than 1
     */
    public final HexEditor bytesPerRow(final int bytesPerRow) {
        this.bytesPerRow.set(bytesPerRow);
        return this;
    }

    /**
     * @return the address shown for the first byte, e.g. the memory location the data was read from
     */
    public final LongProperty baseAddress() {
        return this.baseAddress;
    }

    /**
     * @throws IllegalArgumentException if the address is negative
     */
    public final HexEditor baseAddress(final long baseAddress) {
        this.baseAddress.set(baseAddress);
        return this;
    }

    /**
     * @return whether typing and pasting is blocked, navigating, selecting and copying still works
     */
    public final BooleanProperty readOnly() {
        return this.readOnly;
    }

    public final HexEditor readOnly(final boolean readOnly) {
        this.readOnly.set(readOnly);
        return this;
    }

    /**
     * @return the offset of the byte the cursor is on, 0 if the data is empty
     */
    public final int cursor() {
        return this.cursor;
    }

    /**
     * Moves the cursor and clears the selection.
     *
     * @throws IndexOutOfBoundsException if the offset is outside the data
     */
    public final HexEditor cursor(final int offset) {
        return this.select(offset, offset + 1);
    }

    /**
     * @return the offset of the first selected byte, the cursor if nothing is selected
     */
    public final int selectionStart() {
        return Math.min(this.cursor, this.selectionAnchor);
    }

    /**
     * @return the offset after the last selected byte
     */
    public final int selectionEnd() {
        return Math.max(this.cursor, this.selectionAnchor) + 1;
    }

    /**
     * Selects a range of bytes, the cursor is placed on the last selected byte.
     *
     * @param start the offset of the first selected byte, inclusive
     * @param end   the offset after the last selected byte, exclusive
     * @throws IndexOutOfBoundsException if the range is empty or outside the data
     */
    public final HexEditor select(final int start, final int end) {
        if (start < 0 || end > this.data.length || start >= end) {
            throw new IndexOutOfBoundsException("Invalid selection " + start + " - " + end + " for length " + this.data.length);
        }
        this.selectionAnchor = start;
        this.cursor = end - 1;
        this.lowNibble = false;
        this.scrollCursorIntoView();
        return this;
    }

    @Override
    protected void onRemovedInternal() {
        this.focused = false;
        this.selecting = false;
    }

    @Override
    protected void onDisabledInternal() {
        this.selecting = false;
    }

    @Override
    protected void onFocusGainedInternal() {
        this.focused = true;
    }

    @Override
    protected void onFocusLostInternal() {
        this.focused = false;
        this.selecting = false;
    }

    @Override
    protected boolean onMouseDownInternal(final MouseButtonEvent event, final Size size) {
        if (event.button().equals(MouseButton.LEFT)) {
            final Hit hit = this.hitAt(event.x(), event.y());
            if (hit != null) {
                this.cursor = hit.offset();
                if (!event.modifiers().contains(ModifierKey.SHIFT)) {
                    this.selectionAnchor = hit.offset();
                }
                this.asciiColumn = hit.asciiColumn();
                this.lowNibble = hit.lowNibble();
                this.selecting = true;
                this.scrollCursorIntoView();
            }
        }
        return true;
    }

    @Override
    protected boolean onMouseUpInternal(final MouseButtonEvent event, final Size size) {
        if (event.button().equals(MouseButton.LEFT)) {
            this.selecting = false;
        }
        return true;
    }

    @Override
    protected boolean onMouseMoveInternal(final MouseMoveEvent event, final Size size) {
        if (this.selecting) {
            final Hit hit = this.hitAt(event.x(), event.y());
            if (hit != null) {
                this.cursor = hit.offset();
                this.lowNibble = false;
                this.scrollCursorIntoView();
            }
        }
        return true;
    }

    @Override
    protected boolean onKeyDownInternal(final KeyEvent event) {
        final int length = this.data.length;
        if (length == 0) {
            return false;
        }

        final boolean shift = event.modifiers().contains(ModifierKey.SHIFT);
        final boolean control = event.modifiers().contains(ModifierKey.CONTROL);
        final int bytesPerRow = this.bytesPerRow.get();
        final int rowStart = this.cursor - this.cursor % bytesPerRow;
        switch (event.key()) {
            case LEFT -> {
                if (!this.asciiColumn && this.lowNibble && !shift) {
                    this.lowNibble = false;
                } else {
                    this.moveCursor(this.cursor - 1, shift);
                }
            }
            case RIGHT -> this.moveCursor(this.cursor + 1, shift);
            case UP -> this.moveCursor(this.cursor - bytesPerRow, shift);
            case DOWN -> this.moveCursor(this.cursor + bytesPerRow, shift);
            case PAGE_UP -> this.moveCursor(this.cursor - this.pageRows() * bytesPerRow, shift);
            case PAGE_DOWN -> this.moveCursor(this.cursor + this.pageRows() * bytesPerRow, shift);
            case HOME -> this.moveCursor(control ? 0 : rowStart, shift);
            case END -> this.moveCursor(control ? length - 1 : rowStart + bytesPerRow - 1, shift);
            case TAB -> {
                this.asciiColumn = !this.asciiColumn;
                this.lowNibble = false;
                this.scrollCursorIntoView();
            }
            case A -> {
                if (!control) {
                    return false;
                }
                this.selectionAnchor = 0;
                this.cursor = length - 1;
                this.lowNibble = false;
            }
            case C -> {
                if (!control) {
                    return false;
                }
                this.rivet().backend().setClipboard(this.selectionText());
            }
            case V -> {
                if (!control || this.readOnly.get()) {
                    return false;
                }
                this.paste(this.rivet().backend().getClipboard());
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean onCharTypedInternal(final CharEvent event) {
        final int length = this.data.length;
        if (length == 0 || this.readOnly.get()) {
            return false;
        }

        final int codePoint = event.codePoint();
        if (this.asciiColumn) {
            if (codePoint < FIRST_PRINTABLE || codePoint > LAST_PRINTABLE) {
                return false;
            }
            this.set(this.cursor, (byte) codePoint);
            this.cursor = Math.min(this.cursor + 1, length - 1);
        } else {
            final int digit = hexDigit(codePoint);
            if (digit < 0) {
                return false;
            }
            final int oldValue = this.data[this.cursor];
            this.set(this.cursor, (byte) (this.lowNibble ? (oldValue & 0xF0) | digit : (digit << 4) | (oldValue & 0x0F)));
            if (!this.lowNibble) {
                this.lowNibble = true;
            } else if (this.cursor < length - 1) {
                this.cursor++;
                this.lowNibble = false;
            }
        }
        this.selectionAnchor = this.cursor;
        this.scrollCursorIntoView();
        return true;
    }

    private void moveCursor(final int offset, final boolean extendSelection) {
        this.cursor = MathUtils.clamp(offset, 0, Math.max(0, this.data.length - 1));
        if (!extendSelection) {
            this.selectionAnchor = this.cursor;
        }
        this.lowNibble = false;
        this.scrollCursorIntoView();
    }

    private int pageRows() {
        final Metrics metrics = this.metrics;
        if (metrics == null) {
            return 1;
        }
        // One row overlaps with the previous page to keep the context
        return Math.max(1, (int) (this.visibleHeight / metrics.lineHeight()) - 1);
    }

    private String selectionText() {
        final byte[] data = this.data;
        final int start = Math.min(this.selectionStart(), data.length);
        final int end = Math.min(this.selectionEnd(), data.length);
        return this.asciiColumn ? toAscii(data, start, end) : toHex(data, start, end);
    }

    private void paste(final String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        final byte[] bytes = this.asciiColumn ? text.getBytes(StandardCharsets.ISO_8859_1) : parseHex(text);
        if (bytes == null) {
            return;
        }

        final int start = this.selectionStart();
        final int count = Math.min(bytes.length, this.data.length - start);
        for (int i = 0; i < count; i++) {
            this.set(start + i, bytes[i]);
        }
        this.moveCursor(start + count, false);
    }

    private void scrollCursorIntoView() {
        final Metrics metrics = this.metrics;
        if (metrics == null || this.data.length == 0) {
            return;
        }

        final int row = this.cursor / metrics.bytesPerRow();
        final int column = this.cursor % metrics.bytesPerRow();
        final float x = this.asciiColumn ? metrics.charX(column) : metrics.byteX(column);
        final float width = this.asciiColumn ? metrics.charWidth() : metrics.digitWidth() * 2;
        // The neighboring rows stay visible so the context around the cursor can be seen
        final float y = Math.max(0, metrics.rowY(row) - metrics.lineHeight());
        this.scrollIntoView(new Rectangle(x, y, width, metrics.rowY(row) + metrics.lineHeight() * 2 - y));
    }

    private Hit hitAt(final float x, final float y) {
        final Metrics metrics = this.metrics;
        final int length = this.data.length;
        if (metrics == null || length == 0) {
            return null;
        }

        final int bytesPerRow = metrics.bytesPerRow();
        final int rows = (length + bytesPerRow - 1) / bytesPerRow;
        final int row = MathUtils.clamp((int) Math.floor((y - metrics.padding().top()) / metrics.lineHeight()), 0, rows - 1);
        final boolean asciiColumn = x >= (metrics.hexEndX() + metrics.asciiX()) / 2F;
        int column = bytesPerRow - 1;
        boolean lowNibble = false;
        if (asciiColumn) {
            column = MathUtils.clamp((int) Math.floor((x - metrics.asciiX()) / metrics.charWidth()), 0, bytesPerRow - 1);
        } else {
            for (int i = 0; i < bytesPerRow - 1; i++) {
                // The space between two bytes is split between them
                if (x < (metrics.byteX(i) + metrics.digitWidth() * 2 + metrics.byteX(i + 1)) / 2F) {
                    column = i;
                    break;
                }
            }
            lowNibble = x >= metrics.byteX(column) + metrics.digitWidth();
        }

        final int offset = row * bytesPerRow + column;
        if (offset >= length) {
            return new Hit(length - 1, asciiColumn, false);
        }
        return new Hit(offset, asciiColumn, lowNibble);
    }

    @Override
    protected void renderInternal(final Renderer renderer, final Size size, final Rectangle visibleArea) {
        this.updateTextStyle();
        final Metrics metrics = this.computeMetrics();
        this.metrics = metrics;
        final Rectangle visible = visibleArea.intersection(new Rectangle(size));
        this.visibleHeight = visible.height();
        if (visible.width() <= 0 || visible.height() <= 0) {
            return;
        }

        final byte[] data = this.data;
        final float separatorWidth = this.separatorWidth.get();
        final Color separatorColor = this.separatorColor.get();
        final float addressEndX = metrics.hexX() - metrics.columnGap() / 2F;
        final float asciiStartX = (metrics.hexEndX() + metrics.asciiX()) / 2F;
        renderer.fillRect(0, 0, size.width(), size.height(), this.backgroundColor.get());
        renderer.fillRect(0, 0, addressEndX, size.height(), this.addressBackgroundColor.get());
        renderer.fillRect(addressEndX - separatorWidth / 2F, 0, separatorWidth, size.height(), separatorColor);
        renderer.fillRect(asciiStartX - separatorWidth / 2F, 0, separatorWidth, size.height(), separatorColor);

        final int bytesPerRow = metrics.bytesPerRow();
        final int rows = Math.max(1, (data.length + bytesPerRow - 1) / bytesPerRow);
        final float lineHeight = metrics.lineHeight();
        final float top = metrics.padding().top();
        final int firstRow = MathUtils.clamp((int) Math.floor((visible.y() - top) / lineHeight), 0, rows - 1);
        final int lastRow = MathUtils.clamp((int) Math.floor((visible.maxY() - top) / lineHeight), 0, rows - 1);
        // The data can be replaced from another thread, the cursor is only clamped by the next input
        final int cursor = Math.min(this.cursor, data.length - 1);
        final int selectionStart = Math.min(this.selectionStart(), data.length);
        final int selectionEnd = Math.min(this.selectionEnd(), data.length);
        final boolean hasSelection = selectionEnd - selectionStart > 1;
        final long baseAddress = this.baseAddress.get();
        for (int row = firstRow; row <= lastRow; row++) {
            final float y = metrics.rowY(row);
            final int rowStart = row * bytesPerRow;
            final int rowEnd = Math.min(rowStart + bytesPerRow, data.length);

            this.renderAddress(renderer, metrics, baseAddress + (long) rowStart, y);
            if (hasSelection) {
                this.renderSelection(renderer, metrics, Math.max(selectionStart, rowStart) - rowStart, Math.min(selectionEnd, rowEnd) - rowStart, y);
            }
            if (cursor >= rowStart && cursor < rowEnd) {
                this.renderCursor(renderer, metrics, cursor - rowStart, y, separatorWidth);
            }
            for (int offset = rowStart; offset < rowEnd; offset++) {
                this.renderByte(renderer, metrics, data[offset] & 0xFF, offset - rowStart, y);
            }
        }
    }

    private void renderAddress(final Renderer renderer, final Metrics metrics, final long address, final float y) {
        for (int i = 0; i < metrics.addressDigits(); i++) {
            final int digit = (int) (address >>> ((metrics.addressDigits() - 1 - i) * 4)) & 0x0F;
            this.renderGlyph(renderer, this.addressDigits[digit], metrics.padding().left() + i * metrics.digitWidth(), metrics.digitWidth(), y);
        }
    }

    private void renderSelection(final Renderer renderer, final Metrics metrics, final int startColumn, final int endColumn, final float y) {
        if (startColumn >= endColumn) {
            return;
        }

        final Color selectionColor = this.selectionColor.get();
        final float hexStart = metrics.byteX(startColumn);
        renderer.fillRect(hexStart, y, metrics.byteX(endColumn - 1) + metrics.digitWidth() * 2 - hexStart, metrics.lineHeight(), selectionColor);
        final float asciiStart = metrics.charX(startColumn);
        renderer.fillRect(asciiStart, y, metrics.charX(endColumn) - asciiStart, metrics.lineHeight(), selectionColor);
    }

    private void renderCursor(final Renderer renderer, final Metrics metrics, final int column, final float y, final float outlineWidth) {
        final Color cursorColor = this.cursorColor.get();
        final float lineHeight = metrics.lineHeight();
        final float hexX = metrics.byteX(column);
        final float charX = metrics.charX(column);
        // The active column shows a filled cursor while focused, the other column only marks the same byte
        if (this.focused && !this.asciiColumn) {
            renderer.fillRect(hexX + (this.lowNibble ? metrics.digitWidth() : 0), y, metrics.digitWidth(), lineHeight, cursorColor);
        } else {
            renderer.outlineRect(hexX, y, metrics.digitWidth() * 2, lineHeight, outlineWidth, cursorColor);
        }
        if (this.focused && this.asciiColumn) {
            renderer.fillRect(charX, y, metrics.charWidth(), lineHeight, cursorColor);
        } else {
            renderer.outlineRect(charX, y, metrics.charWidth(), lineHeight, outlineWidth, cursorColor);
        }
    }

    private void renderByte(final Renderer renderer, final Metrics metrics, final int value, final int column, final float y) {
        final ShapedText[] digits = value == 0 ? this.zeroDigits : this.digits;
        final float x = metrics.byteX(column);
        this.renderGlyph(renderer, digits[value >> 4], x, metrics.digitWidth(), y);
        this.renderGlyph(renderer, digits[value & 0x0F], x + metrics.digitWidth(), metrics.digitWidth(), y);
        final ShapedText character = isPrintable(value) ? this.printableChars[value - FIRST_PRINTABLE] : this.nonPrintableChar;
        this.renderGlyph(renderer, character, metrics.charX(column), metrics.charWidth(), y);
    }

    private void renderGlyph(final Renderer renderer, final ShapedText glyph, final float x, final float width, final float y) {
        renderer.text(glyph, x + width / 2F, y, TextOrigin.Horizontal.VISUAL_CENTER, TextOrigin.Vertical.LOGICAL_TOP);
    }

    private void updateTextStyle() {
        final TextStyle style = new TextStyle(this.rivet().backend().font(), this.textColor.get(), this.zeroByteColor.get(), this.addressColor.get(), this.nonPrintableColor.get());
        if (style.equals(this.textStyle)) {
            return;
        }

        this.textStyle = style;
        this.digits = shapeDigits(style.font(), style.textColor());
        this.zeroDigits = shapeDigits(style.font(), style.zeroByteColor());
        this.addressDigits = shapeDigits(style.font(), style.addressColor());
        this.printableChars = new ShapedText[LAST_PRINTABLE - FIRST_PRINTABLE + 1];
        for (int c = FIRST_PRINTABLE; c <= LAST_PRINTABLE; c++) {
            this.printableChars[c - FIRST_PRINTABLE] = style.font().shapeText(String.valueOf((char) c), style.textColor());
        }
        this.nonPrintableChar = style.font().shapeText(".", style.nonPrintableColor());
        this.digitWidth = maxWidth(this.digits);
        this.charWidth = maxWidth(this.printableChars);
    }

    private Metrics computeMetrics() {
        final int length = this.data.length;
        final long highestAddress = this.baseAddress.get() + Math.max(0, length - 1);
        final int addressDigits = Math.max(MIN_ADDRESS_DIGITS, Long.toHexString(highestAddress).length());
        return new Metrics(this.digitWidth, this.charWidth, this.rivet().backend().font().height(), this.padding.get(), this.columnGap.get(),
                this.bytesPerRow.get(), this.groupSize.get(), addressDigits);
    }

    @Override
    protected Size idealSizeInternal(final Size constraints) {
        this.updateTextStyle();
        final Metrics metrics = this.computeMetrics();
        final int rows = Math.max(1, (this.data.length + metrics.bytesPerRow() - 1) / metrics.bytesPerRow());
        return new Size(metrics.charX(metrics.bytesPerRow()) + metrics.padding().right(), metrics.rowY(rows) + metrics.padding().bottom());
    }

    private static ShapedText[] shapeDigits(final Font font, final Color color) {
        final ShapedText[] digits = new ShapedText[16];
        for (int i = 0; i < digits.length; i++) {
            digits[i] = font.shapeText(String.valueOf(hexChar(i)), color);
        }
        return digits;
    }

    private static float maxWidth(final ShapedText[] glyphs) {
        return (float) Arrays.stream(glyphs).mapToDouble(glyph -> glyph.logicalBounds().width()).max().orElse(0);
    }

    public final ThemeOption<Padding> padding() {
        return this.padding;
    }

    public final ThemeOption<Float> columnGap() {
        return this.columnGap;
    }

    public final ThemeOption<Integer> groupSize() {
        return this.groupSize;
    }

    public final ThemeOption<Float> separatorWidth() {
        return this.separatorWidth;
    }

    public final ThemeOption<Color> backgroundColor() {
        return this.backgroundColor;
    }

    public final ThemeOption<Color> addressBackgroundColor() {
        return this.addressBackgroundColor;
    }

    public final ThemeOption<Color> separatorColor() {
        return this.separatorColor;
    }

    public final ThemeOption<Color> addressColor() {
        return this.addressColor;
    }

    public final ThemeOption<Color> textColor() {
        return this.textColor;
    }

    public final ThemeOption<Color> zeroByteColor() {
        return this.zeroByteColor;
    }

    public final ThemeOption<Color> nonPrintableColor() {
        return this.nonPrintableColor;
    }

    public final ThemeOption<Color> cursorColor() {
        return this.cursorColor;
    }

    public final ThemeOption<Color> selectionColor() {
        return this.selectionColor;
    }


    @FunctionalInterface
    public interface ByteChangeListener {
        void onByteChange(final int offset, final byte oldValue, final byte newValue);
    }

    private record TextStyle(Font font, Color textColor, Color zeroByteColor, Color addressColor, Color nonPrintableColor) {
    }

    private record Hit(int offset, boolean asciiColumn, boolean lowNibble) {
    }

    /**
     * Geometry of the editor, all positions are relative to the component.
     *
     * @param digitWidth the width of the widest hex digit, a byte takes two digits and is followed by one digit of space
     * @param groupSize  bytes per group, groups are separated by one additional digit of space
     */
    private record Metrics(float digitWidth, float charWidth, float lineHeight, Padding padding, float columnGap,
                           int bytesPerRow, int groupSize, int addressDigits) {
        private float hexX() {
            return this.padding.left() + this.addressDigits * this.digitWidth + this.columnGap;
        }

        private float byteX(final int column) {
            final int groups = this.groupSize > 0 ? column / this.groupSize : 0;
            return this.hexX() + (column * 3 + groups) * this.digitWidth;
        }

        private float hexEndX() {
            return this.byteX(this.bytesPerRow - 1) + this.digitWidth * 2;
        }

        private float asciiX() {
            return this.hexEndX() + this.columnGap;
        }

        private float charX(final int column) {
            return this.asciiX() + column * this.charWidth;
        }

        private float rowY(final int row) {
            return this.padding.top() + row * this.lineHeight;
        }
    }

}
