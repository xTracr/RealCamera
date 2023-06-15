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

    public static final KeyBinding TOGGLE_PERSPECTIVE = new KeyBinding(
        KEY_ID+"togglePerspective", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, KEY_CATEGORY);
    public static final KeyBinding TOGGLE_ADJUST_MODE = new KeyBinding(
        KEY_ID+"toggleAdjustMode", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding TOGGLE_CAMERA_MODE = new KeyBinding(
        KEY_ID+"toggleCameraMode", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding ADJUST_UP = new KeyBinding(
        KEY_ID+"adjustUP", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding ADJUST_DOWN = new KeyBinding(
        KEY_ID+"adjustDOWN", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding ADJUST_FRONT = new KeyBinding(
        KEY_ID+"adjustFRONT", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding ADJUST_BACK = new KeyBinding(
        KEY_ID+"adjustBACK", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding ADJUST_LEFT = new KeyBinding(
        KEY_ID+"adjustLEFT", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    public static final KeyBinding ADJUST_RIGHT = new KeyBinding(
        KEY_ID+"adjustRIGHT", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);

    public static void handle(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        while (TOGGLE_PERSPECTIVE.wasPressed()) {
            boolean enabled = config.isEnabled();
            ConfigFile.load();
            config.setEnabled(!enabled);
            ConfigFile.save();
        }
        while (TOGGLE_ADJUST_MODE.wasPressed()) {
            if (config.isClassic()) config.cycleClassicAdjustMode();
            else config.setAdjustOffset(!config.isAdjustingOffset());
            ConfigFile.save();
        }
        while (TOGGLE_CAMERA_MODE.wasPressed()) {
            config.setClassic(!config.isClassic());
            ConfigFile.save();
        }
        while (ADJUST_LEFT.wasPressed()) {
            if (config.isClassic()) config.adjustClassicZ(true);
            else config.adjustBindingZ(true);
            ConfigFile.save();
        }
        while (ADJUST_RIGHT.wasPressed()) {
            if (config.isClassic()) config.adjustClassicZ(false);
            else config.adjustBindingZ(false);
            ConfigFile.save();
        }
        while (ADJUST_UP.wasPressed()) {
            if (config.isClassic()) config.adjustClassicY(true);
            else config.adjustBindingY(true);
            ConfigFile.save();
        }
        while (ADJUST_DOWN.wasPressed()) {
            if (config.isClassic()) config.adjustClassicY(false);
            else config.adjustBindingY(false);
            ConfigFile.save();
        }
        while (ADJUST_FRONT.wasPressed()) {
            if (config.isClassic()) config.adjustClassicX(true);
            else config.adjustBindingX(true);
            ConfigFile.save();
        }
        while (ADJUST_BACK.wasPressed()) {
            if (config.isClassic()) config.adjustClassicX(false);
            else config.adjustBindingX(false);
            ConfigFile.save();
        }
    }
}
