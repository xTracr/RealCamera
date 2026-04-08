package com.xtracr.realcamera.renderer.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.renderer.state.gui.GuiCulledModelRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

public class GuiCulledModelRenderer extends PictureInPictureRenderer<GuiCulledModelRenderState> {
    public GuiCulledModelRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public @NotNull Class<GuiCulledModelRenderState> getRenderStateClass() {
        return null;
    }

    @Override
    protected void renderToTexture(@NotNull GuiCulledModelRenderState renderState, @NotNull PoseStack poseStack) {

    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "";
    }
}
