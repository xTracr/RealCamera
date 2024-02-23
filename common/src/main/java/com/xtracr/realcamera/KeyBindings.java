package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.gui.ModelViewScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.HashSet;

public final class KeyBindings {
    private static final ModConfig config = ConfigFile.modConfig;
    private static final String KEY_CATEGORY = "key.category.xtracr_" + RealCamera.MODID;
    private static final String KEY_ID = "key.xtracr_" + RealCamera.MODID + "_";
    public static final Collection<KeyBinding> KEY_BINDINGS = new HashSet<>();
    private static final KeyBinding MODEL_VIEW_GUI = createKeyBinding("modelViewGui");
    private static final KeyBinding TOGGLE_PERSPECTIVE = createKeyBinding("togglePerspective", GLFW.GLFW_KEY_F6);
    private static final KeyBinding TOGGLE_ADJUST_MODE = createKeyBinding("toggleAdjustMode");
    private static final KeyBinding TOGGLE_CAMERA_MODE = createKeyBinding("toggleCameraMode");
    private static final KeyBinding ADJUST_UP = createKeyBinding("adjustUP");
    private static final KeyBinding ADJUST_DOWN = createKeyBinding("adjustDOWN");
    private static final KeyBinding ADJUST_FRONT = createKeyBinding("adjustFRONT");
    private static final KeyBinding ADJUST_BACK = createKeyBinding("adjustBACK");
    private static final KeyBinding ADJUST_LEFT = createKeyBinding("adjustLEFT");
    private static final KeyBinding ADJUST_RIGHT = createKeyBinding("adjustRIGHT");
    
    public static KeyBinding createKeyBinding(String id) {
        return createKeyBinding(id, InputUtil.UNKNOWN_KEY.getCode());
    }

    public static KeyBinding createKeyBinding(String id, int code) {
        KeyBinding keyBinding = new KeyBinding(KEY_ID + id, code, KEY_CATEGORY);
        KEY_BINDINGS.add(keyBinding);
        return keyBinding;
    }

    public static void handle(MinecraftClient client) {
        if (client.player == null) return;
        while (MODEL_VIEW_GUI.wasPressed()) {
            client.setScreen(new ModelViewScreen());
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
