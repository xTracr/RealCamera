package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.compat.DisableHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void realcamera$cancelRender(float frameInterp, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer player, int lightCoords, CallbackInfo ci) {
        if (DisableHelper.RENDER_HANDS.disabled(player)) ci.cancel();
    }
}
