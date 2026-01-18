package com.xtracr.realcamera.config;

import com.google.common.collect.ImmutableList;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemSelectorListEntry extends AbstractListListEntry<String, ItemSelectorListEntry.ItemSelectorCell, ItemSelectorListEntry> {
    public ItemSelectorListEntry(Component fieldName, List<String> value, boolean defaultExpanded,
                                 Supplier<Optional<Component[]>> tooltipSupplier,
                                 Consumer<List<String>> saveConsumer,
                                 Supplier<List<String>> defaultValue,
                                 Component resetButtonKey,
                                 boolean requiresRestart,
                                 boolean deleteButtonEnabled,
                                 boolean insertInFront,
                                 List<String> selections) {
        super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart,
                deleteButtonEnabled, insertInFront, (val, entry) -> new ItemSelectorCell(val, entry, selections));
    }

    @Override
    public ItemSelectorListEntry self() {
        return this;
    }

    @Override
    public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!isExpanded()) {
            return;
        }
        for (ItemSelectorCell cell : cells) {
            cell.lateRender(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public int getMorePossibleHeight() {
        if (!isExpanded()) {
            return -1;
        }
        int max = -1;
        for (ItemSelectorCell cell : cells) {
            max = Math.max(max, cell.getMorePossibleHeight());
        }
        return max;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isExpanded()) {
            for (ItemSelectorCell cell : cells) {
                if (cell.handleMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    static class ItemSelectorCell extends AbstractListCell<String, ItemSelectorCell, ItemSelectorListEntry> {
        private static final int CELL_HEIGHT = 20;
        private final InlineDropdownEntry dropdownEntry;
        private final SizedSelectionElement<String> selectionElement;
        private boolean selected;
        private boolean hovered;
        private boolean wasLeftPressed;

        ItemSelectorCell(String value, ItemSelectorListEntry listListEntry, List<String> selections) {
            super(value == null ? "" : value, listListEntry);
            String initial = value == null ? "" : value;
            DropdownBoxEntry.SelectionTopCellElement<String> topCell =
                    DropdownMenuBuilder.TopCellElementBuilder.of(initial, Function.identity(), Component::literal);
            dropdownEntry = new InlineDropdownEntry(Component.empty(), Component.empty(), null, false,
                    () -> initial, v -> {
                    }, selections, topCell, new ItemIdCellCreator());
            selectionElement = dropdownEntry.getSizedSelectionElement();
            dropdownEntry.setSuggestionMode(true);
        }

        @Override
        public int getCellHeight() {
            return CELL_HEIGHT;
        }

        @Override
        public void updateSelected(boolean isSelected) {
            selected = isSelected;
            dropdownEntry.updateSelected(isSelected);
            if (!isSelected) {
                dropdownEntry.setFocused(null);
            }
        }

        @Override
        public String getValue() {
            return dropdownEntry.getValue();
        }

        @Override
        public Optional<Component> getError() {
            return Optional.empty();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(selectionElement);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            if (!ensureScreen()) {
                return;
            }
            int controlWidth = entryWidth - 12;
            int controlX = x;
            int controlY = y;
            dropdownEntry.renderInline(graphics, controlX, controlY, controlWidth, CELL_HEIGHT, mouseX, mouseY, isSelected, delta);
            handleOutsideClick(mouseX, mouseY);
            hovered = mouseX >= controlX && mouseX <= controlX + controlWidth && mouseY >= controlY && mouseY <= controlY + CELL_HEIGHT;
        }

        private boolean ensureScreen() {
            if (dropdownEntry.getConfigScreen() == null) {
                if (listListEntry.getConfigScreen() == null) {
                    return false;
                }
                dropdownEntry.setScreen(listListEntry.getConfigScreen());
            }
            return true;
        }

        void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (ensureScreen()) {
                dropdownEntry.lateRender(graphics, mouseX, mouseY, delta);
            }
        }

        int getMorePossibleHeight() {
            return selectionElement.getMorePossibleHeight();
        }

        boolean handleMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            return selectionElement.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        private void handleOutsideClick(int mouseX, int mouseY) {
            boolean leftPressed = Minecraft.getInstance().mouseHandler.isLeftPressed();
            if (leftPressed && !wasLeftPressed) {
                if (selectionElement.isMenuExpanded() && !selectionElement.isMouseOverExpandedArea(mouseX, mouseY)) {
                    dropdownEntry.setFocused(null);
                    selectionElement.setFocused(null);
                }
            }
            wasLeftPressed = leftPressed;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (handled) {
                dropdownEntry.setFocused(selectionElement.getFocused() == null ? null : selectionElement);
            }
            return handled;
        }

        @Override
        public NarrationPriority narrationPriority() {
            return selected ? NarrationPriority.FOCUSED : hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, Component.literal(getValue()));
        }
    }

    private static final class InlineDropdownEntry extends DropdownBoxEntry<String> {
        private final SizedSelectionElement<String> sizedSelectionElement;

        private InlineDropdownEntry(Component fieldName, Component resetButtonKey, Supplier<Optional<Component[]>> tooltipSupplier,
                                    boolean requiresRestart, Supplier<String> defaultValue, Consumer<String> saveConsumer,
                                    Iterable<String> selections, SelectionTopCellElement<String> topRenderer,
                                    SelectionCellCreator<String> cellCreator) {
            super(fieldName, resetButtonKey, tooltipSupplier, requiresRestart, defaultValue, saveConsumer, selections, topRenderer, cellCreator);
            ImmutableList<String> values = selections == null ? ImmutableList.of() : ImmutableList.copyOf(selections);
            sizedSelectionElement = new SizedSelectionElement<>(this, new Rectangle(0, 0, 150, 20),
                    new DropdownBoxEntry.DefaultDropdownMenuElement<>(values), topRenderer, cellCreator);
            this.selectionElement = sizedSelectionElement;
        }

        private void renderInline(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean isSelected, float delta) {
            sizedSelectionElement.setActive(isEditable());
            sizedSelectionElement.setBounds(x, y, width, height);
            sizedSelectionElement.render(graphics, mouseX, mouseY, delta);
        }

        private SizedSelectionElement<String> getSizedSelectionElement() {
            return sizedSelectionElement;
        }
    }

    private static final class SizedSelectionElement<R> extends DropdownBoxEntry.SelectionElement<R> {
        private SizedSelectionElement(DropdownBoxEntry<R> entry, Rectangle bounds, DropdownBoxEntry.DropdownMenuElement<R> menu,
                                      DropdownBoxEntry.SelectionTopCellElement<R> topRenderer,
                                      DropdownBoxEntry.SelectionCellCreator<R> cellCreator) {
            super(entry, bounds, menu, topRenderer, cellCreator);
        }

        private void setBounds(int x, int y, int width, int height) {
            bounds.x = x;
            bounds.y = y;
            bounds.width = width;
            bounds.height = height;
        }

        private void setActive(boolean active) {
            this.active = active;
        }

        private boolean isMenuExpanded() {
            return menu.isExpanded();
        }

        private boolean isMouseOverExpandedArea(double mouseX, double mouseY) {
            return isMouseOverSelection(mouseX, mouseY) || isMouseOverMenu(mouseX, mouseY);
        }

        private boolean isMouseOverSelection(double mouseX, double mouseY) {
            return mouseX >= bounds.x && mouseX <= bounds.x + bounds.width
                    && mouseY >= bounds.y && mouseY <= bounds.y + bounds.height;
        }

        private boolean isMouseOverMenu(double mouseX, double mouseY) {
            if (!menu.isExpanded()) {
                return false;
            }
            int menuX = bounds.x;
            int menuY = bounds.y + bounds.height;
            int menuWidth = menu.getCellCreator().getCellWidth();
            int menuHeight = menu.getHeight();
            return mouseX >= menuX && mouseX <= menuX + menuWidth
                    && mouseY >= menuY && mouseY <= menuY + menuHeight;
        }
    }

    private static final class ItemIdCellCreator extends DropdownBoxEntry.SelectionCellCreator<String> {
        private static final int CELL_HEIGHT = 20;
        private static final int CELL_WIDTH = 146;
        private static final int MAX_ITEMS = 7;

        @Override
        public DropdownBoxEntry.SelectionCellElement<String> create(String selection) {
            ItemStack stack = itemStackFor(selection);
            return new DropdownBoxEntry.DefaultSelectionCellElement<>(selection, Component::literal) {
                @Override
                public void render(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, float delta) {
                    rendering = true;
                    this.x = x;
                    this.y = y;
                    this.width = width;
                    this.height = height;
                    boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
                    if (hovered) {
                        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, -15132391);
                    }
                    graphics.drawString(Minecraft.getInstance().font, toTextFunction.apply(r).getVisualOrderText(), x + 6 + 18, y + 6, hovered ? 16777215 : 8947848);
                    graphics.renderItem(stack, x + 4, y + 2);
                }
            };
        }

        private static ItemStack itemStackFor(String selection) {
            ResourceLocation id = ResourceLocation.tryParse(selection);
            if (id == null) {
                return new ItemStack(Items.BARRIER);
            }
            return BuiltInRegistries.ITEM.getOptional(id)
                    .map(ItemStack::new)
                    .orElseGet(() -> new ItemStack(Items.BARRIER));
        }

        @Override
        public int getCellHeight() {
            return CELL_HEIGHT;
        }

        @Override
        public int getCellWidth() {
            return CELL_WIDTH;
        }

        @Override
        public int getDropBoxMaxHeight() {
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int preferred = Math.max(getCellHeight() * MAX_ITEMS, screenHeight / 2);
            return Math.max(getCellHeight(), Math.min(preferred, screenHeight - 60));
        }
    }
}
