package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
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
    private Matrix4f realcamera$modelViewMatrix;

    @Inject(method = "addMainPass", at = @At(value = "HEAD"))
    private void realcamera$cacheProjectionMatrix(FrameGraphBuilder frame, Frustum frustum, Matrix4f modelViewMatrix, GpuBufferSlice terrainFog, boolean renderOutline, LevelRenderState levelRenderState, DeltaTracker deltaTracker, ProfilerFiller profiler, CallbackInfo ci) {
        realcamera$modelViewMatrix = modelViewMatrix;
    }

    @Inject(method = "submitEntities", at = @At(value = "RETURN"))
    private void realcamera$renderCameraEntity(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera, DeltaTracker deltaTracker, List<Entity> list, CallbackInfo ci) {
        if (!RealCameraCore.isRendering()) return;
        Entity entity = camera.entity();
        TickRateManager tickManager = minecraft.level.tickRateManager();
        float deltaTick = deltaTracker.getGameTimeDeltaPartialTick(!tickManager.isEntityFrozen(entity));
        if (!ConfigFile.config().isClassic()) RealCameraCore.renderCameraEntity(minecraft, deltaTick, bufferSource, realcamera$modelViewMatrix);
        else {
            Vec3 cameraPos = camera.position();
            renderEntity(entity, cameraPos.x(), cameraPos.y(), cameraPos.z(), deltaTick, new PoseStack(), bufferSource);
        }
    }

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float deltaTick, PoseStack poseStack, MultiBufferSource bufferSource);
}
