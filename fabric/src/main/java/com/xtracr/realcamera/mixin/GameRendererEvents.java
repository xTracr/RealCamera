package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererEvents {
    @Inject(method = "renderWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
            shift = At.Shift.AFTER))
    private void realCamera$onAfterCameraUpdate(float tickDelta, long limitTime, MatrixStack matrixStack, CallbackInfo cInfo) {
        if (RealCameraCore.isActive()) {
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(RealCameraCore.getRoll()));
        }
    }
}
