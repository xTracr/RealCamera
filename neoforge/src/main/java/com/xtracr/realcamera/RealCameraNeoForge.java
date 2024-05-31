package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigScreen;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RealCamera.MODID)
public class RealCameraNeoForge {
    public RealCameraNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::onKeyRegister);
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
        RealCamera.initialize();

        NeoForge.EVENT_BUS.addListener(EventHandler::onKeyInput);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EventHandler::onCameraUpdate);
        NeoForge.EVENT_BUS.addListener(EventHandler::onRenderWorldStage);

        if (ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                    () -> (client, parent) -> ConfigScreen.create(parent));
        }
    }

    @SubscribeEvent
    public void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event::register);
    }
}
