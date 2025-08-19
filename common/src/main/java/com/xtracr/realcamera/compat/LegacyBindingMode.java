package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.api.PoseHandler;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.mixin.accessor.PlayerRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LegacyBindingMode {
    public static void registerConsumer() {
        RealCameraAPI.registerPoseHandlerConsumer("LEGACY_MODE", LegacyBindingMode::setupPose);
    }

    private static void setupPose(Object obj) {
        if (!ConfigFile.config().legacyBindingMode() || !(obj instanceof PoseHandler poseHandler)) return;
        Minecraft client = poseHandler.getClient();
        float deltaTick = poseHandler.getDeltaTick();
        PoseStack poseStack = new PoseStack();
        AbstractClientPlayer player = client.player;
        // WorldRenderer.render
        if (player.tickCount == 0) {
            player.xOld = player.getX();
            player.yOld = player.getY();
            player.zOld = player.getZ();
        }
        // EntityRenderDispatcher.render
        PlayerRenderer playerRenderer = (PlayerRenderer) client.getEntityRenderDispatcher().getRenderer(player);
        PlayerRenderState renderState = playerRenderer.createRenderState(player, deltaTick);
        Vec3 renderOffset = playerRenderer.getRenderOffset(renderState);
        poseStack.translate(renderOffset.x(), renderOffset.y(), renderOffset.z());
        // LivingEntityRenderer.render
        if (renderState.hasPose(Pose.SLEEPING)) {
            Direction direction = renderState.bedOrientation;
            if (direction != null) {
                float f = renderState.eyeHeight - 0.1f;
                poseStack.translate((float)(-direction.getStepX()) * f, 0.0f, (float)(-direction.getStepZ()) * f);
            }
        }
        float g = renderState.scale;
        poseStack.scale(g, g, g);
        ((PlayerRendererAccessor) playerRenderer).invokeSetupRotations(renderState, poseStack, renderState.bodyRot, g);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        ((PlayerRendererAccessor) playerRenderer).invokeScale(renderState, poseStack);
        poseStack.translate(0.0f, -1.501f, 0.0f);
        playerRenderer.getModel().setupAnim(renderState);
        // AnimalModel.render
        // ModelPart.render
        playerRenderer.getModel().head.translateAndRotate(poseStack);

        Vector4f offset = poseStack.last().pose().transform(new Vector4f(0, -0.125f, -0.2f, 1.0f));
        poseHandler.setPosition(new Vec3(offset.x(), offset.y(), offset.z()));
        poseStack.scale(1f, -1f, -1f);
        poseHandler.setForward(new Vec3(poseStack.last().normal().getColumn(2, new Vector3f())));
        poseHandler.setUpward(new Vec3(poseStack.last().normal().getColumn(1, new Vector3f())));
    }
}
