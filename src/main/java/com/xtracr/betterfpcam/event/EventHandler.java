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
    @SuppressWarnings("resource")
    public static void onCameraSetup(CameraSetup event) {
        if (ConfigController.configController.isEnabled() && (Minecraft.getInstance().options.getCameraType().isFirstPerson() || CameraController.INSTANCE.isActive()) && Minecraft.getInstance().level != null) {
            if (ConfigController.configController.isClassic()) {
                CameraController.INSTANCE.computeClassicCameraOffset(event.getCamera(), Minecraft.getInstance().level, event.getPartialTicks());
            }
            else {
                CameraController.INSTANCE.computeBindingCameraOffset(event.getCamera(), Minecraft.getInstance().level, event.getPartialTicks());
            }
        }
    }

    
}
