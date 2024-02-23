package com.xtracr.realcamera;

import com.xtracr.realcamera.command.ClientCommand;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler {
    @SubscribeEvent
    public static void onKeyInput(Key event) {
        KeyBindings.handle(MinecraftClient.getInstance());
    }

    @SubscribeEvent
    public static void onCameraUpdate(ViewportEvent.ComputeCameraAngles event) {
        if (RealCameraCore.isActive()) {
            event.setRoll(RealCameraCore.getRoll(event.getRoll()));
        }
    }

    @SubscribeEvent
    public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
        new ClientCommand<ServerCommandSource>().register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onRenderWorldStage(RenderLevelStageEvent event) {
        if (RenderLevelStageEvent.Stage.AFTER_SKY.equals(event.getStage())) {
            if (ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
                CrosshairUtil.update(MinecraftClient.getInstance(), event.getCamera(),
                        event.getPoseStack().peek().getPositionMatrix(), event.getProjectionMatrix());
            }
        }
    }
}
