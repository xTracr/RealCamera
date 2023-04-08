package com.xtracr.realcamera.camera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.math.Matrix3d;
import com.xtracr.realcamera.mixins.CameraAccessor;
import com.xtracr.realcamera.mixins.PlayerRendererAccessor;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;

public class CameraController {
    
    private static final ModConfig config = ModConfig.modConfig;

    private static Vec3 cameraOffset = Vec3.ZERO;
    private static Vec3 cameraRotation = Vec3.ZERO;
    private static float centerYRot = 0.0F;

    @SuppressWarnings({"resource","null"})
    public static void debugMessage(String string) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendMessage(new TextComponent(string), Util.NIL_UUID);
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

    @SuppressWarnings("null")
    public static void setCameraOffset(CameraSetup cameraSetup, Minecraft MC, double particalTicks) {
        cameraOffset = Vec3.ZERO;
        cameraRotation = new Vec3(cameraSetup.getPitch(), cameraSetup.getYaw(), cameraSetup.getRoll());

        if (config.isDisabledWhen(MC.player)) return;
        if (config.isRendering() && !config.onlyDisableRenderingWhen(MC.player)) {
            ((CameraAccessor)cameraSetup.getCamera()).setThirdPerson(true);
        }

        if (config.isClassic()) { setClassicOffset(cameraSetup, MC, particalTicks); }
        else { setBindingOffset(cameraSetup, MC, particalTicks); }

        cameraOffset = cameraSetup.getCamera().getPosition().subtract(MC.player.getEyePosition((float)particalTicks));
    }

    @SuppressWarnings("null")
    private static void setClassicOffset(CameraSetup cameraSetup, Minecraft MC, double particalTicks) {
        CameraAccessor camera = (CameraAccessor)cameraSetup.getCamera();
        LocalPlayer player = MC.player;
        
        float xRot = player.getViewXRot((float)particalTicks);
        float yRot = player.getViewYRot((float)particalTicks);
        Vec3 offset = new Vec3(config.getCameraX(), config.getCameraY(), config.getCameraZ()).scale(config.getScale());
        Vec3 center = new Vec3(0.0D, config.getCenterY(), 0.0D).scale(config.getScale());

        if (player.isCrouching()) {
            center = center.add(0.0D, -0.021875, 0.0D);
        }

        if (config.compatPehkui()) {
            offset = PehkuiCompat.scaleVec3d(offset, player, (float)particalTicks);
            center = PehkuiCompat.scaleVec3d(center, player, (float)particalTicks);
        }

        camera.invokeSetRotation(centerYRot, 0.0F);
        camera.invokeMoveBy(center.x(), center.y(), center.z());
        camera.invokeSetRotation(yRot, xRot);
        camera.invokeMoveBy(offset.x(), offset.y(), offset.z());
    }

    @SuppressWarnings("null")
    private static void setBindingOffset(CameraSetup cameraSetup, Minecraft MC, double particalTicks) {
        LocalPlayer player = MC.player;
        Camera camera = cameraSetup.getCamera();
        PlayerRenderer playerRenderer = (PlayerRenderer)MC.getEntityRenderDispatcher().getRenderer(player);

        // get offset vector
        // GameRenderer.render
        PoseStack poseStack = new PoseStack();
        // GameRenderer.renderLevel
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(cameraSetup.getRoll()));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(cameraSetup.getPitch()));
        poseStack.mulPose(Vector3f.YP.rotationDegrees(cameraSetup.getYaw() + 180.0F));
        // EntityRenderDispatcher.render
        Vec3 renderOffset = (config.compatPehkui() ? playerRenderer.getRenderOffset(player, (float)particalTicks) : 
            PehkuiCompat.getScaledRenderOffset(playerRenderer, player, (float)particalTicks));
        // LevelRenderer.renderEntity
        if (player.tickCount == 0) {
            renderOffset = renderOffset.add(player.getX(), player.getY(), player.getZ());
        } else {
            renderOffset = renderOffset.add(Mth.lerp(particalTicks, player.xOld, player.getX()), 
                Mth.lerp(particalTicks, player.yOld, player.getY()), 
                Mth.lerp(particalTicks, player.zOld, player.getZ())
            );
        }
        // EntityRenderDispatcher.render
        renderOffset = renderOffset.subtract(camera.getPosition());
        poseStack.translate(renderOffset.x(), renderOffset.y(), renderOffset.z());

        poseStack.last().normal().setIdentity();
        
        if (config.compatPehkui()) PehkuiCompat.scaleMatrices(poseStack, player, (float)particalTicks);
        getMatrixFromEntity(player, playerRenderer, poseStack, particalTicks);
        
        // ModelPart$Cube.compile
        double cameraX = config.getScale() * config.getBindingX();
        double cameraY = config.getScale() * config.getBindingY();
        double cameraZ = config.getScale() * config.getBindingZ();
        Vector4f offset =  new Vector4f((float)cameraZ, -(float)cameraY, -(float)cameraX, 1.0F);
        offset.transform(poseStack.last().pose());

        ((CameraAccessor)camera).invokeMoveBy(-offset.z(), offset.y(), -offset.x());

        if (config.isDirectionBound()) {
            Matrix3d normal = new Matrix3d(poseStack.last().normal());
            normal.mulByRight(Vector3f.XP.rotationDegrees(180.0F));
            Vector3f eularAngle = new Vector3f(normal.getEulerAngleDegrees());

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
    }

    @SuppressWarnings("null")
    private static void getMatrixFromEntity(LocalPlayer player, PlayerRenderer playerRenderer, PoseStack poseStack, double particalTicks) {
        // get modelPart data
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
        ModelPart modelPart = config.getModelPartFrom(playerModel);

        // LivingEntityRenderer.render
        playerModel.attackTime = player.getAttackAnim((float)particalTicks);

        boolean shouldSit = player.isPassenger() && (player.getVehicle() != null && player.getVehicle().shouldRiderSit());
        playerModel.riding = shouldSit;
        playerModel.young = player.isBaby();
        float yBodyRot = Mth.lerp((float)particalTicks, player.yBodyRotO, player.yBodyRot);
        float yHeadRot = Mth.lerp((float)particalTicks, player.yHeadRotO, player.yHeadRot);
        if (shouldSit && player.getVehicle() instanceof LivingEntity) {
            LivingEntity livingentity = (LivingEntity)player.getVehicle();
            yBodyRot = Mth.rotLerp((float)particalTicks, livingentity.yBodyRotO, livingentity.yBodyRot);
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
        float xPlayerRot = Mth.lerp((float)particalTicks, player.xRotO, player.getXRot());
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

        float bob = (float)player.tickCount + (float)particalTicks;
        ((PlayerRendererAccessor)playerRenderer).invokeSetupRotations(player, poseStack, bob, yBodyRot, (float)particalTicks);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        poseStack.translate(0.0D, -1.501D, 0.0D);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if (!shouldSit && player.isAlive()) {
            f8 = Mth.lerp((float)particalTicks, player.animationSpeedOld, player.animationSpeed);
            f5 = player.animationPosition - player.animationSpeed * (1.0F - (float)particalTicks);
            if (player.isBaby()) {
                f5 *= 3.0F;
            }

            if (f8 > 1.0F) {
                f8 = 1.0F;
            }
        }
        
        playerModel.prepareMobModel(player, f5, f8, (float)particalTicks);
        playerModel.setupAnim(player, f5, f8, bob, f2, xPlayerRot);
        // AgeableListModel.renderToBuffer
        // ModelPart.render
        modelPart.translateAndRotate(poseStack);
    }

}
