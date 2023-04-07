package com.xtracr.realcamera;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {
    
    @SubscribeEvent
    public static void onKeyInput(KeyInputEvent event) {
        KeyController.keyHandler();
    }

    @SubscribeEvent
    public static void onCameraSetup(CameraSetup event) {
        if (CameraController.isActive() && Minecraft.getInstance().player != null) {
            CameraController.setCameraOffset(event, Minecraft.getInstance(), event.getPartialTicks());
        }
    }

}
