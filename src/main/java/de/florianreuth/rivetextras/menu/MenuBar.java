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

import de.florianreuth.rivetextras.theme.ExtrasTheme;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ParentContainer;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.input.keyboard.KeyEvent;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.layer.Layer;
import net.lenni0451.rivet.layer.LayerBucket;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteOptions;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.property.ObjectProperty;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.lenni0451.rivet.utils.MathUtils.relativizeVisibleArea;

/**
 * Horizontal bar of menus, clicking a title opens the content of its menu in a popup below it.
 */
public class MenuBar extends ParentContainer {

    private final List<Menu> menus = new CopyOnWriteArrayList<>();
    private final ObjectProperty<Menu> openMenu = new ObjectProperty<>();

    private final ThemeOption<Padding> titlePadding = new ThemeOption<>(this, ExtrasTheme.MenuBar.TITLE_PADDING);
    private final ThemeOption<Padding> popupPadding = new ThemeOption<>(this, ExtrasTheme.MenuBar.POPUP_PADDING);
    private final ThemeOption<Float> minPopupWidth = new ThemeOption<>(this, ExtrasTheme.MenuBar.MIN_POPUP_WIDTH);
    private final ThemeOption<Float> outlineWidth = new ThemeOption<>(this, ExtrasTheme.MenuBar.OUTLINE_WIDTH);
    private final ThemeOption<Color> backgroundColor = new ThemeOption<>(this, ExtrasTheme.MenuBar.BACKGROUND_COLOR);
    private final ThemeOption<Color> titleHoverBackgroundColor = new ThemeOption<>(this, ExtrasTheme.MenuBar.TITLE_HOVER_BACKGROUND_COLOR);
    private final ThemeOption<Color> titleOpenBackgroundColor = new ThemeOption<>(this, ExtrasTheme.MenuBar.TITLE_OPEN_BACKGROUND_COLOR);
    private final ThemeOption<Color> popupBackgroundColor = new ThemeOption<>(this, ExtrasTheme.MenuBar.POPUP_BACKGROUND_COLOR);
    private final ThemeOption<Color> popupOutlineColor = new ThemeOption<>(this, ExtrasTheme.MenuBar.POPUP_OUTLINE_COLOR);

    // Written by the render thread, read by the input thread for hit testing
    private volatile TitleLayout titleLayout;
    private Layer popupLayer;
    private MenuPopup popup;
    private Menu hoveredMenu;

    public MenuBar() {
        this.openMenu.addValidator(menu -> {
            if (menu != null && !this.menus.contains(menu)) {
                throw new IllegalArgumentException("Menu is not part of this menu bar");
            }
            if (menu != null && this.rivet() == null) {
                throw new IllegalStateException("The menu bar has to be attached to open a menu");
            }
            return menu;
        });
        this.openMenu.syncListener().add(this::onOpenMenuChanged);
        this.positionUpdateListener().add(new PositionUpdateListener(() -> this.popup != null, bounds -> this.invalidatePopupLayout()));
        this.popupPadding.changeListener().add(padding -> this.invalidatePopupLayout());
        this.minPopupWidth.changeListener().add(width -> this.invalidatePopupLayout());
    }

    // The popup is positioned in its layout, which is cached until the popup itself is invalidated
    private void invalidatePopupLayout() {
        final MenuPopup popup = this.popup;
        if (popup != null) {
            popup.invalidateLayout();
        }
    }

    /**
     * @return an unmodifiable view of the menus in display order
     */
    public final List<Menu> menus() {
        return Collections.unmodifiableList(this.menus);
    }

    /**
     * @return the menu whose content is shown, holds {@code null} if all menus are closed
     */
    public final ObjectProperty<Menu> openMenu() {
        return this.openMenu;
    }

    public final MenuBar add(final String title, final Component content) {
        return this.add(new Label(title), content);
    }

    /**
     * @param title   shown in the bar, only displays, the input is handled by the menu bar
     * @param content shown in the popup, does not close the menu by itself when clicked, use {@link #close()}
     */
    public final MenuBar add(final Component title, final Component content) {
        this.menus.add(new Menu(title, content));
        if (this.rivet() != null) {
            title.setRivet(this.rivet(), this);
            this.invalidateLayout();
        }
        return this;
    }

    /**
     * @throws IllegalArgumentException if the menu is not part of this menu bar
     * @throws IllegalStateException    if the menu bar is not attached
     */
    public final MenuBar open(final Menu menu) {
        this.openMenu.set(menu);
        return this;
    }

    public final MenuBar close() {
        this.openMenu.set(null);
        return this;
    }

    private void onOpenMenuChanged(final Menu menu) {
        if (menu == null) {
            if (this.popupLayer != null) {
                final Layer layer = this.popupLayer;
                this.popupLayer = null;
                this.popup = null;
                final Rivet rivet = layer.container().rivet();
                // Disposing rivet removes all layers before the menu bar is detached
                if (rivet != null) {
                    rivet.removeLayer(layer);
                }
            }
        } else if (this.popupLayer == null) {
            this.popup = new MenuPopup(this, menu);
            final Container container = new Container(AbsoluteLayout.INSTANCE);
            container.add(this.popup, popup -> popup.layoutOptions(new AbsoluteOptions(0, 0, -1F, -1F)));
            this.popupLayer = new Layer(container, LayerBucket.OVERLAY);
            this.rivet().addLayer(this.popupLayer);
        } else {
            // Switching between menus keeps the layer, only the shown content changes
            this.popup.menu(menu);
        }
    }

    @Override
    protected void onRemovedInternal() {
        super.onRemovedInternal();
        this.resetState();
    }

    @Override
    protected void onDisabledInternal() {
        super.onDisabledInternal();
        this.resetState();
    }

    @Override
    protected void onMouseLeaveInternal() {
        super.onMouseLeaveInternal();
        this.hoveredMenu = null;
    }

    @Override
    protected boolean onKeyDownInternal(final KeyEvent event) {
        return this.handleKeyDown(event);
    }

    // The titles only display, mouse input is not forwarded to them

    @Override
    protected boolean onMouseDownInternal(final MouseButtonEvent event, final Size size) {
        if (event.button().equals(MouseButton.LEFT)) {
            final Rectangle bounds = this.absoluteBounds();
            this.handleMouseDown(bounds.x() + event.x(), bounds.y() + event.y());
        }
        return true;
    }

    @Override
    protected boolean onMouseUpInternal(final MouseButtonEvent event, final Size size) {
        return true;
    }

    @Override
    protected boolean onMouseMoveInternal(final MouseMoveEvent event, final Size size) {
        final Rectangle bounds = this.absoluteBounds();
        this.handleMouseMove(bounds.x() + event.x(), bounds.y() + event.y());
        return true;
    }

    // The input handlers below take absolute coordinates, they are shared with the popup which covers the whole screen

    final boolean handleKeyDown(final KeyEvent event) {
        if (event.key().equals(Key.ESCAPE) && this.openMenu.get() != null) {
            this.close();
            return true;
        }
        return false;
    }

    final void handleMouseDown(final float x, final float y) {
        final Menu menu = this.menuAt(x, y);
        if (menu != null) {
            this.openMenu.set(menu == this.openMenu.get() ? null : menu);
        } else {
            this.close();
        }
    }

    final void handleMouseMove(final float x, final float y) {
        final Menu menu = this.menuAt(x, y);
        this.hoveredMenu = menu;
        if (menu != null && this.openMenu.get() != null) {
            this.openMenu.set(menu);
        }
    }

    private void resetState() {
        this.close();
        this.hoveredMenu = null;
    }

    @Override
    protected void renderInternal(final Renderer renderer, final Size size, final Rectangle visibleArea) {
        final TitleLayout titleLayout = this.titleLayout;
        final Menu openMenu = this.openMenu.get();
        renderer.fillRect(0, 0, size.width(), size.height(), this.backgroundColor.get());
        for (int i = 0; i < titleLayout.menus().size(); i++) {
            final Menu menu = titleLayout.menus().get(i);
            final float x = titleLayout.edges()[i];
            final float width = titleLayout.edges()[i + 1] - x;
            if (menu == openMenu) {
                renderer.fillRect(x, 0, width, size.height(), this.titleOpenBackgroundColor.get());
            } else if (menu == this.hoveredMenu && openMenu == null) {
                renderer.fillRect(x, 0, width, size.height(), this.titleHoverBackgroundColor.get());
            }

            final Component title = menu.title();
            final Rectangle bounds = this.childBounds(title);
            renderer.translate(bounds.x(), bounds.y(), () -> {
                renderer.componentBounds(0, 0, bounds.width(), bounds.height(), () -> {
                    title.render(renderer, bounds.size(), relativizeVisibleArea(visibleArea, bounds));
                });
            });
        }
    }

    @Override
    protected Size idealSizeInternal(final Size constraints) {
        final Padding titlePadding = this.titlePadding.get();
        float width = 0;
        float height = 0;
        for (final Menu menu : this.menus) {
            final Size titleSize = menu.title().idealSize(constraints.minus(titlePadding)).clamp(menu.title());
            width += titleSize.width() + titlePadding.horizontal();
            height = Math.max(height, titleSize.height());
        }
        return new Size(width, height + titlePadding.vertical());
    }

    @Override
    protected void layoutInternal(final Size size) {
        final Padding titlePadding = this.titlePadding.get();
        final List<Menu> menus = List.copyOf(this.menus);
        final float[] edges = new float[menus.size() + 1];
        for (int i = 0; i < menus.size(); i++) {
            final Component title = menus.get(i).title();
            edges[i + 1] = edges[i] + title.idealSize(size.minus(titlePadding)).clamp(title).width() + titlePadding.horizontal();
        }
        this.titleLayout = new TitleLayout(menus, edges, titlePadding, size.height());
        for (final Menu menu : menus) {
            menu.title().layout(this.childBounds(menu.title()).size());
        }
        // A title can move without the menu bar moving, the position listener would not notice
        this.invalidatePopupLayout();
    }

    @Override
    public Size contentSize() {
        return Size.EMPTY;
    }

    @Override
    public List<Component> children() {
        return this.menus.stream().map(Menu::title).toList();
    }

    @Override
    public Rectangle childBounds(final Component component) {
        final TitleLayout titleLayout = this.titleLayout;
        final int index = titleLayout == null ? -1 : titleLayout.indexOf(component);
        if (index < 0) {
            return Rectangle.EMPTY;
        }

        final Padding padding = titleLayout.padding();
        final float x = titleLayout.edges()[index] + padding.left();
        final float width = titleLayout.edges()[index + 1] - titleLayout.edges()[index] - padding.horizontal();
        return new Rectangle(x, padding.top(), Math.max(0, width), Math.max(0, titleLayout.height() - padding.vertical()));
    }

    /**
     * @return the absolute bounds of the title area of the menu, the whole menu bar if it is not laid out yet
     */
    final Rectangle titleBounds(final Menu menu) {
        final Rectangle bounds = this.absoluteBounds();
        final TitleLayout titleLayout = this.titleLayout;
        final int index = titleLayout == null ? -1 : titleLayout.menus().indexOf(menu);
        if (index < 0) {
            return bounds;
        }
        return new Rectangle(bounds.x() + titleLayout.edges()[index], bounds.y(), titleLayout.edges()[index + 1] - titleLayout.edges()[index], bounds.height());
    }

    /**
     * @return the menu whose title area is under the absolute position, or {@code null} if there is none
     */
    private Menu menuAt(final float x, final float y) {
        final TitleLayout titleLayout = this.titleLayout;
        if (titleLayout == null) {
            return null;
        }

        final Rectangle bounds = this.absoluteBounds();
        if (y < bounds.y() || y >= bounds.maxY()) {
            return null;
        }
        for (int i = 0; i < titleLayout.menus().size(); i++) {
            if (x >= bounds.x() + titleLayout.edges()[i] && x < bounds.x() + titleLayout.edges()[i + 1]) {
                return titleLayout.menus().get(i);
            }
        }
        return null;
    }

    public final ThemeOption<Padding> titlePadding() {
        return this.titlePadding;
    }

    public final ThemeOption<Padding> popupPadding() {
        return this.popupPadding;
    }

    public final ThemeOption<Float> minPopupWidth() {
        return this.minPopupWidth;
    }

    public final ThemeOption<Float> outlineWidth() {
        return this.outlineWidth;
    }

    public final ThemeOption<Color> backgroundColor() {
        return this.backgroundColor;
    }

    public final ThemeOption<Color> titleHoverBackgroundColor() {
        return this.titleHoverBackgroundColor;
    }

    public final ThemeOption<Color> titleOpenBackgroundColor() {
        return this.titleOpenBackgroundColor;
    }

    public final ThemeOption<Color> popupBackgroundColor() {
        return this.popupBackgroundColor;
    }

    public final ThemeOption<Color> popupOutlineColor() {
        return this.popupOutlineColor;
    }


    /**
     * @param title   the component shown in the bar
     * @param content the component shown in the popup
     */
    public record Menu(Component title, Component content) {
    }

    /**
     * @param menus the menus at the time of the layout, menus added later are not laid out yet
     * @param edges the left edge of every title area followed by the right edge of the last one
     */
    private record TitleLayout(List<Menu> menus, float[] edges, Padding padding, float height) {
        private int indexOf(final Component title) {
            for (int i = 0; i < this.menus.size(); i++) {
                if (this.menus.get(i).title() == title) {
                    return i;
                }
            }
            return -1;
        }
    }

}
