package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.mixin.accessor.PlayerRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LegacyBindingMode {
    public static void register() {
        RealCameraAPI.registerFunction(LegacyBindingMode::computeBindResult);
    }

    private static BindResult computeBindResult(Minecraft client, float deltaTick) {
        if (!ConfigFile.config().legacyBindingMode()) return BindResult.EMPTY;
        PoseStack poseStack = new PoseStack();
        AbstractClientPlayer player = client.player;
        // WorldRenderer.render
        // EntityRenderDispatcher.render
        PlayerRenderer playerRenderer = (PlayerRenderer) client.getEntityRenderDispatcher().getRenderer(player);
        Vec3 renderOffset = playerRenderer.getRenderOffset(player, deltaTick);
        poseStack.translate(renderOffset.x(), renderOffset.y(), renderOffset.z());
        // PlayerEntityRenderer.render
        ((PlayerRendererAccessor) playerRenderer).invokeSetModelProperties(player);
        // LivingEntityRenderer.render
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
        playerModel.attackTime = player.getAttackAnim(deltaTick);
        playerModel.riding = player.isPassenger();
        playerModel.young = player.isBaby();
        float h = Mth.rotLerp(deltaTick, player.yBodyRotO, player.yBodyRot);
        float j = Mth.rotLerp(deltaTick, player.yHeadRotO, player.yHeadRot);
        float k = j - h;
        if (player.isPassenger() && player.getVehicle() instanceof LivingEntity livingEntity) {
            h = Mth.rotLerp(deltaTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
            k = j - h;
            float l = Mth.wrapDegrees(k);
            if (l < -85.0F) {
                l = -85.0F;
            }
            if (l >= 85.0F) {
                l = 85.0F;
            }
            h = j - l;
            if (l * l > 2500.0F) {
                h += l * 0.2F;
            }
            k = j - h;
        }
        float m = Mth.lerp(deltaTick, player.xRotO, player.getXRot());
        if (LivingEntityRenderer.isEntityUpsideDown(player)) {
            m *= -1.0F;
            k *= -1.0F;
        }
        k = Mth.wrapDegrees(k);
        if (player.hasPose(Pose.SLEEPING)) {
            Direction direction = player.getBedOrientation();
            if (direction != null) {
                float n = player.getEyeHeight(Pose.STANDING) - 0.1F;
                poseStack.translate((float)(-direction.getStepX()) * n, 0.0F, (float)(-direction.getStepZ()) * n);
            }
        }
        float lx = player.getScale();
        poseStack.scale(lx, lx, lx);
        float n = player.tickCount + deltaTick;
        ((PlayerRendererAccessor) playerRenderer).invokeSetupRotations(player, poseStack, n, h, deltaTick, lx);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        ((PlayerRendererAccessor) playerRenderer).invokeScale(player, poseStack, deltaTick);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        float o = 0.0F;
        float p = 0.0F;
        if (!player.isPassenger() && player.isAlive()) {
            o = player.walkAnimation.speed(deltaTick);
            p = player.walkAnimation.position(deltaTick);
            if (player.isBaby()) {
                p *= 3.0F;
            }

            if (o > 1.0F) {
                o = 1.0F;
            }
        }
        playerModel.prepareMobModel(player, p, o, deltaTick);
        playerModel.setupAnim(player, p, o, n, k, m);
        // AnimalModel.render
        // ModelPart.render
        playerModel.head.translateAndRotate(poseStack);

        Vector4f offset = poseStack.last().pose().transform(new Vector4f(0, -0.125f, -0.2f, 1.0f));
        BindResult result = new BindResult("LEGACY_MODE");
        result.setPosition(new Vec3(offset.x(), offset.y(), offset.z()));
        poseStack.scale(1f, -1f, -1f);
        result.setForward(new Vec3(poseStack.last().normal().getColumn(2, new Vector3f())));
        result.setUpward(new Vec3(poseStack.last().normal().getColumn(1, new Vector3f())));
        return result;
    }
}
