package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RealCamera.MODID)
public class RealCameraNeoForge {
    private final ModContainer modContainer;

    public RealCameraNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        this.modContainer = modContainer;
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::onKeyRegister);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        RealCamera.initialize();

        NeoForge.EVENT_BUS.addListener(EventHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(EventHandler::onRenderLevelStage);

        if (ModList.get().isLoaded("cloth_config")) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, modListScreen) -> ConfigScreen.create(modListScreen));
        }
    }

    public void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event::register);
    }
}
