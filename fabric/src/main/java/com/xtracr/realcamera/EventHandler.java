package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;

public class EventHandler {
    public static void onWorldRenderStart(WorldRenderContext context) {
        if (ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            CrosshairUtil.update(Minecraft.getInstance(), context.camera(), context.matrixStack().last().pose(), context.projectionMatrix());
        }
    }
}
