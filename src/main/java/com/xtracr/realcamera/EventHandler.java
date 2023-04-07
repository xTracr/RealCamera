package com.xtracr.realcamera;

import com.xtracr.realcamera.camera.CameraController;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {
    
    @SubscribeEvent
    public static void onKeyInput(Key event) {
        KeyController.keyHandler();
    }

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {       
        event.register(KeyController.toggleCamera);
        event.register(KeyController.cameraUP);
        event.register(KeyController.cameraDOWN);
        event.register(KeyController.cameraIN);
        event.register(KeyController.cameraOUT);
        event.register(KeyController.cameraLEFT);
        event.register(KeyController.cameraRIGHT);
        event.register(KeyController.centerUP);
        event.register(KeyController.centerDOWN);
    }

    @SubscribeEvent
    public static void onCameraSetup(ComputeCameraAngles event) {
        if (CameraController.isActive() && Minecraft.getInstance().player != null) {
            CameraController.setCameraOffset(event, Minecraft.getInstance(), event.getPartialTick());
        }
    }

}
