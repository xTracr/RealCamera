package com.xtracr.realcamera.mixins;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerEntityRenderer.class)
public interface PlayerEntityRendererAccessor {
    @Invoker
    void invokeSetModelPose(AbstractClientPlayerEntity player);

    @Invoker
    void invokeScale(AbstractClientPlayerEntity abstractClientPlayerEntity, MatrixStack matrixStack, float tickDelta);

    @Invoker
    void invokeSetupTransforms(AbstractClientPlayerEntity abstractClientPlayerEntity, MatrixStack matrixStack,
            float f, float bodyYaw, float tickDelta);
}
