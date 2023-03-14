package com.xtracr.betterfpcam.camera;

import com.xtracr.betterfpcam.config.ConfigController;
import com.xtracr.betterfpcam.mixins.CameraAccessor;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class CameraController {
    
    public static final CameraController INSTANCE = new CameraController();

    private final Minecraft MC = Minecraft.getInstance();
    private final ConfigController config = ConfigController.configController;

    private double cameraX, cameraY, centerX, centerY, centerZ;
    private float xRot, yRot;
    private boolean thirdPersonActive;
    private boolean stopBetterFPCam = false;

    public boolean isActive() {
        return thirdPersonActive;
    }
    public void setInactivate() {
        thirdPersonActive = false;
    }
    public void stopBetterFPCam() {
        stopBetterFPCam = true;
    }

    public boolean doRenderCrosshair(CameraType cameraType) {
        return  (cameraType != CameraType.THIRD_PERSON_BACK || thirdPersonActive) && cameraType != CameraType.THIRD_PERSON_FRONT;
    }

    public void computeClassicCameraOffset(Camera camera, Level level, double particalTicks) {
        if(stopBetterFPCam) {
            stopBetterFPCam = false;
            thirdPersonActive = false;
            return;
        }

        CameraAccessor cameraAccessor = ((CameraAccessor) camera);
        Entity entity = camera.getEntity();

        xRot = entity.getViewXRot((float)particalTicks);
        yRot = entity.getViewYRot((float)particalTicks);
        cameraX = config.getScale() * config.getCameraX();
        cameraY = config.getScale() * config.getCameraY();
        centerX = config.getScale() * 0.0D;
        centerY = config.getScale() * config.getCenterY();
        centerZ = config.getScale() * 0.0D;
        if (config.isThirdPersonMode() && !thirdPersonActive) {
            thirdPersonActive = true;
            MC.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (entity.isCrouching()) {
            centerY -= 0.021875 ;
        }

        
        if (MC.player != null) {
            MC.player.sendMessage(new TextComponent("xRot: " + Float.toString(xRot) + "  yRot: " + Float.toString(yRot)), Util.NIL_UUID);
            MC.player.sendMessage(new TextComponent("cameraY: " + Double.toString(cameraY)), Util.NIL_UUID);
        }

        cameraAccessor.invokeSetPosition(Mth.lerp(particalTicks, entity.xo, entity.getX()), 
            Mth.lerp(particalTicks, entity.yo, entity.getY()) + Mth.lerp(particalTicks, cameraAccessor.getEyeHeightOld(), cameraAccessor.getEyeHeight()), 
            Mth.lerp(particalTicks, entity.zo, entity.getZ())
        );
        cameraAccessor.invokeSetRotation(yRot, 0.0F);
        cameraAccessor.invokeMove(centerX, centerY, centerZ);
        cameraAccessor.invokeSetRotation(yRot, xRot);
        cameraAccessor.invokeMove(cameraX, cameraY, 0.0D);

    }

    public void computeBindingCameraOffset(Camera camera, Level level, double particalTicks) {
        computeClassicCameraOffset(camera, level, particalTicks);
    }


}
