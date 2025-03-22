package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final private Minecraft minecraft;
    @Unique
    private Matrix4f realCamera$projectionMatrix;

    @Inject(method = "addMainPass", at = @At(value = "HEAD"))
    private void realcamera$cacheProjectionMatrix(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, FogParameters fogParameters, boolean bl, boolean bl2, DeltaTracker deltaTracker, ProfilerFiller profilerFiller, CallbackInfo ci) {
        realCamera$projectionMatrix = matrix4f;
    }

    @Inject(method = "renderEntities", at = @At(value = "RETURN"))
    private void realcamera$renderLocalPlayer(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera, DeltaTracker deltaTracker, List<Entity> list, CallbackInfo ci)
    {
        if (!RealCameraCore.isRendering()) return;
        Vec3 cameraPos = camera.getPosition();
        if (!ConfigFile.config().isClassic()) RealCameraCore.renderCameraEntity(bufferSource, realCamera$projectionMatrix);
        else {
            TickRateManager tickManager = minecraft.level.tickRateManager();
            Entity entity = camera.getEntity();
            renderEntity(entity, cameraPos.x(), cameraPos.y(), cameraPos.z(), deltaTracker.getGameTimeDeltaPartialTick(!tickManager.isEntityFrozen(entity)), new PoseStack(), bufferSource);
        }
    }

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers);
}
