package com.xtracr.realcamera;

import org.lwjgl.glfw.GLFW;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class KeyController {
    
    private static final ModConfig config = ConfigFile.modConfig;

    private static final String KEY_CATEGORY = "key.category.xtracr_realcamera";
    private static final String KEY_ID = "key.xtracr_realcamera_";

    public static KeyBinding toggleCamera;
    public static KeyBinding cameraUP;
    public static KeyBinding cameraDOWN;
    public static KeyBinding cameraIN;
    public static KeyBinding cameraOUT;
    public static KeyBinding cameraLEFT;
    public static KeyBinding cameraRIGHT;
    public static KeyBinding centerUP;
    public static KeyBinding centerDOWN;

    public static void register() {
        toggleCamera = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, KEY_CATEGORY));
        cameraUP = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"cameraup", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY));
        cameraDOWN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"cameradown", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY));
        cameraIN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"camerain", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY));
        cameraOUT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"cameraout", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY));
        cameraLEFT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"cameraleft", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY));
        cameraRIGHT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"cameraright", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY));
        centerUP = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"centerup", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY));
        centerDOWN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_ID+"centerdown", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleCamera.wasPressed()) {
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
        });
    }
}
