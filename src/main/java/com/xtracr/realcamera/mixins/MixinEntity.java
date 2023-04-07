package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public abstract class MixinEntity {
    
    @ModifyVariable(
        method = "pick",
        at = @At("STORE"),
        ordinal = 0
    )
    private Vec3 getCameraPosition(Vec3 vec3) {
        return (CameraController.isActive() ? vec3.add(CameraController.getCameraOffset()) : vec3);
    }
    
    @ModifyVariable(
        method = "pick",
        at = @At("STORE"),
        ordinal = 1
    )
    private Vec3 getCameraDirection(Vec3 vec31) {
        return (CameraController.doCrosshairRotate() ? CameraController.getCameraDirection() : vec31);
    }
}
