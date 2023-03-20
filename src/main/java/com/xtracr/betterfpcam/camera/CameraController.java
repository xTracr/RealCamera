package com.xtracr.betterfpcam.camera;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import com.xtracr.betterfpcam.config.ConfigController;
import com.xtracr.betterfpcam.mixins.CameraAccessor;
import com.xtracr.betterfpcam.mixins.RendererAccessor;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
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
    
    public static final CameraController INSTANCE = new CameraController();
    private static final ConfigController config = ConfigController.configController;

    private ModelPart modelPart;
    private Vec3 cameraOffset = Vec3.ZERO;
    private float centerYRot = 0.0F;
    private boolean thirdPersonActive = false;
    private boolean stopBetterFPCam = false;
    private boolean wasVisuallySwimming = false;

    public static float x1, y1, z1;

    @SuppressWarnings({"resource","null"})
    public static void debugMessage(String string) {
        if (Minecraft.getInstance().player != null && config.isDebug()) {
            Minecraft.getInstance().player.sendMessage(new TextComponent(string), Util.NIL_UUID);
        }
    }

    public Vec3 getCameraOffset() {
        return this.cameraOffset;
    }

    public boolean isActive() {
        return this.thirdPersonActive;
    }

    public void inactivate() {
        this.thirdPersonActive = false;
    }
    
    public void stopBetterFPCam() {
        this.stopBetterFPCam = true;
    }

    @SuppressWarnings("null")
    public void setCameraOffset(CameraSetup cameraSetup, Minecraft MC, double particalTicks) {
        if(this.stopBetterFPCam) {
            this.stopBetterFPCam = false;
            this.thirdPersonActive = false;
            this.cameraOffset = Vec3.ZERO;
            return;
        }
        if (MC.player.isFallFlying()) {
            this.thirdPersonActive = false;
            MC.options.setCameraType(CameraType.FIRST_PERSON);
            MC.gameRenderer.checkEntityPostEffect(MC.getCameraEntity());
            return;
        }
        if (config.isThirdPersonMode() && !this.thirdPersonActive) {
            this.thirdPersonActive = true;
            MC.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        else if (!config.isThirdPersonMode() && this.thirdPersonActive) {
            this.thirdPersonActive = false;
            MC.options.setCameraType(CameraType.FIRST_PERSON);
            MC.gameRenderer.checkEntityPostEffect(MC.getCameraEntity());
        }

        if (config.isClassic()) { setClassicOffset(cameraSetup, MC, particalTicks); }
        else { setBindingOffset(cameraSetup, MC, particalTicks); }
    }

    @SuppressWarnings("null")
    private void setClassicOffset(CameraSetup cameraSetup, Minecraft MC, double particalTicks) {
        CameraAccessor camera = (CameraAccessor)cameraSetup.getCamera();
        LocalPlayer player = MC.player;
        
        float xRot = player.getViewXRot((float)particalTicks);
        float yRot = player.getViewYRot((float)particalTicks);
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
            if (!this.wasVisuallySwimming) {
                this.wasVisuallySwimming = true;
                this.centerYRot = yRot;
            }
            else if (yRot-this.centerYRot >= 50.0F) { this.centerYRot += 10.0F; }
            else if (yRot-this.centerYRot <=-50.0F) { this.centerYRot -= 10.0F; }
            cameraX = config.getScale() * config.getCameraY();
            cameraY = - config.getScale() * config.getCameraX();
            centerX = config.getScale() * config.getCenterY();
            centerY = 0.0D;
            if (config.isThirdPersonMode()) {
                cameraX -= 0.09375;
                cameraY += 0.109375;
                centerX += 0.9D;
                centerY -= 0.2D;
            }
        }
        else {
            this.wasVisuallySwimming = false;
            this.centerYRot = yRot;
        }

        camera.invokeSetPosition(Mth.lerp(particalTicks, player.xo, player.getX()), 
            Mth.lerp(particalTicks, player.yo, player.getY()) + Mth.lerp(particalTicks, camera.getEyeHeightOld(), camera.getEyeHeight()), 
            Mth.lerp(particalTicks, player.zo, player.getZ())
        );
        this.cameraOffset = ((Camera)camera).getPosition();
        camera.invokeSetRotation(this.centerYRot, 0.0F);
        camera.invokeMove(centerX, centerY, centerZ);
        camera.invokeSetRotation(yRot, xRot);
        camera.invokeMove(cameraX, cameraY, cameraZ);

        this.cameraOffset = ((Camera)camera).getPosition().subtract(this.cameraOffset);
    }

    @SuppressWarnings("null")
    private void setBindingOffset(CameraSetup cameraSetup, Minecraft MC, double particalTicks) {
        LocalPlayer player = MC.player;
        Camera camera = cameraSetup.getCamera();

        // get modelPart data
        PlayerRenderer playerRenderer = (PlayerRenderer)MC.getEntityRenderDispatcher().getRenderer(player);
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
        this.modelPart = playerModel.head;
        Random random = new Random((long)player.getId());
        Cube cube = this.modelPart.getRandomCube(random);
        Vector3f center = new Vector3f((cube.maxX + cube.minX)/32.0F, (cube.maxY + cube.minY)/32.0F, (cube.maxZ + cube.minZ)/32.0F);

        // get offset vector
        // GameRenderer.render
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(cameraSetup.getRoll()));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(cameraSetup.getPitch()));
        poseStack.mulPose(Vector3f.YP.rotationDegrees(cameraSetup.getYaw() + 180.0F));
        // EntityRenderDispatcher.render
        Vec3 renderOffset = playerRenderer.getRenderOffset(player, (float)particalTicks);
        // LevelRenderer.renderEntity
        if (player.tickCount == 0) {
            renderOffset = renderOffset.add(player.getX(), player.getY(), player.getZ());
        }
        else {
            renderOffset = renderOffset.add(Mth.lerp(particalTicks, player.xOld, player.getX()), 
                Mth.lerp(particalTicks, player.yOld, player.getY()), 
                Mth.lerp(particalTicks, player.zOld, player.getZ())
            );
        }
        // EntityRenderDispatcher.render
        renderOffset = renderOffset.subtract(this.cameraOffset);
        poseStack.translate(renderOffset.x(), renderOffset.y(), renderOffset.z());
        // LivingEntityRenderer.render
        float yBodyRot = Mth.lerp((float)particalTicks, player.yBodyRotO, player.yBodyRot);
        float yHeadRot = Mth.lerp((float)particalTicks, player.yHeadRotO, player.yHeadRot);
        if (playerModel.riding && player.getVehicle() instanceof LivingEntity) {
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
        if (player.getPose() == Pose.SLEEPING) {
            Direction direction = player.getBedOrientation();
            if (direction != null) {
               float f4 = player.getEyeHeight(Pose.STANDING) - 0.1F;
               poseStack.translate((double)((float)(-direction.getStepX()) * f4), 0.0D, (double)((float)(-direction.getStepZ()) * f4));
            }
        }
        ((RendererAccessor)playerRenderer).invokeSetupRotations(player, poseStack, (float)player.tickCount + (float)particalTicks, yBodyRot, (float)particalTicks);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        poseStack.translate(0.0D, -1.501D, 0.0D);
        // ModelPart.render
        modelPart.translateAndRotate(poseStack);
        // ModelPart$Cube.compile
        double cameraX = config.getScale() * config.getBindingX() + center.x();
        double cameraY = config.getScale() * config.getBindingY() + center.y();
        double cameraZ = config.getScale() * config.getBindingZ() + center.z();
        Vector4f offset =  new Vector4f((float)cameraZ, (float)cameraY, (float)cameraX, 1.0F);
        offset.transform(poseStack.last().pose());

        ((CameraAccessor)camera).invokeMove(-offset.z(), -offset.y(), offset.x());

        this.cameraOffset = player.getEyePosition((float)particalTicks);
        this.cameraOffset = camera.getPosition().subtract(this.cameraOffset);
        
        debugMessage("target:   xRot: " + Float.toString(camera.getXRot()) + "  yRot: " + Float.toString(camera.getYRot()));
        debugMessage("moveX: " + Float.toString(offset.x()) + "  moveY: " + Float.toString(offset.y()) + "  moveZ: " + Float.toString(offset.z()));
        debugMessage("cameraX: " + Float.toString((float)cameraX) + "  cameraY: " + Float.toString((float)cameraY) + "  cameraZ: " + Float.toString((float)cameraZ));
        debugMessage("offSetX: " + Float.toString((float)cameraOffset.x) + "  offSetY: " + Float.toString((float)cameraOffset.y) + "  offSetZ: " + Float.toString((float)cameraOffset.z));
    }


}
