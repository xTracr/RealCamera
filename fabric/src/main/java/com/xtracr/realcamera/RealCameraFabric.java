package com.xtracr.realcamera;

import com.xtracr.realcamera.renderer.gui.GuiCulledModelsRenderer;
import com.xtracr.realcamera.renderer.gui.GuiFlattenedModelsRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

@Environment(EnvType.CLIENT)
public final class RealCameraFabric implements ClientModInitializer, RealCamera {
    @Override
    public void onInitializeClient() {
        initialize();

        KeyMapping.Category.register(KeyMappings.GENERAL.id());
        KeyMappings.register(KeyMappingHelper::registerKeyMapping);
        registerPictureInPictureRenderers();

        ClientTickEvents.END_CLIENT_TICK.register(KeyMappings::handle);
    }

    private void registerPictureInPictureRenderers() {
        PictureInPictureRendererRegistry.register(ctx -> new GuiCulledModelsRenderer(ctx.bufferSource()));
        PictureInPictureRendererRegistry.register(ctx -> new GuiFlattenedModelsRenderer(ctx.bufferSource()));
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
