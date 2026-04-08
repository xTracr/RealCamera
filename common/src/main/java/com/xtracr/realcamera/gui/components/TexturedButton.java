package com.xtracr.realcamera.gui.components;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TexturedButton extends AbstractButton {
    public static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath(RealCamera.MODID, "textures/gui/icon.png");
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
        super(x, y, width, height, CommonComponents.EMPTY);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.onPress = onPress;
        this.u = u;
        this.v = v;
        this.texture = texture;
    }

    @Override
    public void onPress(@NotNull InputWithModifiers input) {
        onPress.accept(this);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF646464);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), u, v, width, height, textureWidth, textureHeight);
        if (isHoveredOrFocused()) graphics.outline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput builder) {
        this.defaultButtonNarrationText(builder);
    }
}
