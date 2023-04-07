package com.xtracr.realcamera.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class RealCameraMenu implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return new ConfigScreen();
    }
}
