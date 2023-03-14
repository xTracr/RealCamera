package com.xtracr.betterfpcam;

import com.xtracr.betterfpcam.config.ConfigController;
import com.xtracr.betterfpcam.event.EventHandler;

import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("betterfpcam")
public class BetterFPCam {

    public BetterFPCam() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::clientSetup);

        ModLoadingContext.get().registerConfig(Type.CLIENT, ConfigController.forgeConfigSpec);

    }
    
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {

        MinecraftForge.EVENT_BUS.addListener(EventHandler::onKeyInput);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onCameraSetup);

        ClientRegistry.registerKeyBinding(KeyController.toggleCamera);
        ClientRegistry.registerKeyBinding(KeyController.cameraUP);
        ClientRegistry.registerKeyBinding(KeyController.cameraDOWN);
        ClientRegistry.registerKeyBinding(KeyController.cameraIN);
        ClientRegistry.registerKeyBinding(KeyController.cameraOUT);
        ClientRegistry.registerKeyBinding(KeyController.centerUP);
        ClientRegistry.registerKeyBinding(KeyController.centerDOWN);
    }
    
}
