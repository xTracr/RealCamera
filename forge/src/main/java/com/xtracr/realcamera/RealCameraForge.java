package com.xtracr.realcamera;

import com.xtracr.realcamera.compat.EpicFightCompat;
import com.xtracr.realcamera.config.ConfigScreen;

import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory;
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

        if (ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigGuiFactory.class, () -> 
                new ConfigGuiFactory((client, parent) -> ConfigScreen.create(parent)
            ));
        }
    }
    
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {

        RealCamera.setup();
        if (ModList.get().isLoaded("epicfight")) EpicFightCompat.register();

        MinecraftForge.EVENT_BUS.addListener(EventHandler::onKeyInput);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onCameraUpdate);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onClientCommandRegister);
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onRenderWorldStage);
        
        ClientRegistry.registerKeyBinding(KeyBindings.TOGGLE_PERSPECTIVE);
        ClientRegistry.registerKeyBinding(KeyBindings.TOGGLE_ADJUST_MODE);
        ClientRegistry.registerKeyBinding(KeyBindings.TOGGLE_CAMERA_MODE);
        ClientRegistry.registerKeyBinding(KeyBindings.ADJUST_UP);
        ClientRegistry.registerKeyBinding(KeyBindings.ADJUST_DOWN);
        ClientRegistry.registerKeyBinding(KeyBindings.ADJUST_FRONT);
        ClientRegistry.registerKeyBinding(KeyBindings.ADJUST_BACK);
        ClientRegistry.registerKeyBinding(KeyBindings.ADJUST_LEFT);
        ClientRegistry.registerKeyBinding(KeyBindings.ADJUST_RIGHT);
    }
    
}
