package com.xtracr.realcamera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xtracr.realcamera.config.ConfigFile;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class RealCamera implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("realcamera");
    
    @Override
	public void onInitializeClient() {
		KeyController.register();
		//EventHandler.register();
		ConfigFile.setup();
	}
}
