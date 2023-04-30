package com.xtracr.realcamera;

import com.xtracr.realcamera.command.DebugCommandForge;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.utils.CrosshairUtils;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {
    
    @SubscribeEvent
    public static void onKeyInput(Key event) {
        KeyBindings.handle(MinecraftClient.getInstance());
    }

    @SubscribeEvent
    public static void onCameraUpdate(ComputeCameraAngles event) {
        if (RealCameraCore.isActive()) {
            Camera camera = event.getCamera();
            RealCameraCore.updateCamera(camera, event.getRenderer().getClient(), (float)event.getPartialTick());
            event.setPitch(camera.getPitch());
            event.setYaw(camera.getYaw());
            event.setRoll(RealCameraCore.cameraRoll);
        }
    }

    @SubscribeEvent
    public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
        DebugCommandForge.INSTANCE.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onRenderWorldStage(RenderLevelStageEvent event) {
        if (RenderLevelStageEvent.Stage.AFTER_SKY.equals(event.getStage()) && ConfigFile.modConfig.isCrosshairDynamic()
                && RealCameraCore.isActive()) {
            CrosshairUtils.update(MinecraftClient.getInstance(), event.getCamera(),
                    event.getPoseStack().peek().getPositionMatrix(), event.getProjectionMatrix());
        }
    }

}
