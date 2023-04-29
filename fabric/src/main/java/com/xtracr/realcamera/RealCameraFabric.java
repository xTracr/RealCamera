package com.xtracr.realcamera;

import com.xtracr.realcamera.command.DebugCommandFabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

@Environment(EnvType.CLIENT)
public class RealCameraFabric implements ClientModInitializer {

    @Override
	public void onInitializeClient() {
		RealCamera.setup();
		this.registerKeyBindings();
        ClientCommandRegistrationCallback.EVENT.register(DebugCommandFabric.INSTANCE::register);
	}

	private void registerKeyBindings() {
        
        KeyBindingHelper.registerKeyBinding(KeyBindings.toggleCamera);
        KeyBindingHelper.registerKeyBinding(KeyBindings.toggleAdjustMode);
        KeyBindingHelper.registerKeyBinding(KeyBindings.adjustUP);
        KeyBindingHelper.registerKeyBinding(KeyBindings.adjustDOWN);
        KeyBindingHelper.registerKeyBinding(KeyBindings.adjustBACK);
        KeyBindingHelper.registerKeyBinding(KeyBindings.adjustFRONT);
        KeyBindingHelper.registerKeyBinding(KeyBindings.adjustLEFT);
        KeyBindingHelper.registerKeyBinding(KeyBindings.adjustRIGHT);

        ClientTickEvents.END_CLIENT_TICK.register(KeyBindings::handle);
	}
}
