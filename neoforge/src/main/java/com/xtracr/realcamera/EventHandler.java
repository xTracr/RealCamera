package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public class EventHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        KeyBindings.handle(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onCameraUpdate(ViewportEvent.ComputeCameraAngles event) {
        if (RealCameraCore.isActive()) {
            event.setRoll(RealCameraCore.getRoll(event.getRoll()));
        }
    }

    @SubscribeEvent
    public static void onRenderWorldStage(RenderLevelStageEvent event) {
        if (RenderLevelStageEvent.Stage.AFTER_SKY.equals(event.getStage())) {
            if (ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
                CrosshairUtil.update(Minecraft.getInstance(), event.getCamera(),
                        event.getPoseStack().last().pose(), event.getProjectionMatrix());
            }
        }
    }
}
