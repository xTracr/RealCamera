package com.xtracr.realcamera.renderer.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.renderer.state.ModelSnapshot;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.renderer.state.gui.GuiCulledModelsRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

public final class GuiCulledModelsRenderer extends PictureInPictureRenderer<GuiCulledModelsRenderState> {
    public static final int MIN_Z = 0, MAX_Z = 200;

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

        float minEntityZ = MIN_Z, maxEntityZ = MAX_Z;
        for (ModelSnapshot snapshot : renderState.snapshots()) {
            for (VertexData vertex : snapshot) {
                if (vertex.z() < minEntityZ) minEntityZ = vertex.z();
                if (vertex.z() > maxEntityZ) maxEntityZ = vertex.z();
            }
        }
        Matrix4f positionMatrix = poseStack.last().pose().scale(1, 1, (float) MAX_Z / (maxEntityZ - minEntityZ)).translate(0, 0, -minEntityZ);
        Matrix3f normalMatrix = positionMatrix.normal(new Matrix3f());
        for (ModelSnapshot snapshot : renderState.snapshots()) {
            VertexConsumer buffer = bufferSource.getBuffer(snapshot.renderType());
            if (!snapshot.renderType().canConsolidateConsecutiveGeometry()) {
                for (VertexData vertex : snapshot) vertex.render(buffer, positionMatrix, normalMatrix);
                return;
            }
            for (Primitive primitive : snapshot.primitives()) {
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
        return "realcamera.culled_models";
    }
}
