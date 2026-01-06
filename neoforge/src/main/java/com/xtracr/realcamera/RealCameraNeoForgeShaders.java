package com.xtracr.realcamera;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.xtracr.realcamera.render.RealCameraShaders;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;

public final class RealCameraNeoForgeShaders {
    private RealCameraNeoForgeShaders() {
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "realcamera_entity_solid").toString(), DefaultVertexFormat.NEW_ENTITY), RealCameraShaders::setEntitySolid);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "realcamera_entity_cutout").toString(), DefaultVertexFormat.NEW_ENTITY), RealCameraShaders::setEntityCutout);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "realcamera_entity_translucent").toString(), DefaultVertexFormat.NEW_ENTITY), RealCameraShaders::setEntityTranslucent);
        } catch (IOException e) {
            RealCamera.LOGGER.error("Failed to register RealCamera shaders", e);
        }
    }
}
