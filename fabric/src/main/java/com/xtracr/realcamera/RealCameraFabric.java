package com.xtracr.realcamera;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class RealCameraFabric implements ClientModInitializer, RealCamera {
    @Override
    public void onInitializeClient() {
        initialize();
        // TODO: register category
        KeyMappings.register(KeyMappingHelper::registerKeyMapping);

        ClientTickEvents.END_CLIENT_TICK.register(KeyMappings::handle);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
