package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void realcamera$cancelRender(float tickDelta, PoseStack matrices, MultiBufferSource.BufferSource vertexConsumers, LocalPlayer player, int light, CallbackInfo cInfo) {
        if (player instanceof LocalPlayer && RealCameraCore.isRendering()) {
            cInfo.cancel();
        }
    }
}
