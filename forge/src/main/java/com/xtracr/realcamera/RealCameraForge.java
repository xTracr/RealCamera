package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
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
                new ConfigGuiFactory((mc, screen) -> ConfigScreen.create(screen)
            ));
        }
    }
    
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {

        RealCamera.setup();

        MinecraftForge.EVENT_BUS.addListener((KeyInputEvent keyEvent) -> 
            KeyBindings.handle(MinecraftClient.getInstance())
        );
        MinecraftForge.EVENT_BUS.addListener(EventHandler::onCameraSetup);
        
        ClientRegistry.registerKeyBinding(KeyBindings.toggleCamera);
        ClientRegistry.registerKeyBinding(KeyBindings.toggleAdjustMode);
        ClientRegistry.registerKeyBinding(KeyBindings.adjustUP);
        ClientRegistry.registerKeyBinding(KeyBindings.adjustDOWN);
        ClientRegistry.registerKeyBinding(KeyBindings.adjustFRONT);
        ClientRegistry.registerKeyBinding(KeyBindings.adjustBACK);
        ClientRegistry.registerKeyBinding(KeyBindings.adjustLEFT);
        ClientRegistry.registerKeyBinding(KeyBindings.adjustRIGHT);
    }
    
}
