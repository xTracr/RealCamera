package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.gui.ModelViewScreen;
import com.xtracr.realcamera.util.VertexData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEntityRenderer.class)
public abstract class MixinGuiEntityRenderer extends PictureInPictureRenderer<GuiEntityRenderState> {
    protected MixinGuiEntityRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Inject(method = "renderToTexture(Lnet/minecraft/client/gui/render/state/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;setRenderShadow(Z)V", ordinal = 0), cancellable = true)
    private void realcamera$atRenderToTextureHEAD(GuiEntityRenderState renderState, PoseStack poseStack, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ModelViewScreen screen) {
            poseStack.mulPose(screen.analyser.poseStack.last().pose().invert(new Matrix4f()));
            float scale = Minecraft.getInstance().getWindow().getGuiScale() * renderState.scale();
            Matrix4f positionMatrix = new Matrix4f().translate(0, 0, -3 * scale).mul(poseStack.last().pose());
            Matrix3f normalMatrix = poseStack.last().normal();
            screen.analyser.records().forEach(record -> VertexData.renderVertices(record.vertices(), bufferSource.getBuffer(record.renderType()), positionMatrix, normalMatrix));
            ci.cancel();
        }
    }
}
