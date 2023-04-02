package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xtracr.realcamera.camera.CameraController;
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
    private Vec3 getHitResult(Entity entity, float particalTicks) {
        if (CameraController.isActive()) {
            double distance = (double)Minecraft.getInstance().gameMode.getPickRange();
            Vec3 vec3 = entity.getEyePosition(particalTicks).add(CameraController.getCameraOffset());
            Vec3 vec31 = ( CameraController.doCrosshairRotate() ? CameraController.getViewVector() : entity.getViewVector(particalTicks));
            Vec3 vec32 = vec3.add(vec31.x * distance, vec31.y * distance, vec31.z * distance);
            Minecraft.getInstance().hitResult = entity.level.clip(new ClipContext(vec3, vec32, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
            
            return vec3;
        }
        return entity.getEyePosition(particalTicks);
    }

}
