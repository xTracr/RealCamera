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
        RealCamera.setup();

        ClientTickEvents.END_CLIENT_TICK.register(KeyBindings::handle);
        WorldRenderEvents.START.register(EventHandler::onWorldRenderStart);
        WorldRenderEvents.END.register(context -> RealCameraCore.isRenderingWorld = false);

        KeyBindingHelper.registerKeyBinding(KeyBindings.TOGGLE_PERSPECTIVE);
        KeyBindingHelper.registerKeyBinding(KeyBindings.TOGGLE_ADJUST_MODE);
        KeyBindingHelper.registerKeyBinding(KeyBindings.TOGGLE_CAMERA_MODE);
        KeyBindingHelper.registerKeyBinding(KeyBindings.ADJUST_UP);
        KeyBindingHelper.registerKeyBinding(KeyBindings.ADJUST_DOWN);
        KeyBindingHelper.registerKeyBinding(KeyBindings.ADJUST_BACK);
        KeyBindingHelper.registerKeyBinding(KeyBindings.ADJUST_FRONT);
        KeyBindingHelper.registerKeyBinding(KeyBindings.ADJUST_LEFT);
        KeyBindingHelper.registerKeyBinding(KeyBindings.ADJUST_RIGHT);
    }
}
