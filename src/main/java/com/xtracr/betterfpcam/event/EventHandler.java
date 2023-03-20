package com.xtracr.betterfpcam.event;

import com.xtracr.betterfpcam.KeyController;
import com.xtracr.betterfpcam.camera.CameraController;
import com.xtracr.betterfpcam.config.ConfigController;

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
        if (ConfigController.configController.isEnabled() && (Minecraft.getInstance().options.getCameraType().isFirstPerson() || CameraController.INSTANCE.isActive()) && Minecraft.getInstance().player != null) {
            CameraController.INSTANCE.setCameraOffset(event, Minecraft.getInstance(), event.getPartialTicks());
        }
    }

}
