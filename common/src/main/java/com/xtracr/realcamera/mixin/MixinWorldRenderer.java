package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
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
    @Final private MinecraftClient client;

    @Shadow
    @Final private BufferBuilderStorage bufferBuilders;

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;drawCurrentLayer()V",  ordinal = 0))
    private void realcamera$renderClientPlayer(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, CallbackInfo cInfo) {
        ModConfig config = ConfigFile.modConfig;
        if (camera.isThirdPerson() || !RealCameraCore.isActive() || !config.isRendering() || config.shouldDisableRendering(this.client)) return;
        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        Vec3d offset = camera.getPos().subtract(RealCameraCore.getModelOffset());
        RealCameraCore.setRenderingPlayer(true);
        if (config.binding.experimental && !config.isClassic()) RealCameraCore.renderPlayer(offset, matrices, immediate);
        else renderEntity(camera.getFocusedEntity(), offset.getX(), offset.getY(), offset.getZ(), tickDelta, matrices, immediate);
        RealCameraCore.setRenderingPlayer(false);
    }

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);
}
