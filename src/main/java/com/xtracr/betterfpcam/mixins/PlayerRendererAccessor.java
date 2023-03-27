package com.xtracr.betterfpcam.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

@Mixin(PlayerRenderer.class)
public interface PlayerRendererAccessor {

    @Invoker
    void invokeSetupRotations(AbstractClientPlayer player, PoseStack poseStack, float bob, float yRot, float particalTicks);
}
