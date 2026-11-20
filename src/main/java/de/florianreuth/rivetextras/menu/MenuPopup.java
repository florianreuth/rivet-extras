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

package de.florianreuth.rivetextras.menu;

import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ParentContainer;
import net.lenni0451.rivet.input.keyboard.KeyEvent;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.input.mouse.MouseScrollEvent;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.List;

import static net.lenni0451.rivet.utils.MathUtils.relativizeVisibleArea;

/**
 * Covers the whole screen while a menu is open and shows the content of the open menu below its title.
 */
final class MenuPopup extends ParentContainer {

    private final MenuBar menuBar;
    private MenuBar.Menu menu;
    // Written by the render thread, read by the input thread for hit testing
    private volatile Rectangle panelBounds = Rectangle.EMPTY;
    private volatile Rectangle contentBounds = Rectangle.EMPTY;

    MenuPopup(final MenuBar menuBar, final MenuBar.Menu menu) {
        this.menuBar = menuBar;
        this.menu = menu;
    }

    void menu(final MenuBar.Menu menu) {
        if (menu == this.menu) {
            return;
        }

        // Only called while no mouse button is held, the hovered content would otherwise keep receiving events
        this.mouseHandler().onMouseLeave();
        if (this.rivet() != null) {
            this.menu.content().setRivet(null, null);
            this.menu = menu;
            menu.content().setRivet(this.rivet(), this);
            this.invalidateLayout();
        } else {
            this.menu = menu;
        }
    }

    @Override
    protected boolean onKeyDownInternal(final KeyEvent event) {
        return this.menuBar.handleKeyDown(event);
    }

    // All mouse events are consumed, a click outside the popup only closes it and must not reach the components below

    @Override
    protected boolean onMouseDownInternal(final MouseButtonEvent event, final Size size) {
        if (this.panelBounds.contains(event.x(), event.y())) {
            super.onMouseDownInternal(event, size);
        } else if (event.button().equals(MouseButton.LEFT)) {
            this.menuBar.handleMouseDown(event.x(), event.y());
        }
        return true;
    }

    @Override
    protected boolean onMouseUpInternal(final MouseButtonEvent event, final Size size) {
        super.onMouseUpInternal(event, size);
        return true;
    }

    @Override
    protected boolean onMouseMoveInternal(final MouseMoveEvent event, final Size size) {
        // Hovering another title switches the menu, but not while dragging inside of the content
        if (!this.mouseHandler().isMouseHeld()) {
            this.menuBar.handleMouseMove(event.x(), event.y());
        }
        super.onMouseMoveInternal(event, size);
        return true;
    }

    @Override
    protected boolean onMouseScrollInternal(final MouseScrollEvent event, final Size size) {
        super.onMouseScrollInternal(event, size);
        return true;
    }

    @Override
    protected void renderInternal(final Renderer renderer, final Size size, final Rectangle visibleArea) {
        final Rectangle panelBounds = this.panelBounds;
        final Rectangle contentBounds = this.contentBounds;
        final float outlineWidth = this.menuBar.outlineWidth().get();
        renderer.fillRect(panelBounds.x(), panelBounds.y(), panelBounds.width(), panelBounds.height(), this.menuBar.popupBackgroundColor().get());
        final Component content = this.menu.content();
        renderer.translate(contentBounds.x(), contentBounds.y(), () -> {
            renderer.scissor(0, 0, contentBounds.width(), contentBounds.height(), () -> {
                content.render(renderer, contentBounds.size(), relativizeVisibleArea(visibleArea, contentBounds));
            });
        });
        if (outlineWidth > 0) {
            renderer.outlineRect(panelBounds.x(), panelBounds.y(), panelBounds.width(), panelBounds.height(), outlineWidth, this.menuBar.popupOutlineColor().get());
        }
    }

    @Override
    protected Size idealSizeInternal(final Size constraints) {
        return Size.EMPTY;
    }

    @Override
    protected void layoutInternal(final Size size) {
        final Component content = this.menu.content();
        final Padding padding = this.menuBar.popupPadding().get();
        final Rectangle titleBounds = this.menuBar.titleBounds(this.menu);
        final float y = titleBounds.maxY();
        final Size idealSize = content.idealSize(size.minus(padding)).clamp(content);
        final float width = Math.min(Math.max(this.menuBar.minPopupWidth().get(), idealSize.width() + padding.horizontal()), size.width());
        final float height = Math.max(0, Math.min(idealSize.height() + padding.vertical(), size.height() - y));
        // Moved left if it would leave the screen, otherwise aligned with the title
        final float x = MathUtils.clamp(titleBounds.x(), 0, Math.max(0, size.width() - width));
        this.panelBounds = new Rectangle(x, y, width, height);
        this.contentBounds = new Rectangle(x + padding.left(), y + padding.top(), Math.max(0, width - padding.horizontal()), Math.max(0, height - padding.vertical()));
        content.layout(this.contentBounds.size());
    }

    @Override
    public Size contentSize() {
        return Size.EMPTY;
    }

    @Override
    public List<Component> children() {
        return List.of(this.menu.content());
    }

    @Override
    public Rectangle childBounds(final Component component) {
        if (component == this.menu.content()) {
            return this.contentBounds;
        }
        return Rectangle.EMPTY;
    }

}
