package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.gui.ModelViewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
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
    private void realcamera$beforeSetRenderShadow(GuiEntityRenderState renderState, PoseStack poseStack, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ModelViewScreen screen) {
            if (renderState.overrideCameraAngle() != null) screen.analyser.drawModel(bufferSource, poseStack);
            else screen.analyser.drawTexture(bufferSource, poseStack);
            ci.cancel();
        }
    }
}
