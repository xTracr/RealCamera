package com.xtracr.realcamera;

import com.xtracr.realcamera.camera.CameraController;
import com.xtracr.realcamera.config.ConfigController;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent event) {
        KeyController.keyHandler();
    }

    @SubscribeEvent
    public static void onCameraSetup(CameraSetup event) {
        if (ConfigController.configController.isEnabled() && (Minecraft.getInstance().options.getCameraType().isFirstPerson() || CameraController.INSTANCE.isThirdPersonActive()) && Minecraft.getInstance().player != null) {
            CameraController.INSTANCE.setCameraOffset(event, Minecraft.getInstance(), event.getPartialTicks());
        }
    }

}
