package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.api.BindingContext;
import com.xtracr.realcamera.api.BindingResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.mixin.accessor.AvatarRendererAccessor;
import com.xtracr.realcamera.util.ReferenceFrame;
import org.jspecify.annotations.Nullable;
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
        RealCameraAPI.register("legacy", 100, LegacyBindingMode::computeResult);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable BindingResult computeResult(BindingContext context) {
        Minecraft client = context.client();
        float partialTicks = context.partialTicks();
        if (!ConfigFile.config().binding.legacyMode) return null;
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
        poseStack.scale(1f, -1f, -1f);
        ReferenceFrame frame = ReferenceFrame.tryFromForwardUp(
                new Vec3(offset.x(), offset.y(), offset.z()),
                new Vec3(poseStack.last().normal().getColumn(2, new Vector3f())),
                new Vec3(poseStack.last().normal().getColumn(1, new Vector3f())));
        if (frame == null) return null;
        return context.result("head", frame);
    }
}
