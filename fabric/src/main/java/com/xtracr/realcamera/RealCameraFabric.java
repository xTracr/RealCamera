package com.xtracr.realcamera;

import com.xtracr.realcamera.command.ClientCommand;
import com.xtracr.realcamera.util.VertexDataAnalyser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

@Environment(EnvType.CLIENT)
public class RealCameraFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RealCamera.setup();

        ClientTickEvents.END_CLIENT_TICK.register(KeyBindings::handle);
        ClientTickEvents.END_CLIENT_TICK.register(client -> VertexDataAnalyser.tick());
        WorldRenderEvents.START.register(EventHandler::onWorldRenderStart);
        ClientCommandRegistrationCallback.EVENT.register(new ClientCommand<FabricClientCommandSource>()::register);

        KeyBindingHelper.registerKeyBinding(KeyBindings.MODEL_VIEW_GUI);
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
