package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void realcamera$atRenderLevelHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        if (ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            CrosshairUtil.update(minecraft, cameraState.pos, modelViewMatrix, cameraState.projectionMatrix);
        }
    }

    @Inject(method = "submitEntities", at = @At(value = "RETURN"))
    private void realcamera$submitCameraEntity(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
        if (!RealCameraCore.isRendering()) return;
        Entity entity = minecraft.getCameraEntity();
        TickRateManager tickManager = minecraft.level.tickRateManager();
        float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(!tickManager.isEntityFrozen(entity));
        CameraRenderState cameraState = levelRenderState.cameraRenderState;
        if (!ConfigFile.config().isClassic()) RealCameraCore.renderCameraEntity(minecraft, partialTicks, output, cameraState.viewRotationMatrix);
        else {
            Vec3 cameraPos = cameraState.pos;
            EntityRenderState state = entityRenderDispatcher.extractEntity(entity, partialTicks);
            entityRenderDispatcher.submit(state, cameraState, state.x - cameraPos.x, state.y - cameraPos.y, state.z - cameraPos.z, poseStack, output);
        }
    }
}
