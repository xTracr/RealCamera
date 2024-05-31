package com.xtracr.realcamera.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererEvents {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareCullFrustum(Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
    private void realcamera$onAfterCameraUpdate(float f, long l, CallbackInfo ci, @Local(ordinal = 1) Matrix4f matrix4f2) {
        if (RealCameraCore.isActive()) {
            matrix4f2.rotateLocalZ(RealCameraCore.getRoll(0) * (float) (Math.PI / 180.0));
        }
    }
}
