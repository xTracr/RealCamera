package com.xtracr.realcamera.gui.components;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public class SimpleIconButton extends Button {
    public static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "textures/gui/icon.png");
    protected final ResourceLocation texture;
    protected final int textureWidth, textureHeight, u, v;

    public SimpleIconButton(int u, int v, Button.OnPress onPress) {
        this(0, 0, 16, 16, u, v, onPress);
    }

    public SimpleIconButton(int x, int y, int width, int height, int u, int v, Button.OnPress onPress) {
        this(x, y, width, height, u, v, ICON_TEXTURE, 256, 256, onPress);
    }

    public SimpleIconButton(int x, int y, int width, int height, int u, int v, ResourceLocation texture, int textureWidth, int textureHeight, Button.OnPress onPress) {
        super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.u = u;
        this.v = v;
        this.texture = texture;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF646464);
        graphics.blit(texture, getX(), getY(), u, v, width, height, textureWidth, textureHeight);
        if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
    }
}
