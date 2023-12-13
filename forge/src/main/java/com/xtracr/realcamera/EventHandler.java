package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.utils.CrosshairUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {
    @SubscribeEvent
    public static void onKeyInput(Key event) {
        KeyBindings.handle(MinecraftClient.getInstance());
    }

    @SubscribeEvent
    public static void onCameraUpdate(ComputeCameraAngles event) {
        if (RealCameraCore.isActive()) {
            event.setPitch(RealCameraCore.getPitch());
            event.setYaw(RealCameraCore.getYaw());
            event.setRoll(RealCameraCore.getRoll());
        }
    }

    @SubscribeEvent
    public static void onRenderWorldStage(RenderLevelStageEvent event) {
        if (RenderLevelStageEvent.Stage.AFTER_SKY.equals(event.getStage())) {
            if (ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
                CrosshairUtils.update(MinecraftClient.getInstance(), event.getCamera(),
                        event.getPoseStack().peek().getPositionMatrix(), event.getProjectionMatrix());
            }
        }
    }
}
