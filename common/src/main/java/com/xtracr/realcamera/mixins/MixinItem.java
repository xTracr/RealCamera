package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

@Mixin(Item.class)
public abstract class MixinItem {
    
    @ModifyVariable(
        method = "raycast",
        at = @At("STORE"),
        ordinal = 0
    )
    private static Vec3d getCameraPosition(Vec3d vec3d) {
        return (CameraController.isActive() ? vec3d.add(CameraController.getCameraOffset()) : vec3d);
    }
    
    @ModifyVariable(
        method = "raycast",
        at = @At("STORE"),
        ordinal = 0
    )
    private static float getCameraPitch(float f) {
        return (CameraController.doCrosshairRotate() ? MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getPitch() : f);
    }
    
    @ModifyVariable(
        method = "raycast",
        at = @At("STORE"),
        ordinal = 1
    )
    private static float getCameraYaw(float g) {
        return (CameraController.doCrosshairRotate() ? MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getYaw() : g);
    }
}
