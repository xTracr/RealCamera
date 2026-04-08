package com.xtracr.realcamera.renderer.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.renderer.state.gui.GuiTextureRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

public class GuiTextureRenderer extends PictureInPictureRenderer<GuiTextureRenderState> {
    public GuiTextureRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public @NotNull Class<GuiTextureRenderState> getRenderStateClass() {
        return null;
    }

    @Override
    protected void renderToTexture(@NotNull GuiTextureRenderState renderState, @NotNull PoseStack poseStack) {

    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "";
    }
}
