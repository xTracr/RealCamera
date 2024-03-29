package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RealCamera.MODID)
public class RealCameraForge {
    public RealCameraForge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::clientSetup);
        eventBus.addListener(this::onKeyRegister);
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
        RealCamera.initialize();

        MinecraftForge.EVENT_BUS.addListener(EventHandler::onKeyInput);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, EventHandler::onCameraUpdate);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onRenderWorldStage);

        if (ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenFactory.class,
                    () -> new ConfigScreenFactory((client, parent) -> ConfigScreen.create(parent)));
        }
    }

    @SubscribeEvent
    public void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event::register);
    }
}
