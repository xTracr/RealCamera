package com.xtracr.realcamera.camera;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.math.Matrix3dr;
import com.xtracr.realcamera.mixins.CameraAccessor;
import com.xtracr.realcamera.mixins.PlayerRendererAccessor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;

public class CameraController {
    
    private static final ModConfig config = ModConfig.modConfig;

    private static Vec3 cameraOffset = Vec3.ZERO;
    private static Vec3 cameraRotation = Vec3.ZERO;
    private static float centerYRot = 0.0F;
    private static boolean wasSwimming = false;

    @SuppressWarnings("resource")
    public static void debugMessage(String string) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(string));;
        }
    }

    public static boolean isActive() {
        return config.isEnabled() && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    public static boolean doCrosshairRotate() {
        return isActive() && config.isDirectionBound() && !config.isClassic();
    }

    public static Vec3 getCameraOffset() {
        return new Vec3(cameraOffset.x(), cameraOffset.y(), cameraOffset.z());
    }

    public static Vec3 getCameraDirection() {
        float f = (float)cameraRotation.x() * ((float)Math.PI / 180F);
        float f1 = -(float)cameraRotation.y() * ((float)Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
    }

    public static void setCameraOffset(ComputeCameraAngles cameraSetup, Minecraft MC, double particalTick) {
        cameraOffset = Vec3.ZERO;
        cameraRotation = new Vec3(cameraSetup.getPitch(), cameraSetup.getYaw(), cameraSetup.getRoll());

        if (config.isDisabledWhen(MC.player)) return;
        if (config.isRendering() && !config.onlyDisableRenderingWhen(MC.player)) {
            ((CameraAccessor)cameraSetup.getCamera()).setThirdPerson(true);
        }

        if (config.isClassic()) { setClassicOffset(cameraSetup, MC, particalTick); }
        else { setBindingOffset(cameraSetup, MC, particalTick); }
    }

    private static void setClassicOffset(ComputeCameraAngles cameraSetup, Minecraft MC, double particalTick) {
        CameraAccessor camera = (CameraAccessor)cameraSetup.getCamera();
        LocalPlayer player = MC.player;
        
        float xRot = player.getViewXRot((float)particalTick);
        float yRot = player.getViewYRot((float)particalTick);
        double cameraX = config.getScale() * config.getCameraX();
        double cameraY = config.getScale() * config.getCameraY();
        double cameraZ = config.getScale() * config.getCameraZ();
        double centerX = config.getScale() * 0.0D;
        double centerY = config.getScale() * config.getCenterY();
        double centerZ = config.getScale() * 0.0D;

        if (player.isCrouching()) {
            centerY -= 0.021875;
        }
        else if (player.isSleeping()) {
            centerY = cameraX;
            cameraX = 0.0D;
            cameraY = 0.0D;
            cameraZ = 0.0D;
        }
        if (player.isVisuallySwimming()) {
            if (!wasSwimming) {
                wasSwimming = true;
                centerYRot = yRot;
            }
            else if (yRot-centerYRot >= 50.0F) { centerYRot += 10.0F; }
            else if (yRot-centerYRot <=-50.0F) { centerYRot -= 10.0F; }
            cameraX = config.getScale() * config.getCameraY();
            cameraY = - config.getScale() * config.getCameraX();
            centerX = config.getScale() * config.getCenterY();
            centerY = 0.0D;
            if (config.isRendering() && !config.onlyDisableRenderingWhen(MC.player)) {
                cameraX -= 0.09375;
                cameraY += 0.109375;
                centerX += 0.9D;
                centerY -= 0.2D;
            }
        }
        else {
            wasSwimming = false;
            centerYRot = yRot;
        }

        camera.invokeSetPos(Mth.lerp(particalTick, player.xo, player.getX()), 
        Mth.lerp(particalTick, player.yo, player.getY()) + Mth.lerp(particalTick, camera.getCameraY(), camera.getLastCameraY()), 
        Mth.lerp(particalTick, player.zo, player.getZ())
        );
        camera.invokeSetRotation(centerYRot, 0.0F);
        camera.invokeMoveBy(centerX, centerY, centerZ);
        camera.invokeSetRotation(yRot, xRot);
        camera.invokeMoveBy(cameraX, cameraY, cameraZ);

        cameraOffset = ((Camera)camera).getPosition().subtract(player.getEyePosition((float)particalTick));
    }

    private static void setBindingOffset(ComputeCameraAngles cameraSetup, Minecraft MC, double particalTick) {
        LocalPlayer player = MC.player;
        Camera camera = cameraSetup.getCamera();
        PlayerRenderer playerRenderer = (PlayerRenderer)MC.getEntityRenderDispatcher().getRenderer(player);

        // get offset vector
        // GameRenderer.render
        PoseStack poseStack = new PoseStack();
        // GameRenderer.renderLevel
        poseStack.mulPose(Axis.ZP.rotationDegrees(cameraSetup.getRoll()));
        poseStack.mulPose(Axis.XP.rotationDegrees(cameraSetup.getPitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(cameraSetup.getYaw() + 180.0F));
        // EntityRenderDispatcher.render
        Vec3 renderOffset = playerRenderer.getRenderOffset(player, (float)particalTick);
        // LevelRenderer.renderEntity
        if (player.tickCount == 0) {
            renderOffset = renderOffset.add(player.getX(), player.getY(), player.getZ());
        }
        else {
            renderOffset = renderOffset.add(Mth.lerp(particalTick, player.xOld, player.getX()), 
                Mth.lerp(particalTick, player.yOld, player.getY()), 
                Mth.lerp(particalTick, player.zOld, player.getZ())
            );
        }
        // EntityRenderDispatcher.render
        renderOffset = renderOffset.subtract(camera.getPosition());
        poseStack.translate(renderOffset.x(), renderOffset.y(), renderOffset.z());

        poseStack.last().normal().identity();
        
        getMatrixFromEntity(player, playerRenderer, poseStack, particalTick);
        
        // ModelPart$Cube.compile
        double cameraX = config.getScale() * config.getBindingX();
        double cameraY = config.getScale() * config.getBindingY();
        double cameraZ = config.getScale() * config.getBindingZ();
        Vector4f offset =  poseStack.last().pose().transform(new Vector4f((float)cameraZ, -(float)cameraY, -(float)cameraX, 1.0F));

        ((CameraAccessor)camera).invokeMoveBy(-offset.z(), offset.y(), -offset.x());

        if (config.isDirectionBound()) {
            Matrix3dr normal = new Matrix3dr(poseStack.last().normal());
            normal.mulByRight(Axis.XP.rotationDegrees(180.0F));
            Vector3f eularAngle = normal.getEulerAngleDegrees().toVector3f();

            float pitch =  eularAngle.x() + config.getPitch();
            float yaw = -eularAngle.y() - config.getYaw();
            float roll =  eularAngle.z() + config.getRoll();

            ((CameraAccessor)camera).invokeSetRotation(yaw, pitch);
            cameraSetup.setPitch(pitch);
            cameraSetup.setYaw(yaw);
            if (!config.isRollingLocked()) {
                cameraSetup.setRoll(roll);
            }
            cameraRotation = new Vec3(pitch, yaw, roll);
        }

        cameraOffset = camera.getPosition().subtract(player.getEyePosition((float)particalTick));
    }

    private static void getMatrixFromEntity(LocalPlayer player, PlayerRenderer playerRenderer, PoseStack poseStack, double particalTick) {
        // get modelPart data
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
        ModelPart modelPart = config.getModelPartFrom(playerModel);

        // LivingEntityRenderer.render
        playerModel.attackTime = player.getAttackAnim((float)particalTick);

        boolean shouldSit = player.isPassenger() && (player.getVehicle() != null && player.getVehicle().shouldRiderSit());
        playerModel.riding = shouldSit;
        playerModel.young = player.isBaby();
        float yBodyRot = Mth.lerp((float)particalTick, player.yBodyRotO, player.yBodyRot);
        float yHeadRot = Mth.lerp((float)particalTick, player.yHeadRotO, player.yHeadRot);
        if (shouldSit && player.getVehicle() instanceof LivingEntity) {
            LivingEntity livingentity = (LivingEntity)player.getVehicle();
            yBodyRot = Mth.rotLerp((float)particalTick, livingentity.yBodyRotO, livingentity.yBodyRot);
            float f3 = Mth.wrapDegrees(yHeadRot - yBodyRot);
            if (f3 < -85.0F) {
               f3 = -85.0F;
            }
            if (f3 >= 85.0F) {
               f3 = 85.0F;
            }
            yBodyRot = yHeadRot - f3;
            if (f3 * f3 > 2500.0F) {
               yBodyRot += f3 * 0.2F;
            }
        }

        float f2 = yHeadRot - yBodyRot;
        float xPlayerRot = Mth.lerp((float)particalTick, player.xRotO, player.getXRot());
        if (PlayerRenderer.isEntityUpsideDown(player)) {
            f2 *= -1.0F;
            xPlayerRot *= -1.0F;

        }

        if (player.getPose() == Pose.SLEEPING) {
            Direction direction = player.getBedOrientation();
            if (direction != null) {
               float f4 = player.getEyeHeight(Pose.STANDING) - 0.1F;
               poseStack.translate((double)((float)(-direction.getStepX()) * f4), 0.0D, (double)((float)(-direction.getStepZ()) * f4));
            }
        }

        float bob = (float)player.tickCount + (float)particalTick;
        ((PlayerRendererAccessor)playerRenderer).invokeSetupRotations(player, poseStack, bob, yBodyRot, (float)particalTick);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        poseStack.translate(0.0D, -1.501D, 0.0D);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if (!shouldSit && player.isAlive()) {
            f8 = player.walkAnimation.speed((float)particalTick);
            f5 = player.walkAnimation.position((float)particalTick);
            if (player.isBaby()) {
                f5 *= 3.0F;
            }

            if (f8 > 1.0F) {
                f8 = 1.0F;
            }
        }
        
        playerModel.prepareMobModel(player, f5, f8, (float)particalTick);
        playerModel.setupAnim(player, f5, f8, bob, f2, xPlayerRot);
        // AgeableListModel.renderToBuffer
        // ModelPart.render
        modelPart.translateAndRotate(poseStack);
    }

}
