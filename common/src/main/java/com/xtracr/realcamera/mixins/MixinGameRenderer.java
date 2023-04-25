package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.realcamera.camera.CameraController;
import com.xtracr.realcamera.compat.DoABarrelRollCompat;
import com.xtracr.realcamera.config.ConfigFile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    
    private static boolean toggle = false;

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
            shift = At.Shift.BY,
            by = -2
        )
    )
    private void resetMatrixStack(float tickDelta, long limitTime, MatrixStack matrixStack, CallbackInfo cInfo) {
        if (DoABarrelRollCompat.loaded && ConfigFile.modConfig.compatDoABarrelRoll()) {
            matrixStack.loadIdentity();
        }
    }

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
        if (CameraController.isActive() && ConfigFile.modConfig.isRendering()) {
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
