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
        if (VirtualRenderer.shouldDisableRender("body")) this.getModel().body.visible = false;
        if (VirtualRenderer.shouldDisableRender("rightArm")) this.getModel().rightArm.visible = false;
        if (VirtualRenderer.shouldDisableRender("leftArm")) this.getModel().leftArm.visible = false;
        if (VirtualRenderer.shouldDisableRender("rightLeg")) this.getModel().rightLeg.visible = false;
        if (VirtualRenderer.shouldDisableRender("leftLeg")) this.getModel().leftLeg.visible = false;
        if (VirtualRenderer.shouldDisableRender("leftSleeve")) this.getModel().leftSleeve.visible = false;
        if (VirtualRenderer.shouldDisableRender("rightSleeve")) this.getModel().rightSleeve.visible = false;
        if (VirtualRenderer.shouldDisableRender("leftPants")) this.getModel().leftPants.visible = false;
        if (VirtualRenderer.shouldDisableRender("rightPants")) this.getModel().rightPants.visible = false;
        if (VirtualRenderer.shouldDisableRender("jacket")) this.getModel().jacket.visible = false;
    }
}
