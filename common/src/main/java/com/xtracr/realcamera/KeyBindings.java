package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.gui.ModelViewScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class KeyBindings {
    private static final String KEY_CATEGORY = "key.category." + RealCamera.FULL_ID;
    private static final String KEY_ID = "key." + RealCamera.FULL_ID + ".";
    private static final List<KeyBinding> KEY_BINDINGS = new ArrayList<>();
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
        return createKeyBinding(id, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static KeyBinding createKeyBinding(String id, int code) {
        KeyBinding keyBinding = new KeyBinding(KEY_ID + id, code, KEY_CATEGORY);
        KEY_BINDINGS.add(keyBinding);
        return keyBinding;
    }

    public static void register(Consumer<KeyBinding> registerer) {
        KEY_BINDINGS.forEach(registerer);
    }

    public static void handle(MinecraftClient client) {
        if (client.player == null) return;
        final ModConfig config = ConfigFile.modConfig;
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
            if (config.isClassic()) config.adjustClassicZ(1);
            else config.adjustBindingZ(true);
            ConfigFile.save();
        }
        while (ADJUST_RIGHT.wasPressed()) {
            if (config.isClassic()) config.adjustClassicZ(-1);
            else config.adjustBindingZ(false);
            ConfigFile.save();
        }
        while (ADJUST_UP.wasPressed()) {
            if (config.isClassic()) config.adjustClassicY(1);
            else config.adjustBindingY(true);
            ConfigFile.save();
        }
        while (ADJUST_DOWN.wasPressed()) {
            if (config.isClassic()) config.adjustClassicY(-1);
            else config.adjustBindingY(false);
            ConfigFile.save();
        }
        while (ADJUST_FRONT.wasPressed()) {
            if (config.isClassic()) config.adjustClassicX(1);
            else config.adjustBindingX(true);
            ConfigFile.save();
        }
        while (ADJUST_BACK.wasPressed()) {
            if (config.isClassic()) config.adjustClassicX(-1);
            else config.adjustBindingX(false);
            ConfigFile.save();
        }
    }
}
