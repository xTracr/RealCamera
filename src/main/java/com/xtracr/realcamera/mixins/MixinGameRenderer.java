package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    
    // Events
    @Shadow
    private Camera camera;

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
            shift = At.Shift.AFTER
        )
    )
    private void onCameraUpdate(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo cInfo) {
        if (CameraController.isActive() && MinecraftClient.getInstance().player != null) {
            CameraController.cameraRoll = 0.0F;
            CameraController.setCameraOffset(camera, MinecraftClient.getInstance(), tickDelta);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(CameraController.cameraRoll));
        }
    }
    
    // Mixins
    private static boolean toggle = false;

    @ModifyVariable(
        method = "updateTargetedEntity",
        at = @At("STORE"),
        ordinal = 0
    )
    private Vec3d getCameraPosition(Vec3d vec3d) {
        return (CameraController.isActive() ? vec3d.add(CameraController.getCameraOffset()) : vec3d);
    }

    @SuppressWarnings("resource")
    @Inject(
        method = "renderHand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;push()V"
        )
    )
    private void setThirdPerson(CallbackInfo cInfo) {
        if (CameraController.isActive()) {
            MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_BACK);
            toggle = true;
        }
    }

    @SuppressWarnings("resource")
    @Inject(
        method = "renderHand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"
        )
    )
    private void setFirstPerson(CallbackInfo cInfo) {
        if (toggle) {
            MinecraftClient.getInstance().options.setPerspective(Perspective.FIRST_PERSON);
            toggle = false;
        }
    }

}
