package com.xtracr.betterfpcam.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xtracr.betterfpcam.camera.CameraController;
import com.xtracr.betterfpcam.config.ConfigController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @SuppressWarnings({"resource","null"})
    @Redirect(
        method = "pick(F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 getCameraPosition(Entity entity, float particalTicks) {
        if (ConfigController.configController.isEnabled() && (Minecraft.getInstance().options.getCameraType().isFirstPerson() || CameraController.INSTANCE.isActive()) 
        && Minecraft.getInstance().level != null) {
            double distance = (double)Minecraft.getInstance().gameMode.getPickRange();
            //Vec3 vec3 = CameraController.INSTANCE.localCamera.getPosition();
            Vec3 vec3 = entity.getEyePosition(particalTicks).add(CameraController.INSTANCE.cameraOffset);
            Vec3 vec31 = entity.getViewVector(particalTicks);
            Vec3 vec32 = vec3.add(vec31.x * distance, vec31.y * distance, vec31.z * distance);
            Minecraft.getInstance().hitResult = entity.level.clip(new ClipContext(vec3, vec32, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
            
            return vec3;
        }
        return entity.getEyePosition(particalTicks);
    }
    
}
