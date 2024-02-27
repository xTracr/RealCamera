package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public class CyclingTexturedButton extends PressableWidget {
    private static final Identifier ICON_TEXTURE = new Identifier(RealCamera.MODID, "textures/gui/icon.png");
    protected final Identifier texture;
    protected final int textureWidth, textureHeight, vOffset, maximum;
    protected int u, v;
    private int value = 0;

    public CyclingTexturedButton(int u, int v, int maximum) {
        this(0, 0, 16, 16, u, v, maximum);
    }

    public CyclingTexturedButton(int x, int y, int width, int height, int u, int v, int maximum) {
        this(x, y, width, height, u, v, height, maximum,  ICON_TEXTURE, 256, 256, Text.empty());
    }

    public CyclingTexturedButton(int x, int y, int width, int height, int u, int v, int vOffset, int maximum, Identifier texture, int textureWidth, int textureHeight, Text message) {
        super(x, y, width, height, message);
        this.u = u;
        this.v = v;
        this.vOffset = vOffset;
        this.maximum = maximum;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = (value % maximum + maximum) % maximum;
    }

    private void cycle(int amount) {
        value = (value + amount % maximum + maximum) % maximum;
    }

    @Override
    public void onPress() {
        if (Screen.hasShiftDown()) cycle(-1);
        else cycle(1);
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF646464);
        context.drawTexture(texture, getX(), getY(), u, v + value * vOffset, width, height, textureWidth, textureHeight);
        if (hovered) context.drawBorder(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
