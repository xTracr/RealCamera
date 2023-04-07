package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    private static boolean toggle = false;

    @ModifyVariable(
        method = "pick(F)V",
        at = @At("STORE"),
        ordinal = 0
    )
    private Vec3 getCameraPosition(Vec3 vec3) {
        return (CameraController.isActive() ? vec3.add(CameraController.getCameraOffset()) : vec3);
    }

    @SuppressWarnings("resource")
    @Inject(
        method = "renderItemInHand",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"
        )
    )
    private void setThirdPerson(CallbackInfo cInfo) {
        if (CameraController.isActive()) {
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
            toggle = true;
        }
    }

    @SuppressWarnings("resource")
    @Inject(
        method = "renderItemInHand",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
        )
    )
    private void setFirstPerson(CallbackInfo cInfo) {
        if (toggle) {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            toggle = false;
        }
    }

}
