package com.xtracr.betterfpcam;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.betterfpcam.camera.CameraController;
import com.xtracr.betterfpcam.config.ConfigController;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class KeyController {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final ConfigController config = ConfigController.configController; 

    private static final String KEY_CATEGORY = "category.xtracr_betterfpcam";
    
    public static final KeyMapping toggleCamera = new KeyMapping("key.xtracr_betterfpcam_toggle", GLFW.GLFW_KEY_F6, KEY_CATEGORY);
    public static final KeyMapping cameraUP = new KeyMapping("key.xtracr_betterfpcam_cameraup", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping cameraDOWN = new KeyMapping("key.xtracr_betterfpcam_cameradown", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping cameraIN = new KeyMapping("key.xtracr_betterfpcam_camerain", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping cameraOUT = new KeyMapping("key.xtracr_betterfpcam_cameraout", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping cameraLEFT = new KeyMapping("key.xtracr_betterfpcam_cameraleft", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping cameraRIGHT = new KeyMapping("key.xtracr_betterfpcam_cameraright", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping centerUP = new KeyMapping("key.xtracr_betterfpcam_centerup", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping centerDOWN = new KeyMapping("key.xtracr_betterfpcam_centerdown", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    

    public static void keyHandler() {
        if (MC == null || MC.player == null || MC.screen != null) {
            return;
        }

        if (CameraController.INSTANCE.isActive()) {
            for(; MC.options.keyTogglePerspective.consumeClick(); MC.levelRenderer.needsUpdate()) { 
                CameraController.INSTANCE.stopBetterFPCam();
            }
        }

        for(; toggleCamera.consumeClick(); MC.levelRenderer.needsUpdate()) {
            if (config.isEnabled()) {
                config.setDisabled();
                if (CameraController.INSTANCE.isActive()){
                    CameraController.INSTANCE.setInactivate();
                    MC.options.setCameraType(CameraType.FIRST_PERSON);
                    MC.gameRenderer.checkEntityPostEffect(MC.getCameraEntity());
                }
            }
            else {
                config.setEnabled();
            }
        }
        while (cameraUP.consumeClick()) {
            if (config.isClassic()) { config.addCameraY(); }
            else { config.addBindingY(); }
        }
        while (cameraDOWN.consumeClick()) {
            if (config.isClassic()) { config.subCameraY(); }
            else { config.subBindingY(); }
        }
        while (cameraIN.consumeClick()) {
            if (config.isClassic()) { config.subCameraX(); }
            else { config.subBindingX(); }
        }
        while (cameraOUT.consumeClick()) {
            if (config.isClassic()) { config.addCameraX(); }
            else { config.addBindingX(); }
        }
        while (cameraLEFT.consumeClick()) {
            if (config.isClassic()) { config.addCameraZ(); }
            else { config.addBindingZ(); }
        }
        while (cameraRIGHT.consumeClick()) {
            if (config.isClassic()) { config.subCameraZ(); }
            else { config.subBindingZ(); }
        }
        while (centerUP.consumeClick()) {
            config.addCenterY();
        }
        while (centerDOWN.consumeClick()) {
            config.subCenterY();
        }
    }

}
