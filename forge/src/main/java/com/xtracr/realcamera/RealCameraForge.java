package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigScreen;

import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.common.MinecraftForge;
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

        if (ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenFactory.class, () -> 
                new ConfigScreenFactory((mc, screen) -> ConfigScreen.create(screen)
            ));
        }
    }
    
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {

        RealCamera.setup();

        MinecraftForge.EVENT_BUS.addListener((Key keyEvent) -> KeyBindings.handle());
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onCameraSetup);
    }

    @SubscribeEvent
    public void onKeyRegister(RegisterKeyMappingsEvent event) {       
        event.register(KeyBindings.toggleCamera);
        event.register(KeyBindings.cameraUP);
        event.register(KeyBindings.cameraDOWN);
        event.register(KeyBindings.cameraIN);
        event.register(KeyBindings.cameraOUT);
        event.register(KeyBindings.cameraLEFT);
        event.register(KeyBindings.cameraRIGHT);
        event.register(KeyBindings.centerUP);
        event.register(KeyBindings.centerDOWN);
    }

    
}
