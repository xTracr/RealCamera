package com.xtracr.realcamera.camera;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.math.Matrix3dr;
import com.xtracr.realcamera.mixins.CameraAccessor;
import com.xtracr.realcamera.mixins.PlayerEntityRendererAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class CameraController {
    
    private static final ModConfig config = ConfigFile.modConfig;

    private static Vec3d cameraOffset = Vec3d.ZERO;
    private static Vec3d cameraRotation = Vec3d.ZERO;
    private static float centerYRot = 0.0F;

    public static float cameraRoll = 0.0F;

    @SuppressWarnings("resource")
    public static void debugMessage(String string) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(string));;
        }
    }
    public static boolean isActive() {
        return config.isEnabled() && MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
    }

    public static boolean doCrosshairRotate() {
        return isActive() && config.isDirectionBound() && !config.isClassic();
    }

    public static Vec3d getCameraOffset() {
        return new Vec3d(cameraOffset.getX(), cameraOffset.getY(), cameraOffset.getZ());
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

    public static void setCameraOffset(Camera cameraOld, MinecraftClient MC, float tickDelta) {
        cameraOffset = Vec3d.ZERO;
        cameraRotation = new Vec3d(cameraOld.getPitch(), cameraOld.getYaw(), cameraRoll);

        if (config.isDisabledWhen(MC.player)) return;
        if (config.isRendering() && !config.onlyDisableRenderingWhen(MC.player)) {
            ((CameraAccessor)cameraOld).setThirdPerson(true);
        }

        if (config.isClassic()) {setClassicOffset(cameraOld, MC, tickDelta);}
        else {setBindingOffset(cameraOld, MC, tickDelta);}

        cameraOffset = cameraOld.getPos().subtract(MC.player.getCameraPosVec(tickDelta));
    }

    private static void setClassicOffset(Camera cameraOld, MinecraftClient MC, float tickDelta) {
        CameraAccessor camera = (CameraAccessor)cameraOld;
        ClientPlayerEntity player = MC.player;
        
        float xRot = player.getPitch(tickDelta);
        float yRot = player.getYaw(tickDelta);
        Vec3d offset = new Vec3d(config.getCameraX(), config.getCameraY(), config.getCameraZ()).multiply(config.getScale());
        Vec3d center = new Vec3d(0.0D, config.getCenterY(), 0.0D).multiply(config.getScale());

        if (player.isSneaking()) {
            center = center.add(0.0D, -0.021875, 0.0D);
        }

        if (config.compatPehkui()) {
            offset = PehkuiCompat.scaleVec3d(offset, player, tickDelta);
            center = PehkuiCompat.scaleVec3d(center, player, tickDelta);
        }

        camera.invokeSetRotation(centerYRot, 0.0F);
        camera.invokeMoveBy(center.getX(), center.getY(), center.getZ());
        camera.invokeSetRotation(yRot, xRot);
        camera.invokeMoveBy(offset.getX(), offset.getY(), offset.getZ());
    }

    private static void setBindingOffset(Camera camera, MinecraftClient MC, float tickDelta) {
        ClientPlayerEntity player = MC.player;
        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer)MC.getEntityRenderDispatcher().getRenderer(player);

        // get offset vector
        // GameRenderer.render
        MatrixStack matrices = new MatrixStack();
        // GameRenderer.renderWorld
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cameraRoll));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        // WorldRender.render
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
        //renderOffset = renderOffset.add((config.compatPehkui() ? playerRenderer.getPositionOffset(player, tickDelta) : 
        //    PehkuiCompat.getScaledRenderOffset(playerRenderer, player, tickDelta)));
        renderOffset = renderOffset.add(playerRenderer.getPositionOffset(player, tickDelta));
        matrices.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());

        matrices.peek().getNormalMatrix().identity();
        
        if (config.compatPehkui()) PehkuiCompat.scaleMatrices(matrices, player, tickDelta);
        getMatrixFromEntity(player, playerRenderer, matrices, tickDelta);
        
        // ModelPart$Cube.compile
        double cameraX = config.getScale() * config.getBindingX();
        double cameraY = config.getScale() * config.getBindingY();
        double cameraZ = config.getScale() * config.getBindingZ();
        Vector4f offset =  matrices.peek().getPositionMatrix().transform(new Vector4f((float)cameraZ, -(float)cameraY, -(float)cameraX, 1.0F));

        ((CameraAccessor)camera).invokeMoveBy(-offset.z(), offset.y(), -offset.x());

        if (config.isDirectionBound()) {
            Matrix3dr normal = new Matrix3dr(matrices.peek().getNormalMatrix());
            normal.mulByRight(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
            Vector3f eularAngle = normal.getEulerAngleDegrees().toVector3f();

            float pitch =  eularAngle.x() + config.getPitch();
            float yaw = -eularAngle.y() - config.getYaw();
            float roll =  eularAngle.z() + config.getRoll();

            ((CameraAccessor)camera).invokeSetRotation(yaw, pitch);
            if (!config.isRollingLocked()) {
                cameraRoll = roll;
            }
            cameraRotation = new Vec3d(pitch, yaw, roll);
        }

    }

    private static void getMatrixFromEntity(ClientPlayerEntity player, PlayerEntityRenderer playerRenderer, MatrixStack matrices, float tickDelta) {
        // get modelPart data
        PlayerEntityModel<AbstractClientPlayerEntity> playerModel = playerRenderer.getModel();
        ModelPart modelPart = config.getModelPartFrom(playerModel);

        // PlayerEntityRenderer.render
        ((PlayerEntityRendererAccessor)playerRenderer).invokeSetModelPose(player);
        // LivingEntityRenderer.render
        float n;
        Direction direction;
        playerModel.handSwingProgress = player.getHandSwingProgress(tickDelta);
        playerModel.riding = player.hasVehicle();
        playerModel.child = player.isBaby();
        float h = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float j = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float k = j - h;
        if (player.hasVehicle() && player.getVehicle() instanceof LivingEntity) {
            LivingEntity player2 = (LivingEntity)player.getVehicle();
            h = MathHelper.lerpAngleDegrees(tickDelta, player2.prevBodyYaw, player2.bodyYaw);
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
        if (LivingEntityRenderer.shouldFlipUpsideDown(player)) {
            m *= -1.0f;
            k *= -1.0f;
        }
        if (player.isInPose(EntityPose.SLEEPING) && (direction = player.getSleepingDirection()) != null) {
            n = player.getEyeHeight(EntityPose.STANDING) - 0.1f;
            matrices.translate((float)(-direction.getOffsetX()) * n, 0.0f, (float)(-direction.getOffsetZ()) * n);
        }
        float l = player.age + tickDelta;
        ((PlayerEntityRendererAccessor)playerRenderer).invokeSetupTransforms(player, matrices, l, h, tickDelta);
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.scale(0.9375f, 0.9375f, 0.9375f);
        matrices.translate(0.0f, -1.501f, 0.0f);
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
        modelPart.rotate(matrices);
    }

}
