package com.xtracr.realcamera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xtracr.realcamera.config.ModConfig;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("realcamera")
public class RealCamera {

    public static final Logger LOGGER = LoggerFactory.getLogger("realcamera");
    
    public RealCamera() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::clientSetup);

        ModLoadingContext.get().registerConfig(Type.CLIENT, ModConfig.forgeConfigSpec);

    }
    
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {

        MinecraftForge.EVENT_BUS.addListener(EventHandler::onKeyInput);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onCameraSetup);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onKeyRegister);
    }
    
}
