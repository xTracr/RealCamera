package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

public class EventHandler {
    public static void onWorldRenderStart(WorldRenderContext context) {
        if (ConfigFile.modConfig.dynamicCrosshair() && RealCameraCore.isActive()) {
            CrosshairUtil.update(MinecraftClient.getInstance(), context.camera(),
                    context.matrixStack().peek().getPositionMatrix(), context.projectionMatrix());
        }
    }
}
