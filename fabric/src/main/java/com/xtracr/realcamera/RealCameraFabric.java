package com.xtracr.realcamera;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

@Environment(EnvType.CLIENT)
public class RealCameraFabric implements ClientModInitializer {

    @Override
	public void onInitializeClient() {
		RealCamera.setup();
		this.registerKeyBindings();
	}

	private void registerKeyBindings() {
        
        KeyBindingHelper.registerKeyBinding(KeyBindings.toggleCamera);
        KeyBindingHelper.registerKeyBinding(KeyBindings.cameraUP);
        KeyBindingHelper.registerKeyBinding(KeyBindings.cameraDOWN);
        KeyBindingHelper.registerKeyBinding(KeyBindings.cameraIN);
        KeyBindingHelper.registerKeyBinding(KeyBindings.cameraOUT);
        KeyBindingHelper.registerKeyBinding(KeyBindings.cameraLEFT);
        KeyBindingHelper.registerKeyBinding(KeyBindings.cameraRIGHT);
        KeyBindingHelper.registerKeyBinding(KeyBindings.centerUP);
        KeyBindingHelper.registerKeyBinding(KeyBindings.centerDOWN);

        ClientTickEvents.END_CLIENT_TICK.register(client -> KeyBindings.handle());
	}
}
