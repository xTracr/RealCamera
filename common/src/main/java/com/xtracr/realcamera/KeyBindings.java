package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.gui.ModelViewScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class KeyBindings {
    private static final String KEY_CATEGORY = "key.category." + RealCamera.FULL_ID;
    private static final String KEY_ID = "key." + RealCamera.FULL_ID + ".";
    private static final Map<KeyBinding, Consumer<MinecraftClient>> KEY_BINDINGS = new HashMap<>();

    static {
        createKeyBinding("modelViewGui", client -> client.setScreen(new ModelViewScreen()));
        createKeyBinding("togglePerspective", GLFW.GLFW_KEY_F6, client -> {
            boolean enabled = config().enabled();
            ConfigFile.load();
            config().setEnabled(!enabled);
            RealCameraCore.readyToSendMessage();
        });
        createKeyBinding("toggleAdjustMode", client -> config().cycleAdjustMode());
        createKeyBinding("toggleCameraMode", client -> config().setClassic(!config().isClassic()));
        createKeyBinding("adjustFRONT", client -> config().adjustOffsetX(1));
        createKeyBinding("adjustBACK", client -> config().adjustOffsetX(-1));
        createKeyBinding("adjustUP", client -> config().adjustOffsetY(1));
        createKeyBinding("adjustDOWN", client -> config().adjustOffsetY(-1));
        createKeyBinding("adjustLEFT", client -> config().adjustOffsetZ(1));
        createKeyBinding("adjustRIGHT", client -> config().adjustOffsetZ(-1));
    }

    private static ModConfig config() {
        return ConfigFile.modConfig;
    }

    private static void createKeyBinding(String id, Consumer<MinecraftClient> whenPressed) {
        createKeyBinding(id, GLFW.GLFW_KEY_UNKNOWN, whenPressed);
    }

    private static void createKeyBinding(String id, int code, Consumer<MinecraftClient> whenPressed) {
        KEY_BINDINGS.put(new KeyBinding(KEY_ID + id, code, KEY_CATEGORY), whenPressed);
    }

    public static void register(Consumer<KeyBinding> registerer) {
        KEY_BINDINGS.keySet().forEach(registerer);
    }

    public static void handle(MinecraftClient client) {
        if (client.player == null) return;
        KEY_BINDINGS.forEach((keyBinding, whenPressed) -> {
            while (keyBinding.wasPressed()) {
                whenPressed.accept(client);
                ConfigFile.save();
            }
        });
    }
}
