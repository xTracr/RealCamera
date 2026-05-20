package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class EventHandler {
    public static void addListeners() {
        NeoForge.EVENT_BUS.addListener(EventHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(EventHandler::onRenderLevelStage);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        KeyMappings.handle(Minecraft.getInstance());
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (RenderLevelStageEvent.Stage.AFTER_SKY.equals(event.getStage())) {
            if (ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
                CrosshairUtil.update(Minecraft.getInstance(), event.getCamera().getPosition(), event.getModelViewMatrix(), event.getProjectionMatrix());
            }
        }
    }
}
