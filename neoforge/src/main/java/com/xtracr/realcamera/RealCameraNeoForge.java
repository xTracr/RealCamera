package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigScreen;
import com.xtracr.realcamera.renderer.gui.GuiCulledModelsRenderer;
import com.xtracr.realcamera.renderer.gui.GuiFlattenedModelsRenderer;
import com.xtracr.realcamera.renderer.state.gui.GuiCulledModelsRenderState;
import com.xtracr.realcamera.renderer.state.gui.GuiFlattenedModelsRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.Map;

@Mod(value = RealCamera.MODID, dist = Dist.CLIENT)
public class RealCameraNeoForge implements RealCamera {
    private final ModContainer modContainer;
    private final Map<String, String> modIdMap = Map.of("cloth-config", "cloth_config");

    public RealCameraNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        this.modContainer = modContainer;
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::onKeyRegister);
        modEventBus.addListener(this::onPictureInPictureRenderersRegister);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        initialize();

        EventHandler.addListeners();

        if (isModLoaded("cloth-config")) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (_, modListScreen) -> ConfigScreen.create(modListScreen));
        }
    }

    private void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.registerCategory(KeyMappings.GENERAL);
        KeyMappings.register(event::register);
    }

    private void onPictureInPictureRenderersRegister(RegisterPictureInPictureRenderersEvent event) {
        event.register(GuiCulledModelsRenderState.class, GuiCulledModelsRenderer::new);
        event.register(GuiFlattenedModelsRenderState.class, GuiFlattenedModelsRenderer::new);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modIdMap.getOrDefault(modId, modId));
    }
}
