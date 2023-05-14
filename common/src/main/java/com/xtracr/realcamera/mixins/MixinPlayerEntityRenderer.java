package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.realcamera.api.VirtualRenderer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public MixinPlayerEntityRenderer(Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model,
            float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "setModelPose", at = @At("RETURN"))
    private void onSetModelPoseRETURN(CallbackInfo cInfo) {
        if (VirtualRenderer.shouldDisableRender("head")) this.getModel().head.visible = false;
        if (VirtualRenderer.shouldDisableRender("hat")) this.getModel().hat.visible = false;
    }
}
