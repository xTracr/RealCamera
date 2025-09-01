package com.xtracr.realcamera.mixin;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerRenderer.class)
public abstract class MixinPlayerRenderer {
    @Redirect(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;acos(D)D"))
    private double realcamera$redirectArccos(double n) {
        return Math.acos(Mth.clamp(n, -1.0, 1.0));
    }
}
