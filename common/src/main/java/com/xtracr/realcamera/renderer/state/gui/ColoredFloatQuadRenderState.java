package com.xtracr.realcamera.renderer.state.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.gui.GUIHelper;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record ColoredFloatQuadRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0,
        float y0,
        float z0,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        int argb,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColoredFloatQuadRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            int argb,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, argb, scissorArea,
                GUIHelper.getBounds((int) MathUtil.floor(x0, x1, x2, x3), (int) MathUtil.floor(y0, y1, y2, y3),
                        (int) MathUtil.ceil(x0, x1, x2, x3), (int) MathUtil.ceil(y0, y1, y2, y3), pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x0, y0).setColor(argb);
        vertexConsumer.addVertexWith2DPose(pose, x1, y1).setColor(argb);
        vertexConsumer.addVertexWith2DPose(pose, x2, y2).setColor(argb);
        vertexConsumer.addVertexWith2DPose(pose, x3, y3).setColor(argb);
    }
}
