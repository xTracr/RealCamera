package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public class EventHandler {
    public static void onClientTick(ClientTickEvent.Post event) {
        KeyBindings.handle(Minecraft.getInstance());
    }

    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (RealCameraCore.isActive()) {
            event.setRoll(RealCameraCore.getRoll(event.getRoll()));
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (RenderLevelStageEvent.Stage.AFTER_SKY.equals(event.getStage())) {
            if (ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
                CrosshairUtil.update(Minecraft.getInstance(), event.getCamera(), event.getModelViewMatrix(), event.getProjectionMatrix());
            }
        }
    }
}
