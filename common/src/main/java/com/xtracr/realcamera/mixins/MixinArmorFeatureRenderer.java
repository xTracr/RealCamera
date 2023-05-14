package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.realcamera.api.VirtualRenderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;

@Mixin(ArmorFeatureRenderer.class)
public abstract class MixinArmorFeatureRenderer {

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void onRenderArmorHEAD(MatrixStack matrices, VertexConsumerProvider vertexConsumers, LivingEntity entity,
            EquipmentSlot armorSlot, int light, BipedEntityModel<? extends LivingEntity> model, CallbackInfo cInfo) {
        if (VirtualRenderer.shouldDisableRender("helmet") && armorSlot == EquipmentSlot.HEAD) {
            cInfo.cancel();
        }
    }
}
