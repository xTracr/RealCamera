package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

@Mixin(Item.class)
public abstract class MixinItem {
    
    @ModifyVariable(
        method = "getPlayerPOVHitResult",
        at = @At("STORE"),
        ordinal = 0
    )
    private static Vec3 getCameraPosition(Vec3 vec3) {
        return (CameraController.isActive() ? vec3.add(CameraController.getCameraOffset()) : vec3);
    }
    
    @ModifyVariable(
        method = "getPlayerPOVHitResult",
        at = @At("STORE"),
        ordinal = 0
    )
    private static float getCameraXRot(float f) {
        return (CameraController.doCrosshairRotate() ? Minecraft.getInstance().getEntityRenderDispatcher().camera.getXRot() : f);
    }
    
    @ModifyVariable(
        method = "getPlayerPOVHitResult",
        at = @At("STORE"),
        ordinal = 1
    )
    private static float getCameraYRot(float f1) {
        return (CameraController.doCrosshairRotate() ? Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot() : f1);
    }
}
