package com.xtracr.realcamera;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

@Environment(EnvType.CLIENT)
public class RealCameraFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RealCamera.initialize();
        KeyBindings.register(KeyBindingHelper::registerKeyBinding);

        ClientTickEvents.END_CLIENT_TICK.register(KeyBindings::handle);
        WorldRenderEvents.START.register(EventHandler::onWorldRenderStart);
    }
}
