package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigScreen;

import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
                new ConfigScreenFactory((client, parent) -> ConfigScreen.create(parent)
            ));
        }
    }
    
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {

        RealCamera.setup();

        MinecraftForge.EVENT_BUS.addListener(EventHandler::onKeyInput);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onCameraUpdate);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onClientCommandRegister);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onRenderWorldStage);
    }

    @SubscribeEvent
    public void onKeyRegister(RegisterKeyMappingsEvent event) {       
        event.register(KeyBindings.TOGGLE_PERSPECTIVE);
        event.register(KeyBindings.TOGGLE_ADJUST_MODE);
        event.register(KeyBindings.TOGGLE_CAMERA_MODE);
        event.register(KeyBindings.ADJUST_UP);
        event.register(KeyBindings.ADJUST_DOWN);
        event.register(KeyBindings.ADJUST_BACK);
        event.register(KeyBindings.ADJUST_FRONT);
        event.register(KeyBindings.ADJUST_LEFT);
        event.register(KeyBindings.ADJUST_RIGHT);
    }

    
}
