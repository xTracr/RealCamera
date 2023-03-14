package com.xtracr.betterfpcam.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.betterfpcam.camera.CameraController;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

@Mixin(Gui.class)
public abstract class MixinGui {

    /*
    @Redirect(
        method = "renderCrosshair(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"
        )
    )
    public boolean doRenderCrosshair(CameraType cameraType) {
        return CameraController.INSTANCE.doRenderCrosshair(cameraType);
    }*/

    @SuppressWarnings("resource")
    @Inject(
        method = "renderCrosshair(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At("HEAD")
    )
    public void onRenderCrosshairHEAD(CallbackInfo cInfo) {
        if (CameraController.INSTANCE.isActive()) {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        }
    }
    
    @SuppressWarnings("resource")
    @Inject(
        method = "renderCrosshair(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At("TAIL")
    )
    public void onRenderCrosshairTAIL(CallbackInfo cInfo) {
        if (CameraController.INSTANCE.isActive()) {
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }
    
}
