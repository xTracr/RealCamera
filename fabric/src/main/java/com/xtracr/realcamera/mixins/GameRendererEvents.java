package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.realcamera.RealCameraCore;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;

@Mixin(GameRenderer.class)
public abstract class GameRendererEvents {

    @Shadow
    private Camera camera;
    @Shadow
    @Final MinecraftClient client;

    @Inject(method = "renderWorld", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
        shift = At.Shift.AFTER))
    private void onAfterCameraUpdate(float tickDelta, long limitTime, MatrixStack matrixStack, CallbackInfo cInfo) {
        if (RealCameraCore.isActive()) {
            RealCameraCore.updateCamera(this.camera, this.client, tickDelta);
            matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(RealCameraCore.getRoll()));
        }
    }
}
