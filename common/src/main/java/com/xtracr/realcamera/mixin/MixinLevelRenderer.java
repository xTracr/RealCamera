package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final private Minecraft minecraft;
    @Shadow
    @Final private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V", ordinal = 0))
    private void realcamera$renderLocalPlayer(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (!RealCameraCore.isRendering()) return;
        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
        Vec3 cameraPos = camera.getPosition();
        if (!ConfigFile.config().isClassic()) RealCameraCore.renderCameraEntity(bufferSource, matrix4f);
        else {
            TickRateManager tickManager = minecraft.level.tickRateManager();
            Entity entity = camera.getEntity();
            renderEntity(entity, cameraPos.x(), cameraPos.y(), cameraPos.z(), deltaTracker.getGameTimeDeltaPartialTick(!tickManager.isEntityFrozen(entity)), new PoseStack(), bufferSource);
        }
    }

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers);
}
