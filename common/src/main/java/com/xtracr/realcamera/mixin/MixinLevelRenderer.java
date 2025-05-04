package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final private Minecraft minecraft;
    @Unique
    private Matrix4f realcamera$cameraPose = new Matrix4f();

    @Inject(method = "renderLevel", at = @At( "HEAD"))
    private void realcamera$renderLocalPlayer(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        realcamera$cameraPose = matrix4f;
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void realcamera$atRenderEntityHEAD(Entity entity, double d, double e, double f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        if (entity != minecraft.getCameraEntity() || !RealCameraCore.isRendering() || ConfigFile.config().isClassic()) return;
        RealCameraCore.renderCameraEntity(minecraft, g, multiBufferSource, realcamera$cameraPose);
        ci.cancel();
    }
}
