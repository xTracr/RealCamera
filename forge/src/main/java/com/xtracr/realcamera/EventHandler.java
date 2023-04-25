package com.xtracr.realcamera;

import com.xtracr.realcamera.camera.CameraController;
import com.xtracr.realcamera.command.ClientCommandForge;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
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
    public static void onCameraSetup(CameraSetup event) {
        if (CameraController.isActive()) {
            Camera camera = event.getCamera();
            CameraController.setCameraOffset(camera, event.getRenderer().getClient(), (float)event.getPartialTicks());
            event.setPitch(camera.getPitch());
            event.setYaw(camera.getYaw());
            event.setRoll(CameraController.cameraRoll);
        }
    }

    @SubscribeEvent
    public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
        ClientCommandForge.INSTANCE.register(event.getDispatcher());
    }

}
