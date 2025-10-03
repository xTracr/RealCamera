package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public record ColoredFloatLineRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x,
        float y,
        float z,
        float vectorX,
        float vectorY,
        float vectorZ,
        int argb,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColoredFloatLineRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            float x,
            float y,
            float z,
            float vectorX,
            float vectorY,
            float vectorZ,
            int argb,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x, y, z, vectorX, vectorY, vectorZ, argb, scissorArea,
                GUIHelper.getBounds((int) MathUtil.floor(x, x + vectorX), (int) MathUtil.floor(y, y + vectorY),
                        (int) MathUtil.ceil(x, x + vectorX), (int) MathUtil.ceil(x, x + vectorX), pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer, float depth) {
        Vector2f vec = pose.transformPosition(vectorX, vectorY, new Vector2f());
        if (pipeline.getVertexFormatMode().primitiveLength == 2) {
            vertexConsumer.addVertexWith2DPose(pose, x, y, z + depth).setColor(argb).setNormal(vec.x(), vec.y(), vectorZ);
            vertexConsumer.addVertexWith2DPose(pose, x + vectorX, y + vectorY, z + vectorZ + depth).setColor(argb).setNormal(vec.x(), vec.y(), vectorZ);
        } else {
            Vector2f start = pose.transformPosition(x, y, new Vector2f());
            Vector2f end = start.add(vec, new Vector2f());
            Vector2f normal = vec.normalize(new Vector2f()).mul(0.5f);
            vertexConsumer.addVertex(start.x() - normal.y(), start.y() + normal.x(), z + depth).setColor(argb);
            vertexConsumer.addVertex(end.x() - normal.y(), end.y() + normal.x(), z + vectorZ + depth).setColor(argb);
            vertexConsumer.addVertex(end.x() + normal.y(), end.y() - normal.x(), z + vectorZ + depth).setColor(argb);
            vertexConsumer.addVertex(start.x() + normal.y(), start.y() - normal.x(), z + depth).setColor(argb);
        }
    }
}
