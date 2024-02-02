package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {
    @Shadow
    @Final private MinecraftClient client;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void realcamera$cancelRendering(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
            ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo cInfo) {
        ModConfig config = ConfigFile.modConfig;
        if (player instanceof ClientPlayerEntity && RealCameraCore.isActive() && config.isRendering() &&
                !config.shouldDisableRendering(client) && !config.allowRenderingHand(client)) {
            cInfo.cancel();
        }
    }
}
