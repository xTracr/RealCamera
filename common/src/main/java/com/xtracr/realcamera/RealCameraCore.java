package com.xtracr.realcamera;

import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.compat.PhysicsModCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.mixins.PlayerEntityRendererAccessor;
import com.xtracr.realcamera.utils.MathUtils;
import com.xtracr.realcamera.utils.Matrix3fc;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vector4f;

public class RealCameraCore {
    private static final ModConfig config = ConfigFile.modConfig;

    private static String status = "Successful";
    private static float pitch = 0.0F;
    private static float yaw = 0.0F;
    private static float roll = 0.0F;
    private static Vec3d pos = Vec3d.ZERO;
    private static Vec3d modelOffset = Vec3d.ZERO;

    public static String getStatus() {
        return status;
    }

    public static float getPitch() {
        return pitch;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getRoll() {
        return roll;
    }

    public static Vec3d getPos() {
        return pos;
    }

    public static Vec3d getModelOffset() {
        return modelOffset;
    }

    public static void setModelOffset(Vec3d vec3d) {
        modelOffset = vec3d;
    }

    public static boolean isActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        return config.isEnabled() && client.options.getPerspective().isFirstPerson() && client.gameRenderer.getCamera() != null
                && client.player != null && !config.shouldDisableMod(client);
    }

    public static void computeCamera(MinecraftClient client, float tickDelta) {
        modelOffset = Vec3d.ZERO;
        roll = config.getClassicRoll();
        if (config.isClassic()) return;

        // GameRenderer.renderWorld
        MatrixStack matrixStack = new MatrixStack();
        virtualRender(client, tickDelta, matrixStack);

        // ModelPart$Cuboid.renderCuboid
        Vector4f offset = new Vector4f((float)(config.getBindingZ() * config.getScale()),
                -(float)(config.getBindingY() * config.getScale()),
                -(float)(config.getBindingX() * config.getScale()), 1.0F);
        offset.transform(matrixStack.peek().getPositionMatrix());
        pos = new Vec3d(offset.getX(), offset.getY(), offset.getZ());

        Matrix3fc normal = new Matrix3fc(matrixStack.peek().getNormalMatrix()).scale(1.0F, -1.0F, -1.0F);
        normal.rotateLocal((float) Math.toRadians(config.getBindingYaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal((float) Math.toRadians(config.getBindingPitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal((float) Math.toRadians(config.getBindingRoll()), normal.m20, normal.m21, normal.m22);
        Vec3d eulerAngle = MathUtils.getEulerAngleYXZ(normal).multiply(180.0D / Math.PI);
        pitch = (float) eulerAngle.getX();
        yaw = (float) -eulerAngle.getY();
        roll = config.isRollingBound() ? (float) eulerAngle.getZ() : config.getBindingRoll();
    }

    private static void virtualRender(MinecraftClient client, float tickDelta, MatrixStack matrixStack) {
        ClientPlayerEntity player = client.player;
        // WorldRenderer.render
        if (player.age == 0) {
            player.lastRenderX = player.getX();
            player.lastRenderY = player.getY();
            player.lastRenderZ = player.getZ();
        }
        // WorldRenderer.renderEntity
        Vec3d renderOffset = new Vec3d(MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()),
                MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()),
                MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()));
        // EntityRenderDispatcher.render
        if (config.compatPhysicsMod())
            PhysicsModCompat.renderStart(client.getEntityRenderDispatcher(), player, renderOffset.getX(), renderOffset.getY(),
                    renderOffset.getZ(), MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw()), tickDelta, matrixStack);

        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(player);
        renderOffset = renderOffset.add(playerRenderer.getPositionOffset(player, tickDelta));
        matrixStack.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());

        if (config.compatPehkui()) PehkuiCompat.scaleMatrices(matrixStack, player, tickDelta);

        if (config.isUsingModModel()) {
            status = "Successful";
            try {
                matrixStack.push();
                if (!VirtualRenderer.virtualRender(tickDelta, matrixStack)) {
                    return;
                }
            } catch (Throwable throwable) {
                status = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getName();
                matrixStack.pop();
            }
        }

        // PlayerEntityRenderer.render
        ((PlayerEntityRendererAccessor) playerRenderer).invokeSetModelPose(player);
        // LivingEntityRenderer.render
        PlayerEntityModel<AbstractClientPlayerEntity> playerModel = playerRenderer.getModel();
        float n;
        Direction direction;
        playerModel.handSwingProgress = player.getHandSwingProgress(tickDelta);
        playerModel.riding = player.hasVehicle();
        playerModel.child = player.isBaby();
        float h = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float j = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float k = j - h;
        if (player.hasVehicle() && player.getVehicle() instanceof LivingEntity) {
            LivingEntity vehicle = (LivingEntity)player.getVehicle();
            h = MathHelper.lerpAngleDegrees(tickDelta, vehicle.prevBodyYaw, vehicle.bodyYaw);
            k = j - h;
            float l = MathHelper.wrapDegrees(k);
            if (l < -85.0f) l = -85.0f;
            else if (l >= 85.0f) l = 85.0f;
            h = j - l;
            if (l * l > 2500.0f) h += l * 0.2f;
            k = j - h;
        }
        float m = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        if (PlayerEntityRenderer.shouldFlipUpsideDown(player)) {
            m *= -1.0f;
            k *= -1.0f;
        }
        if (player.getPose() == EntityPose.SLEEPING && (direction = player.getSleepingDirection()) != null) {
            n = player.getEyeHeight(EntityPose.STANDING) - 0.1f;
            matrixStack.translate((float) (-direction.getOffsetX()) * n, 0.0f, (float) (-direction.getOffsetZ()) * n);
        }
        float l = player.age + tickDelta;
        ((PlayerEntityRendererAccessor) playerRenderer).invokeSetupTransforms(player, matrixStack, l, h, tickDelta);
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        ((PlayerEntityRendererAccessor) playerRenderer).invokeScale(player, matrixStack, tickDelta);
        matrixStack.translate(0.0f, -1.501f, 0.0f);
        n = 0.0f;
        float o = 0.0f;
        if (!player.hasVehicle() && player.isAlive()) {
            n = MathHelper.lerp(tickDelta, player.lastLimbDistance, player.limbDistance);
            o = player.limbAngle - player.limbDistance * (1.0f - tickDelta);
            if (player.isBaby()) {
                o *= 3.0f;
            }
            if (n > 1.0f) {
                n = 1.0f;
            }
        }
        playerModel.animateModel(player, o, n, tickDelta);
        playerModel.setAngles(player, o, n, l, k, m);
        // AnimalModel.render
        // ModelPart.render
        config.getVanillaModelPart().get(playerRenderer.getModel()).rotate(matrixStack);
    }
}
