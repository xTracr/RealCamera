package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xtracr.realcamera.camera.CameraController;
import com.xtracr.realcamera.config.ConfigController;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

@Mixin(Item.class)
public abstract class MixinItem {
    
    @Redirect(
        method = "getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getEyePosition()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private static Vec3 getCameraPosition(Player player) {
        if (CameraController.INSTANCE.doCrosshairOffset()) {
            return player.getEyePosition().add(CameraController.INSTANCE.getCameraOffset());
        }
        return player.getEyePosition();
    }
    
    @SuppressWarnings("resource")
    @Redirect(
        method = "getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"
        )
    )
    private static float getCameraXRot(Player player) {
        if (CameraController.INSTANCE.doCrosshairOffset() && ConfigController.configController.isDirectionBound()) {
            return Minecraft.getInstance().getEntityRenderDispatcher().camera.getXRot();
        }
        return player.getXRot();
    }
    
    @SuppressWarnings("resource")
    @Redirect(
        method = "getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
        )
    )
    private static float getCameraYRot(Player player) {
        if (CameraController.INSTANCE.doCrosshairOffset() && ConfigController.configController.isDirectionBound()) {
            return Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
        }
        return player.getYRot();
    }
}
