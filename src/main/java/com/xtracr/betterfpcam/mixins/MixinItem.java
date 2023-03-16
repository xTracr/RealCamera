package com.xtracr.betterfpcam.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xtracr.betterfpcam.camera.CameraController;
import com.xtracr.betterfpcam.config.ConfigController;

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
        if (ConfigController.configController.isEnabled() && (Minecraft.getInstance().options.getCameraType().isFirstPerson() || CameraController.INSTANCE.isActive()) 
        && Minecraft.getInstance().level != null) {
            return player.getEyePosition().add(CameraController.INSTANCE.cameraOffset);
        }
        return player.getEyePosition();
    }
}
