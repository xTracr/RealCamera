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
    private void realcamera$renderCameraEntity(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (!RealCameraCore.isRendering()) return;
        MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();
        Entity entity = camera.getEntity();
        TickRateManager tickManager = minecraft.level.tickRateManager();
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(!tickManager.isEntityFrozen(entity));
        if (!ConfigFile.config().isClassic) RealCameraCore.renderCameraEntity(minecraft, partialTicks, bufferSource, matrix4f);
        else {
            Vec3 cameraPos = camera.getPosition();
            renderEntity(entity, cameraPos.x(), cameraPos.y(), cameraPos.z(), partialTicks, new PoseStack(), bufferSource);
        }
    }

    @Shadow
    protected abstract void renderEntity(Entity entity, double d, double e, double f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource);
}
