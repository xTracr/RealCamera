package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.mixin.accessor.AvatarRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class LegacyBindingMode {
    public static void register() {
        RealCameraAPI.registerFunction(100, LegacyBindingMode::computeBindResult);
    }

    @SuppressWarnings("unchecked")
    private static BindResult computeBindResult(Minecraft client, float partialTicks) {
        if (!ConfigFile.config().binding.legacyMode) return BindResult.EMPTY;
        PoseStack poseStack = new PoseStack();
        AbstractClientPlayer player = client.player;
        // WorldRenderer.render
        // EntityRenderDispatcher.render
        AvatarRenderer<AbstractClientPlayer> playerRenderer = (AvatarRenderer<AbstractClientPlayer>) client.getEntityRenderDispatcher().getRenderer(player);
        AvatarRenderState renderState = playerRenderer.createRenderState(player, partialTicks);
        Vec3 renderOffset = playerRenderer.getRenderOffset(renderState);
        poseStack.translate(renderOffset.x(), renderOffset.y(), renderOffset.z());
        // LivingEntityRenderer.render
        if (renderState.hasPose(Pose.SLEEPING)) {
            Direction direction = renderState.bedOrientation;
            if (direction != null) {
                float f = renderState.eyeHeight - 0.1f;
                poseStack.translate((float) (-direction.getStepX()) * f, 0.0f, (float) (-direction.getStepZ()) * f);
            }
        }
        float scale = renderState.scale;
        poseStack.scale(scale, scale, scale);
        ((AvatarRendererAccessor) playerRenderer).invokeSetupRotations(renderState, poseStack, renderState.bodyRot, scale);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        ((AvatarRendererAccessor) playerRenderer).invokeScale(renderState, poseStack);
        poseStack.translate(0.0f, -1.501f, 0.0f);
        playerRenderer.getModel().setupAnim(renderState);
        // AnimalModel.render
        // ModelPart.render
        playerRenderer.getModel().head.translateAndRotate(poseStack);

        Vector4f offset = poseStack.last().pose().transform(new Vector4f(0, -0.125f, -0.2f, 1.0f));
        BindResult result = BindResult.getOrCreate("LEGACY_MODE");
        result.setPosition(new Vec3(offset.x(), offset.y(), offset.z()));
        poseStack.scale(1f, -1f, -1f);
        result.setForward(new Vec3(poseStack.last().normal().getColumn(2, new Vector3f())));
        result.setUpward(new Vec3(poseStack.last().normal().getColumn(1, new Vector3f())));
        return result;
    }
}
