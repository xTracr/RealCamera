package com.xtracr.betterfpcam.camera;

import com.xtracr.betterfpcam.config.ConfigController;
import com.xtracr.betterfpcam.mixins.CameraAccessor;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class CameraController {
    
    public static final CameraController INSTANCE = new CameraController();
    private static final ConfigController config = ConfigController.configController;

    private static double cameraX, cameraY, cameraZ, centerX, centerY, centerZ;
    private static float xRot, yRot, centerYRot = 0.0F;

    public Vec3 cameraOffset = Vec3.ZERO;
    private boolean thirdPersonActive;
    private boolean stopBetterFPCam = false;

    @SuppressWarnings({"resource","null"})
    public static void debugMessage(String string) {
        if (Minecraft.getInstance().player != null && config.isDebug()) {
            Minecraft.getInstance().player.sendMessage(new TextComponent(string), Util.NIL_UUID);
        }
    }

    public boolean isActive() {
        return this.thirdPersonActive;
    }

    public void setInactivate() {
        this.thirdPersonActive = false;
    }
    
    public void stopBetterFPCam() {
        this.stopBetterFPCam = true;
    }

    public void setCameraOffset(Camera camera, Minecraft MC, double particalTicks) {
        if(this.stopBetterFPCam) {
            this.stopBetterFPCam = false;
            this.thirdPersonActive = false;
            this.cameraOffset = Vec3.ZERO;
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
        
        if (config.isClassic()) { setClassicOffset(camera, MC, particalTicks); }
        else { setBindingOffset(camera, MC, particalTicks); }

        debugMessage("xRot: " + Float.toString(xRot) + "  yRot: " + Float.toString(yRot));
        debugMessage("cameraY: " + Double.toString(cameraY));
        //debugMessage("offSetX: " + Float.toString((float)cameraOffset.x) + "  offSetY: " + Float.toString((float)cameraOffset.y) + "  offSetZ: " + Float.toString((float)cameraOffset.z));
    }

    private void setClassicOffset(Camera camera, Minecraft MC, double particalTicks) {
        CameraAccessor cameraAccessor = ((CameraAccessor) camera);
        Entity entity = camera.getEntity();
        
        xRot = entity.getViewXRot((float)particalTicks);
        yRot = entity.getViewYRot((float)particalTicks);
        cameraX = config.getScale() * config.getCameraX();
        cameraY = config.getScale() * config.getCameraY();
        cameraZ = config.getScale() * config.getCameraZ();
        centerX = config.getScale() * 0.0D;
        centerY = config.getScale() * config.getCenterY();
        centerZ = config.getScale() * 0.0D;

        if (entity.isVisuallySwimming() ) {
            if (centerYRot == 0.0F) { centerYRot = yRot; }
            else { centerYRot += (yRot-centerYRot) - (yRot-centerYRot)%10.0F; }
            cameraX = config.getScale() * config.getCameraY();
            cameraY = - config.getScale() * config.getCameraX();
            centerX = config.getScale() * config.getCenterY();
            centerY = config.getScale() * 0.0D;
        }
        else {
            centerYRot = 0.0F;
            if (entity.isCrouching()) {
                centerY -= 0.021875 ;
            }
            else if ((Entity)MC.player == entity && MC.player.isFallFlying()) {

            }
        }

        cameraAccessor.invokeSetPosition(Mth.lerp(particalTicks, entity.xo, entity.getX()), 
            Mth.lerp(particalTicks, entity.yo, entity.getY()) + Mth.lerp(particalTicks, cameraAccessor.getEyeHeightOld(), cameraAccessor.getEyeHeight()), 
            Mth.lerp(particalTicks, entity.zo, entity.getZ())
        );
        this.cameraOffset = ((Camera)cameraAccessor).getPosition();
        cameraAccessor.invokeSetRotation(centerYRot, 0.0F);
        cameraAccessor.invokeMove(centerX, centerY, centerZ);
        cameraAccessor.invokeSetRotation(yRot, xRot);
        cameraAccessor.invokeMove(cameraX, cameraY, cameraZ);

        this.cameraOffset = ((Camera)cameraAccessor).getPosition().subtract(this.cameraOffset);
    }

    private void setBindingOffset(Camera camera, Minecraft MC, double particalTicks) {
        PlayerRenderer playerRenderer = (PlayerRenderer)MC.getEntityRenderDispatcher().getRenderer(MC.player);
        //EntityRenderer<? super Player> playerRenderer = minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
        ModelPart modelPart = playerModel.head;

        CameraAccessor cameraAccessor = ((CameraAccessor) camera);
        Entity entity = camera.getEntity();

        xRot = modelPart.xRot;
        yRot = modelPart.yRot;
        cameraX = config.getScale() * config.getBindingX() + modelPart.x;
        cameraY = config.getScale() * config.getBindingY() + modelPart.y;
        cameraZ = config.getScale() * config.getBindingZ() + modelPart.z;

        cameraAccessor.invokeSetPosition(Mth.lerp(particalTicks, entity.xo, entity.getX()), 
            Mth.lerp(particalTicks, entity.yo, entity.getY()) + Mth.lerp(particalTicks, cameraAccessor.getEyeHeightOld(), cameraAccessor.getEyeHeight()), 
            Mth.lerp(particalTicks, entity.zo, entity.getZ())
        );
        this.cameraOffset = ((Camera)cameraAccessor).getPosition();
        cameraAccessor.invokeSetRotation(yRot, xRot);
        cameraAccessor.invokeMove(cameraX, cameraY, cameraZ);

        this.cameraOffset = ((Camera)cameraAccessor).getPosition().subtract(this.cameraOffset);
    }


}
