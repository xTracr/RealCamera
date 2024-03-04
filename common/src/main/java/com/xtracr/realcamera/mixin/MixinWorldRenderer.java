package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow
    @Final private BufferBuilderStorage bufferBuilders;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;drawCurrentLayer()V", ordinal = 0))
    private void realcamera$renderClientPlayer(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                                               Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, CallbackInfo cInfo) {
        final ModConfig config = ConfigFile.modConfig;
        if (!RealCameraCore.isActive() || !config.isRendering() || (camera.getFocusedEntity() instanceof PlayerEntity player && player.isUsingSpyglass())) return;
        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        Vec3d cameraPos = camera.getPos();
        if (!config.isClassic()) RealCameraCore.renderPlayer(immediate);
        else renderEntity(camera.getFocusedEntity(), cameraPos.getX(), cameraPos.getY(), cameraPos.getZ(), tickDelta, matrices, immediate);
    }

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);
}
