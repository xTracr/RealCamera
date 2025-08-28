package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.FrameGraphSetupEvent;

public class EventHandler {
    public static void onClientTick(ClientTickEvent.Post event) {
        KeyBindings.handle(Minecraft.getInstance());
    }

    public static void onFrameGraphSetup(FrameGraphSetupEvent event) {
        if (ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            CrosshairUtil.update(Minecraft.getInstance(), event.getCamera(), event.getModelViewMatrix(), event.getProjectionMatrix());
        }
    }
}
