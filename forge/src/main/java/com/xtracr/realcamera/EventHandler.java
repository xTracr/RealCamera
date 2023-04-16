package com.xtracr.realcamera;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {
    
    @SubscribeEvent
    public static void onCameraSetup(CameraSetup event) {
        if (CameraController.isActive() && MinecraftClient.getInstance().player != null) {
            Camera camera = event.getCamera();
            CameraController.setCameraOffset(camera, MinecraftClient.getInstance(), (float)event.getPartialTicks());
            event.setPitch(camera.getPitch());
            event.setYaw(camera.getYaw());
            event.setRoll(CameraController.cameraRoll);
        }
    }

}
