package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class TexturedButton extends PressableWidget {
    public static final Identifier ICON_TEXTURE = new Identifier(RealCamera.MODID, "textures/gui/icon.png");
    protected final Identifier texture;
    protected final int textureWidth, textureHeight, u, v;
    private final Consumer<TexturedButton> onPress;

    public TexturedButton(int u, int v, Consumer<TexturedButton> onPress) {
        this(0, 0, 16, 16, u, v, onPress);
    }

    public TexturedButton(int x, int y, int width, int height, int u, int v, Consumer<TexturedButton> onPress) {
        this(x, y, width, height, u, v, ICON_TEXTURE, 256, 256, onPress);
    }

    public TexturedButton(int x, int y, int width, int height, int u, int v, Identifier texture, int textureWidth, int textureHeight, Consumer<TexturedButton> onPress) {
        super(x, y, width, height, Text.empty());
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
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF646464);
        context.drawTexture(texture, getX(), getY(), u, v, width, height, textureWidth, textureHeight);
        if (isSelected()) context.drawBorder(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
