package com.xtracr.realcamera;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {
    
    @SubscribeEvent
    public static void onCameraSetup(ComputeCameraAngles event) {
        if (CameraController.isActive() && MinecraftClient.getInstance().player != null) {
            Camera camera = event.getCamera();
            CameraController.setCameraOffset(camera, MinecraftClient.getInstance(), (float)event.getPartialTick());
            event.setPitch(camera.getPitch());
            event.setYaw(camera.getYaw());
            event.setRoll(CameraController.cameraRoll);
        }
    }

}
