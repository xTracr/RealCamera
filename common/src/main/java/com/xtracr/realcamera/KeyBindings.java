package com.xtracr.realcamera;

import org.lwjgl.glfw.GLFW;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class KeyBindings {
    
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final ModConfig config = ConfigFile.modConfig; 

    private static final String KEY_CATEGORY = "key.category.xtracr_"+RealCamera.MODID;
    private static final String KEY_ID = "key.xtracr_"+RealCamera.MODID+"_";
    
    public static final KeyBinding toggleCamera = new KeyBinding(
        KEY_ID+"toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, KEY_CATEGORY);
    public static final KeyBinding cameraUP = new KeyBinding(
        KEY_ID+"cameraup", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding cameraDOWN = new KeyBinding(
        KEY_ID+"cameradown", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding cameraIN = new KeyBinding(
        KEY_ID+"camerain", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding cameraOUT = new KeyBinding(
        KEY_ID+"cameraout", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding cameraLEFT = new KeyBinding(
        KEY_ID+"cameraleft", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding cameraRIGHT = new KeyBinding(
        KEY_ID+"cameraright", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding centerUP = new KeyBinding(
        KEY_ID+"centerup", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding centerDOWN = new KeyBinding(
        KEY_ID+"centerdown", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    
    public static void handle() {
        if (MC.player == null || MC.currentScreen != null) {
            return;
        }

        while (toggleCamera.wasPressed()) {
            config.setEnabled(!config.isEnabled());
        }
        while (cameraUP.wasPressed()) {
            if (config.isClassic()) { config.addCameraY(); }
            else { config.addBindingY(); }
        }
        while (cameraDOWN.wasPressed()) {
            if (config.isClassic()) { config.subCameraY(); }
            else { config.subBindingY(); }
        }
        while (cameraIN.wasPressed()) {
            if (config.isClassic()) { config.subCameraX(); }
            else { config.subBindingX(); }
        }
        while (cameraOUT.wasPressed()) {
            if (config.isClassic()) { config.addCameraX(); }
            else { config.addBindingX(); }
        }
        while (cameraLEFT.wasPressed()) {
            if (config.isClassic()) { config.addCameraZ(); }
            else { config.addBindingZ(); }
        }
        while (cameraRIGHT.wasPressed()) {
            if (config.isClassic()) { config.subCameraZ(); }
            else { config.subBindingZ(); }
        }
        while (centerUP.wasPressed()) {
            config.addCenterY();
        }
        while (centerDOWN.wasPressed()) {
            config.subCenterY();
        }
    }

}
