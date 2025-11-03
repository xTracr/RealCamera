package com.xtracr.realcamera;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.gui.ModelViewScreen;
import com.xtracr.realcamera.util.LocUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class KeyMappings {
    private static final Map<KeyMapping, Consumer<Minecraft>> KEY_MAPPINGS = new HashMap<>();
    public static final KeyMapping MODEL_VIEW_SCREEN;

    static {
        MODEL_VIEW_SCREEN = createKeyMapping("modelViewScreen", client -> client.setScreen(new ModelViewScreen()));
        createKeyMapping("togglePerspective", InputConstants.KEY_F6, client -> {
            boolean enabled = ConfigFile.config().enabled();
            ConfigFile.load();
            ConfigFile.config().setEnabled(!enabled);
            RealCameraCore.reset();
        });
        createKeyMapping("toggleAdjustMode", client -> ConfigFile.config().cycleAdjustMode());
        createKeyMapping("toggleCameraMode", client -> ConfigFile.config().setClassic(!ConfigFile.config().isClassic()));
        createKeyMapping("adjustFRONT", client -> ConfigFile.config().adjustOffsetX(1));
        createKeyMapping("adjustBACK", client -> ConfigFile.config().adjustOffsetX(-1));
        createKeyMapping("adjustUP", client -> ConfigFile.config().adjustOffsetY(1));
        createKeyMapping("adjustDOWN", client -> ConfigFile.config().adjustOffsetY(-1));
        createKeyMapping("adjustLEFT", client -> ConfigFile.config().adjustOffsetZ(1));
        createKeyMapping("adjustRIGHT", client -> ConfigFile.config().adjustOffsetZ(-1));
        createKeyMapping("activeConfigIndexNEXT", client -> ConfigFile.config().binding.activeConfigIndex += 1);
        createKeyMapping("activeConfigIndexPREV", client -> ConfigFile.config().binding.activeConfigIndex -= 1);
        createKeyMapping("activeConfigIndexRESET", client -> ConfigFile.config().binding.activeConfigIndex = 0);
    }

    private static KeyMapping createKeyMapping(String id, Consumer<Minecraft> whenPressed) {
        return createKeyMapping(id, -1, whenPressed);
    }

    private static KeyMapping createKeyMapping(String id, int code, Consumer<Minecraft> whenPressed) {
        KeyMapping keyMapping = new KeyMapping("key." + RealCamera.FULL_ID + "." + id, code, LocUtil.KEY_MOD_NAME);
        KEY_MAPPINGS.put(keyMapping, whenPressed);
        return keyMapping;
    }

    public static void register(Consumer<KeyMapping> registerer) {
        KEY_MAPPINGS.keySet().forEach(registerer);
    }

    public static void handle(Minecraft client) {
        if (client.player == null) return;
        KEY_MAPPINGS.forEach((keyMapping, whenPressed) -> {
            while (keyMapping.consumeClick()) {
                whenPressed.accept(client);
                ConfigFile.save();
            }
        });
    }
}
