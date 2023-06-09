package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.realcamera.api.VirtualRenderer;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;

@Mixin(PlayerHeldItemFeatureRenderer.class)
public abstract class MixinPlayerHeldItemFeatureRenderer {

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void onRenderItemHEAD(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode,
            Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo cInfo) {
        if (!(entity instanceof ClientPlayerEntity)) return;
        if (VirtualRenderer.shouldDisableRender("heldItem")) {
            cInfo.cancel();
        }
    }
}
