package com.xtracr.realcamera;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.util.Map;

@Mod(RealCamera.MODID)
public class RealCameraNeoForge implements RealCamera {
    private final ModContainer modContainer;
    private final Map<String, String> modIdMap = Map.of("cloth-config", "cloth_config");

    public RealCameraNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        this.modContainer = modContainer;
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::onKeyRegister);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        initialize();

        EventHandler.addListeners();

//        if (isModLoaded("cloth-config")) {
//            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, modListScreen) -> ConfigScreen.create(modListScreen));
//        }
    }

    public void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.registerCategory(KeyMappings.GENERAL);
        KeyMappings.register(event::register);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modIdMap.getOrDefault(modId, modId));
    }
}
