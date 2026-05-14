package com.xtracr.realcamera.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.IntConsumer;

public class CycleIconButton extends AbstractButton {
    protected final Identifier texture;
    protected final int textureWidth, textureHeight, u, v, vOffset, size;
    @Nullable
    private IntConsumer onValueChange;
    private int value;

    public CycleIconButton(int u, int v, int value, int size) {
        this(0, 0, 16, 16, u, v, value, size);
    }

    public CycleIconButton(int x, int y, int width, int height, int u, int v, int value, int size) {
        this(x, y, width, height, u, v, height, value, size, SimpleIconButton.ICON_TEXTURE, 256, 256);
    }

    public CycleIconButton(int x, int y, int width, int height, int u, int v, int vOffset, int value, int size, Identifier texture, int textureWidth, int textureHeight) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.u = u;
        this.v = v;
        this.vOffset = vOffset;
        this.size = size;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        setValue(value);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = (value % size + size) % size;
    }

    public CycleIconButton setOnValueChange(IntConsumer onValueChange) {
        this.onValueChange = onValueChange;
        return this;
    }

    @Override
    public void onPress(@NonNull InputWithModifiers input) {
        if (input.hasShiftDown()) setValue(value - 1);
        else setValue(value + 1);
        if (onValueChange != null) onValueChange.accept(value);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF646464);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), u, v + value * vOffset, width, height, textureWidth, textureHeight);
        if (isHoveredOrFocused()) graphics.outline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
