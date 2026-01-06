package com.xtracr.realcamera;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.xtracr.realcamera.render.RealCameraShaders;

@Environment(EnvType.CLIENT)
public class RealCameraFabric implements ClientModInitializer, RealCamera {
    @Override
    public void onInitializeClient() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "realcamera_entity_solid"), DefaultVertexFormat.NEW_ENTITY, RealCameraShaders::setEntitySolid);
            context.register(ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "realcamera_entity_cutout"), DefaultVertexFormat.NEW_ENTITY, RealCameraShaders::setEntityCutout);
            context.register(ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "realcamera_entity_translucent"), DefaultVertexFormat.NEW_ENTITY, RealCameraShaders::setEntityTranslucent);
        });
        initialize();
        KeyMappings.register(KeyBindingHelper::registerKeyBinding);

        ClientTickEvents.END_CLIENT_TICK.register(KeyMappings::handle);
        WorldRenderEvents.START.register(EventHandler::onWorldRenderStart);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
