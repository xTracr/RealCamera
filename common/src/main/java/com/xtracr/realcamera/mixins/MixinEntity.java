package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

@Mixin(Entity.class)
public abstract class MixinEntity {
    
    @ModifyVariable(method = "raycast", at = @At("STORE"), ordinal = 0)
    private Vec3d getCameraPosition(Vec3d vec3d) {
        return (CameraController.isActive() ? vec3d.add(CameraController.getCameraOffset()) : vec3d);
    }
    
    @ModifyVariable(method = "raycast", at = @At("STORE"), ordinal = 1)
    private Vec3d getCameraDirection(Vec3d vec3d2) {
        return (CameraController.doCrosshairRotate() ? CameraController.getCameraDirection() : vec3d2);
    }
    
}
