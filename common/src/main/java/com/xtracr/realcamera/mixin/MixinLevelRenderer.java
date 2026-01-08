package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
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
    private void realcamera$renderCameraEntity(PoseStack poseStack, float deltaTick, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        if (!RealCameraCore.isRendering()) return;
        MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();
        if (!ConfigFile.config().isClassic()) {
            Matrix4f modelView = new Matrix4f(poseStack.last().pose());
            RealCameraCore.renderCameraEntity(minecraft, deltaTick, bufferSource, modelView);
        }
        else {
            Vec3 cameraPos = camera.getPosition();
            renderEntity(camera.getEntity(), cameraPos.x(), cameraPos.y(), cameraPos.z(), deltaTick, poseStack, bufferSource);
        }
    }

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float deltaTick, PoseStack poseStack, MultiBufferSource bufferSource);
}
