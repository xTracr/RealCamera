package com.xtracr.realcamera.camera;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.xtracr.realcamera.command.ClientCommand;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.mixins.CameraAccessor;
import com.xtracr.realcamera.mixins.PlayerEntityRendererAccessor;
import com.xtracr.realcamera.utils.Matrix3dr;
import com.xtracr.realcamera.utils.VirtualRenderer;

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

public class CameraController {
    
    private static final ModConfig config = ConfigFile.modConfig;

    private static Vec3d cameraOffset = Vec3d.ZERO;
    private static Vec3d cameraRotation = Vec3d.ZERO;

    public static float cameraRoll = 0.0F;

    public static boolean isActive() {
        MinecraftClient MC = MinecraftClient.getInstance();
        return config.isEnabled() && MC.options.getPerspective().isFirstPerson() && !config.isDisabledWhen(MC.player);
    }

    public static boolean doCrosshairRotate() {
        return isActive() && config.isDirectionBound() && !config.isClassic();
    }

    public static Vec3d getCameraOffset() {
        return new Vec3d(cameraOffset.getX(), cameraOffset.getY(), cameraOffset.getZ());
    }

    public static Vec3d getCameraRotation() {
        return new Vec3d(cameraRotation.getX(), cameraRotation.getY(), cameraRotation.getZ());
    }

    public static Vec3d getCameraDirection() {
        float f = (float)cameraRotation.getX() * ((float)Math.PI / 180);
        float g = -(float)cameraRotation.getY() * ((float)Math.PI / 180);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public static void setCameraOffset(Camera camera, MinecraftClient MC, float tickDelta) {
        cameraRoll = 0.0F;
        cameraOffset = Vec3d.ZERO;
        cameraRotation = new Vec3d(camera.getPitch(), camera.getYaw(), cameraRoll);

        if (config.isRendering() && !config.onlyDisableRenderingWhen(MC.player)) {
            ((CameraAccessor)camera).setThirdPerson(true);
        }

        if (config.isClassic()) { setClassicOffset(camera, MC, tickDelta); }
        else { setBindingOffset(camera, MC, tickDelta); }

        cameraOffset = camera.getPos().subtract(MC.player.getCameraPosVec(tickDelta));
        cameraRotation = new Vec3d(camera.getPitch(), camera.getYaw(), cameraRoll);
    }

    private static void setClassicOffset(Camera camera, MinecraftClient MC, float tickDelta) {
        CameraAccessor cameraAccessor = (CameraAccessor)camera;
        ClientPlayerEntity player = MC.player;
        
        float xRot = camera.getPitch();
        float yRot = camera.getYaw();
        float centerYRot = yRot;
        Vec3d offset = new Vec3d(config.getCameraX(), config.getCameraY(), config.getCameraZ()).multiply(config.getScale());
        Vec3d center = new Vec3d(config.getCenterX(), config.getCenterY(), config.getCenterZ()).multiply(config.getScale());

        if (player.isSneaking()) {
            center = center.add(0.0D, -0.021875, 0.0D);
        } else if (player.isInSwimmingPose()) {
            offset = offset.rotateZ((float)Math.PI/2);
            center = center.rotateZ((float)Math.PI/2);
        }

        if (config.compatPehkui()) {
            offset = PehkuiCompat.scaleVec3d(offset, player, tickDelta);
            center = PehkuiCompat.scaleVec3d(center, player, tickDelta);
        }

        cameraAccessor.invokeSetRotation(centerYRot, 0.0F);
        cameraAccessor.invokeMoveBy(center.getX(), center.getY(), center.getZ());
        cameraAccessor.invokeSetRotation(yRot, xRot);
        cameraAccessor.invokeMoveBy(offset.getX(), offset.getY(), offset.getZ());
    }

    private static void setBindingOffset(Camera camera, MinecraftClient MC, float tickDelta) {
        
        // GameRenderer.render
        MatrixStack matrixStack = new MatrixStack();
        // GameRenderer.renderWorld
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cameraRoll));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrixStack.peek().getNormalMatrix().identity();
        // WorldRenderer.render
        ClientPlayerEntity player = MC.player;
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
        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer)MC.getEntityRenderDispatcher().getRenderer(player);
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
            Matrix3dr normal = new Matrix3dr(matrixStack.peek().getNormalMatrix());
            normal.mulByRight(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
            Vector3f eularAngle = normal.getEulerAngleDegrees().toVector3f();

            float pitch =  eularAngle.x() + config.getPitch();
            float yaw = -eularAngle.y() - config.getYaw();
            float roll =  eularAngle.z() + config.getRoll();

            ((CameraAccessor)camera).invokeSetRotation(yaw, pitch);
            if (!config.isRollingLocked()) {
                cameraRoll = roll;
            }
        }

    }

    private static void virtualRender(AbstractClientPlayerEntity player, PlayerEntityRenderer playerRenderer, float tickDelta, MatrixStack matrixStack) {
        
        ClientCommand.virtualRenderException = null;
        if (config.isUsingModModel()) {
            try {
                matrixStack.push();
                if (!VirtualRenderer.virtualRender(tickDelta, matrixStack)) {
                    return;
                }
            } catch (Exception exception) {
                ClientCommand.virtualRenderException = exception;
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
        /*
        if (playerModel.child) {
            float f;
            matrixStack.push();
            if (playerModel.headScaled) {
                f = 1.5f / playerModel.invertedChildHeadScale;
                matrixStack.scale(f, f, f);
            }
            matrixStack.translate(0.0f, playerModel.childHeadYOffset / 16.0f, playerModel.childHeadZOffset / 16.0f);
            config.getVanillaModelPart().get(renderer.getModel()).rotate(matrixStack);
            if (...) return; 
            matrixStack.pop();
            matrixStack.push();
            f = 1.0f / playerModel.invertedChildBodyScale;
            matrixStack.scale(f, f, f);
            matrixStack.translate(0.0f, playerModel.childBodyYOffset / 16.0f, 0.0f);
            config.getVanillaModelPart().get(renderer.getModel()).rotate(matrixStack);
        }
         */
        // ModelPart.render
        config.getVanillaModelPart().get(playerRenderer.getModel()).rotate(matrixStack);
    }
}
