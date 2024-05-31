package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.gui.ModelViewScreen;
import com.xtracr.realcamera.util.LocUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class KeyBindings {
    private static final Map<KeyMapping, Consumer<Minecraft>> KEY_BINDINGS = new HashMap<>();

    static {
        createKeyBinding("modelViewScreen", client -> client.setScreen(new ModelViewScreen()));
        createKeyBinding("togglePerspective", GLFW.GLFW_KEY_F6, client -> {
            boolean enabled = ConfigFile.config().enabled();
            ConfigFile.load();
            ConfigFile.config().setEnabled(!enabled);
            RealCameraCore.readyToSendMessage();
        });
        createKeyBinding("toggleAdjustMode", client -> ConfigFile.config().cycleAdjustMode());
        createKeyBinding("toggleCameraMode", client -> ConfigFile.config().setClassic(!ConfigFile.config().isClassic()));
        createKeyBinding("adjustFRONT", client -> ConfigFile.config().adjustOffsetX(1));
        createKeyBinding("adjustBACK", client -> ConfigFile.config().adjustOffsetX(-1));
        createKeyBinding("adjustUP", client -> ConfigFile.config().adjustOffsetY(1));
        createKeyBinding("adjustDOWN", client -> ConfigFile.config().adjustOffsetY(-1));
        createKeyBinding("adjustLEFT", client -> ConfigFile.config().adjustOffsetZ(1));
        createKeyBinding("adjustRIGHT", client -> ConfigFile.config().adjustOffsetZ(-1));
    }

    private static void createKeyBinding(String id, Consumer<Minecraft> whenPressed) {
        createKeyBinding(id, GLFW.GLFW_KEY_UNKNOWN, whenPressed);
    }

    private static void createKeyBinding(String id, int code, Consumer<Minecraft> whenPressed) {
        KEY_BINDINGS.put(new KeyMapping("key." + RealCamera.FULL_ID + "." + id, code, LocUtil.KEY_MOD_NAME), whenPressed);
    }

    public static void register(Consumer<KeyMapping> registerer) {
        KEY_BINDINGS.keySet().forEach(registerer);
    }

    public static void handle(Minecraft client) {
        if (client.player == null) return;
        KEY_BINDINGS.forEach((keyBinding, whenPressed) -> {
            while (keyBinding.consumeClick()) {
                whenPressed.accept(client);
                ConfigFile.save();
            }
        });
    }
}
