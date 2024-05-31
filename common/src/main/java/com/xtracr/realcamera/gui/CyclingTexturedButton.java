package com.xtracr.realcamera.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CyclingTexturedButton extends AbstractButton {
    protected final ResourceLocation texture;
    protected final int textureWidth, textureHeight, u, v, vOffset, size;
    private int value;

    public CyclingTexturedButton(int u, int v, int value, int size) {
        this(0, 0, 16, 16, u, v, value, size);
    }

    public CyclingTexturedButton(int x, int y, int width, int height, int u, int v, int value, int size) {
        this(x, y, width, height, u, v, height, value, size, TexturedButton.ICON_TEXTURE, 256, 256);
    }

    public CyclingTexturedButton(int x, int y, int width, int height, int u, int v, int vOffset, int value, int size, ResourceLocation texture, int textureWidth, int textureHeight) {
        super(x, y, width, height, Component.empty());
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

    @Override
    public void onPress() {
        if (Screen.hasShiftDown()) setValue(value - 1);
        else setValue(value + 1);
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF646464);
        context.blit(texture, getX(), getY(), u, v + value * vOffset, width, height, textureWidth, textureHeight);
        if (isHoveredOrFocused()) context.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
}
