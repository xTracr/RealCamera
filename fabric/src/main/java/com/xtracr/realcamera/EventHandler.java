package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.utils.CrosshairUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

public class EventHandler {

    public static void onWorldRenderStart(WorldRenderContext context) {
        RealCameraCore.isRenderingWorld = true;
        if (ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
            CrosshairUtils.update(MinecraftClient.getInstance(), context.camera(),
                    context.matrixStack().peek().getPositionMatrix(), context.projectionMatrix());
        }
    }
}
