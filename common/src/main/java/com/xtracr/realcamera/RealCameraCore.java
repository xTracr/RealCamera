package com.xtracr.realcamera;

import org.joml.Matrix3f;
import org.joml.Vector4f;

import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.command.DebugCommand;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.mixins.CameraAccessor;
import com.xtracr.realcamera.mixins.PlayerEntityRendererAccessor;
import com.xtracr.realcamera.utils.MathUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class RealCameraCore {
    
    private static final ModConfig config = ConfigFile.modConfig;

    public static float cameraRoll = 0.0F;

    public static boolean isActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        return config.isEnabled() && client.options.getPerspective().isFirstPerson() && client.gameRenderer.getCamera() != null 
            && client.player != null && !config.isDisabledWhen(client.player);
    }

    public static void updateCamera(Camera camera, MinecraftClient client, float tickDelta) {
        cameraRoll = 0.0F;

        if (config.isRendering() && !config.onlyDisableRenderingWhen(client.player)) {
            ((CameraAccessor)camera).setThirdPerson(true);
        }

        if (config.isClassic()) {
            classicModeUpdate(camera, client, tickDelta);
        } else {
            bindingModeUpdate(camera, client, tickDelta);
        }
    }

    private static void classicModeUpdate(Camera camera, MinecraftClient client, float tickDelta) {
        CameraAccessor cameraAccessor = (CameraAccessor)camera;
        ClientPlayerEntity player = client.player;
        
        float centerYaw = camera.getYaw();
        float pitch = camera.getPitch() + config.getClassicPitch();
        float yaw = centerYaw - config.getClassicYaw();
        cameraRoll = config.getClassicRoll();
        Vec3d offset = new Vec3d(config.getClassicX(), config.getClassicY(), config.getClassicZ()).multiply(config.getScale());
        Vec3d center = new Vec3d(config.getCenterX(), config.getCenterY(), config.getCenterZ()).multiply(config.getScale());

        if (player.isSneaking()) {
            center = center.add(0.0D, -0.021875, 0.0D);
        }
        if (config.compatPehkui()) {
            offset = PehkuiCompat.scaleVec3d(offset, player, tickDelta);
            center = PehkuiCompat.scaleVec3d(center, player, tickDelta);
        }
        if (player.isInSwimmingPose()) {
            offset = offset.rotateZ((float)Math.PI/2);
            center = center.rotateZ((float)Math.PI/2);
        }

        cameraAccessor.invokeSetRotation(centerYaw, 0.0F);
        cameraAccessor.invokeMoveBy(center.getX(), center.getY(), center.getZ());
        cameraAccessor.invokeSetRotation(yaw, pitch);
        cameraAccessor.invokeMoveBy(offset.getX(), offset.getY(), offset.getZ());
    }

    private static void bindingModeUpdate(Camera camera, MinecraftClient client, float tickDelta) {
        
        // GameRenderer.render
        MatrixStack matrixStack = new MatrixStack();
        // GameRenderer.renderWorld
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrixStack.peek().getNormalMatrix().identity();
        // WorldRenderer.render
        ClientPlayerEntity player = client.player;
        if (player.age == 0) {
            player.lastRenderX = player.getX();
            player.lastRenderY = player.getY();
            player.lastRenderZ = player.getZ();
        }
        // WorldRenderer.renderEntity
        Vec3d renderOffset = new Vec3d(MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()), 
            MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()), 
            MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ())
        ).subtract(camera.getPos());
        // EntityRenderDispatcher.render
        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer)client.getEntityRenderDispatcher().getRenderer(player);
        renderOffset = renderOffset.add(playerRenderer.getPositionOffset(player, tickDelta));
        matrixStack.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());

        if (config.compatPehkui()) PehkuiCompat.scaleMatrices(matrixStack, player, tickDelta);
        
        virtualRender(player, playerRenderer, tickDelta, matrixStack);
        
        // ModelPart$Cuboid.renderCuboid
        double cameraX = config.getScale() * config.getBindingX();
        double cameraY = config.getScale() * config.getBindingY();
        double cameraZ = config.getScale() * config.getBindingZ();
        Vector4f offset =  matrixStack.peek().getPositionMatrix().transform(new Vector4f((float)cameraZ, -(float)cameraY, -(float)cameraX, 1.0F));

        ((CameraAccessor)camera).invokeMoveBy(-offset.z(), offset.y(), -offset.x());

        if (config.isDirectionBound()) {
            Matrix3f normal = matrixStack.peek().getNormalMatrix().scale(1.0F, -1.0F, -1.0F);
            normal.rotateLocal(config.getBindingYaw()*(float)Math.PI/180.0F, normal.m10, normal.m11, normal.m12);
            normal.rotateLocal(config.getBindingPitch()*(float)Math.PI/180.0F, normal.m00, normal.m01, normal.m02);
            normal.rotateLocal(config.getBindingRoll()*(float)Math.PI/180.0F, normal.m20, normal.m21, normal.m22);
            Vec3d eulerAngle = MathUtils.getEulerAngleYXZ(normal).multiply(180.0D/Math.PI);

            ((CameraAccessor)camera).invokeSetRotation((float)-eulerAngle.getY(), (float)eulerAngle.getX());
            if (!config.isRollingLocked()) {
                cameraRoll = (float)eulerAngle.getZ();
            }
        }

    }

    private static void virtualRender(AbstractClientPlayerEntity player, PlayerEntityRenderer playerRenderer, float tickDelta, MatrixStack matrixStack) {
        
        DebugCommand.virtualRenderException = null;
        if (config.isUsingModModel()) {
            try {
                matrixStack.push();
                if (!VirtualRenderer.virtualRender(tickDelta, matrixStack)) {
                    return;
                }
            } catch (Exception exception) {
                DebugCommand.virtualRenderException = exception;
                matrixStack.pop();
            }
        }

        // PlayerEntityRenderer.render
        ((PlayerEntityRendererAccessor)playerRenderer).invokeSetModelPose(player);
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
            if (l < -85.0f) {
                l = -85.0f;
            }
            if (l >= 85.0f) {
                l = 85.0f;
            }
            h = j - l;
            if (l * l > 2500.0f) {
                h += l * 0.2f;
            }
            k = j - h;
        }
        float m = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        if (PlayerEntityRenderer.shouldFlipUpsideDown(player)) {
            m *= -1.0f;
            k *= -1.0f;
        }
        if (player.isInPose(EntityPose.SLEEPING) && (direction = player.getSleepingDirection()) != null) {
            n = player.getEyeHeight(EntityPose.STANDING) - 0.1f;
            matrixStack.translate((float)(-direction.getOffsetX()) * n, 0.0f, (float)(-direction.getOffsetZ()) * n);
        }
        float l = player.age + tickDelta;
        ((PlayerEntityRendererAccessor)playerRenderer).invokeSetupTransforms(player, matrixStack, l, h, tickDelta);
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        ((PlayerEntityRendererAccessor)playerRenderer).invokeScale(player, matrixStack, tickDelta);
        matrixStack.translate(0.0f, -1.501f, 0.0f);
        n = 0.0f;
        float o = 0.0f;
        if (!player.hasVehicle() && player.isAlive()) {
            n = player.limbAnimator.getSpeed(tickDelta);
            o = player.limbAnimator.getPos(tickDelta);
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
