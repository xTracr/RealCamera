package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererEvents {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", shift = At.Shift.AFTER))
    private void realcamera$onAfterCameraUpdate(float tickDelta, long limitTime, PoseStack poseStack, CallbackInfo cInfo) {
        if (RealCameraCore.isActive()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(RealCameraCore.getRoll(0)));
        }
    }
}
