package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.utils.CrosshairUtils;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {

    @SubscribeEvent
    public static void onKeyInput(KeyInputEvent event) {
        KeyBindings.handle(MinecraftClient.getInstance());
    }

    @SubscribeEvent
    public static void onCameraUpdate(CameraSetup event) {
        if (RealCameraCore.isActive()) {
            Camera camera = event.getCamera();
            RealCameraCore.updateCamera(camera, event.getRenderer().getClient(), (float)event.getPartialTicks());
            event.setPitch(camera.getPitch());
            event.setYaw(camera.getYaw());
            event.setRoll(RealCameraCore.getRoll());
        }
    }

    @SubscribeEvent
    public static void onRenderWorldStage(RenderLevelStageEvent event) {
        if (RenderLevelStageEvent.Stage.AFTER_SKY.equals(event.getStage())) {
            RealCameraCore.isRenderingWorld = true;
            if (ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
                CrosshairUtils.update(MinecraftClient.getInstance(), event.getCamera(),
                    event.getPoseStack().peek().getPositionMatrix(), event.getProjectionMatrix());
            }
        } else if (RenderLevelStageEvent.Stage.AFTER_WEATHER.equals(event.getStage())) {
            RealCameraCore.isRenderingWorld = false;
        }
    }
}
