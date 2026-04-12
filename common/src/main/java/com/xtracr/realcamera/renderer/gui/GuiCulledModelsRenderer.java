package com.xtracr.realcamera.renderer.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.renderer.state.BuiltModelRecord;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.renderer.state.gui.GuiCulledModelsRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

public class GuiCulledModelsRenderer extends PictureInPictureRenderer<GuiCulledModelsRenderState> {
    public GuiCulledModelsRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public @NonNull Class<GuiCulledModelsRenderState> getRenderStateClass() {
        return GuiCulledModelsRenderState.class;
    }

    @Override
    protected void renderToTexture(@NonNull GuiCulledModelsRenderState renderState, @NonNull PoseStack poseStack) {
        poseStack.mulPose(renderState.transform());

        float minEntityZ = 0f, maxEntityZ = 200f;
        for (BuiltModelRecord record : renderState.records()) {
            for (VertexData vertex : record.vertices()) {
                if (vertex.z() < minEntityZ) minEntityZ = vertex.z();
                if (vertex.z() > maxEntityZ) maxEntityZ = vertex.z();
            }
        }
        Matrix4f positionMatrix = poseStack.last().pose().scale(1, 1, 200 / (maxEntityZ - minEntityZ)).translate(0, 0, -minEntityZ);
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
        for (BuiltModelRecord record : renderState.records()) {
            VertexConsumer buffer = bufferSource.getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                for (VertexData vertex : record.vertices()) vertex.render(buffer, positionMatrix, normalMatrix);
                return;
            }
            for (VertexData[] primitive : record.primitives()) {
                for (VertexData vertex : primitive) vertex.render(buffer, positionMatrix, normalMatrix);
            }
        }
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale) {
        return height / 2.0f;
    }

    @Override
    protected @NonNull String getTextureLabel() {
        return "realcamera.culled_model";
    }
}
