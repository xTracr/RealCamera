package com.xtracr.realcamera.renderer.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.renderer.state.BuiltModelRecord;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.renderer.state.gui.GuiFlattenedModelsRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

public final class GuiFlattenedModelsRenderer extends PictureInPictureRenderer<GuiFlattenedModelsRenderState> {
    public GuiFlattenedModelsRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public @NonNull Class<GuiFlattenedModelsRenderState> getRenderStateClass() {
        return GuiFlattenedModelsRenderState.class;
    }

    @Override
    protected void renderToTexture(@NonNull GuiFlattenedModelsRenderState renderState, @NonNull PoseStack poseStack) {
        Vector3f translation = renderState.translation();
        poseStack.translate(translation.x, translation.y, translation.z);

        Matrix4f positionMatrix = poseStack.last().pose();
        for (BuiltModelRecord record : renderState.records()) {
            VertexConsumer buffer = bufferSource.getBuffer(record.renderType());
            Vector3f position = new Vector3f();
            for (VertexData vertex : record.vertices()) {
                position.set(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                buffer.addVertex(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            }
        }
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale) {
        return height / 2.0f;
    }

    @Override
    protected @NonNull String getTextureLabel() {
        return "realcamera.flattened_models";
    }
}
