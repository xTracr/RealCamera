package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class TexturedButton extends AbstractButton {
    public static final ResourceLocation ICON_TEXTURE = new ResourceLocation(RealCamera.MODID, "textures/gui/icon.png");
    protected final ResourceLocation texture;
    protected final int textureWidth, textureHeight, u, v;
    private final Consumer<TexturedButton> onPress;

    public TexturedButton(int u, int v, Consumer<TexturedButton> onPress) {
        this(0, 0, 16, 16, u, v, onPress);
    }

    public TexturedButton(int x, int y, int width, int height, int u, int v, Consumer<TexturedButton> onPress) {
        this(x, y, width, height, u, v, ICON_TEXTURE, 256, 256, onPress);
    }

    public TexturedButton(int x, int y, int width, int height, int u, int v, ResourceLocation texture, int textureWidth, int textureHeight, Consumer<TexturedButton> onPress) {
        super(x, y, width, height, Component.empty());
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.onPress = onPress;
        this.u = u;
        this.v = v;
        this.texture = texture;
    }

    @Override
    public void onPress() {
        onPress.accept(this);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF646464);
        graphics.blit(texture, getX(), getY(), u, v, width, height, textureWidth, textureHeight);
        if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        this.defaultButtonNarrationText(builder);
    }
}
