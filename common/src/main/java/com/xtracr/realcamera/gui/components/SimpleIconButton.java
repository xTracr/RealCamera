package com.xtracr.realcamera.gui.components;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

@SuppressWarnings("removal")
public class SimpleIconButton extends AbstractButton {
    public static final ResourceLocation ICON_TEXTURE = new ResourceLocation(RealCamera.MOD_ID, "textures/gui/icon.png");
    protected final ResourceLocation texture;
    protected final int textureWidth, textureHeight, u, v;
    private final Consumer<SimpleIconButton> onPress;

    public SimpleIconButton(int u, int v, Consumer<SimpleIconButton> onPress) {
        this(0, 0, 16, 16, u, v, onPress);
    }

    public SimpleIconButton(int x, int y, int width, int height, int u, int v, Consumer<SimpleIconButton> onPress) {
        this(x, y, width, height, u, v, ICON_TEXTURE, 256, 256, onPress);
    }

    public SimpleIconButton(int x, int y, int width, int height, int u, int v, ResourceLocation texture, int textureWidth, int textureHeight, Consumer<SimpleIconButton> onPress) {
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
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF646464);
        graphics.blit(texture, getX(), getY(), u, v, width, height, textureWidth, textureHeight);
        if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        this.defaultButtonNarrationText(builder);
    }
}
