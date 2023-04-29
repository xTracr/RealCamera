package com.xtracr.realcamera;

import org.lwjgl.glfw.GLFW;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class KeyBindings {
    
    private static final ModConfig config = ConfigFile.modConfig; 

    private static final String KEY_CATEGORY = "key.category.xtracr_"+RealCamera.MODID;
    private static final String KEY_ID = "key.xtracr_"+RealCamera.MODID+"_";
    
    public static final KeyBinding toggleCamera = new KeyBinding(
        KEY_ID+"toggleCamera", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, KEY_CATEGORY);
    public static final KeyBinding toggleAdjustMode = new KeyBinding(
        KEY_ID+"toggleAdjust", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding adjustUP = new KeyBinding(
        KEY_ID+"adjustUP", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding adjustDOWN = new KeyBinding(
        KEY_ID+"adjustDOWN", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding adjustFRONT = new KeyBinding(
        KEY_ID+"adjustFRONT", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding adjustBACK = new KeyBinding(
        KEY_ID+"adjustBACK", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding adjustLEFT = new KeyBinding(
        KEY_ID+"adjustLEFT", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding adjustRIGHT = new KeyBinding(
        KEY_ID+"adjustRIGHT", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    
    public static void handle(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        while (toggleCamera.wasPressed()) {
            boolean enabled = config.isEnabled();
            ConfigFile.load();
            config.setEnabled(!enabled);
        }
        while (toggleAdjustMode.wasPressed()) {
            if (config.isClassic()) config.cycleClassicAdjustMode();
            else config.setAdjustOffset(!config.isAdjustOffset());
        }
        while (adjustLEFT.wasPressed()) {
            if (config.isClassic()) config.adjustClassicZ(true);
            else config.adjustBindingZ(true);
        }
        while (adjustRIGHT.wasPressed()) {
            if (config.isClassic()) config.adjustClassicZ(false);
            else config.adjustBindingZ(false);
        }
        while (adjustUP.wasPressed()) {
            if (config.isClassic()) config.adjustClassicY(true);
            else config.adjustBindingY(true);
        }
        while (adjustDOWN.wasPressed()) {
            if (config.isClassic()) config.adjustClassicY(false);
            else config.adjustBindingY(false);
        }
        while (adjustFRONT.wasPressed()) {
            if (config.isClassic()) config.adjustClassicX(true);
            else config.adjustBindingX(true);
        }
        while (adjustBACK.wasPressed()) {
            if (config.isClassic()) config.adjustClassicX(false);
            else config.adjustBindingX(false);
        }
    }

}
