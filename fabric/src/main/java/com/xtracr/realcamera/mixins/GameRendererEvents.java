package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererEvents {
    @Shadow
    @Final MinecraftClient client;
    @Shadow
    @Final private Camera camera;

    @Inject(method = "renderWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
            shift = At.Shift.AFTER))
    private void realCamera$onAfterCameraUpdate(float tickDelta, long limitTime, MatrixStack matrixStack, CallbackInfo cInfo) {
        if (RealCameraCore.isActive()) {
            RealCameraCore.updateCamera(camera, client, tickDelta);
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(RealCameraCore.getRoll()));
        }
    }
}
