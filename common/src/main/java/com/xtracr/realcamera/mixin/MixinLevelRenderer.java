package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
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
    private Matrix4f modelViewMatrix;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void realcamera$atRenderLevelHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, Camera camera, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Matrix4f projectionMatrixForCulling, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        this.modelViewMatrix = modelViewMatrix;
        if (ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            CrosshairUtil.update(minecraft, camera, modelViewMatrix, projectionMatrix);
        }
    }

    @Inject(method = "submitEntities", at = @At(value = "RETURN"))
    private void realcamera$renderCameraEntity(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera, DeltaTracker deltaTracker, List<Entity> list, CallbackInfo ci) {
        if (!RealCameraCore.isRendering()) return;
        Entity entity = camera.entity();
        TickRateManager tickManager = minecraft.level.tickRateManager();
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(!tickManager.isEntityFrozen(entity));
        if (!ConfigFile.config().isClassic()) RealCameraCore.renderCameraEntity(minecraft, partialTicks, bufferSource, modelViewMatrix);
        else {
            Vec3 cameraPos = camera.position();
            renderEntity(entity, cameraPos.x(), cameraPos.y(), cameraPos.z(), partialTicks, new PoseStack(), bufferSource);
        }
    }

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource);
}
