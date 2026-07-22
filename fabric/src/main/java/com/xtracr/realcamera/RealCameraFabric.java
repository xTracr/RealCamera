package com.xtracr.realcamera;

import com.xtracr.realcamera.renderer.gui.GuiCulledModelsRenderer;
import com.xtracr.realcamera.renderer.gui.GuiFlattenedModelsRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

@Environment(EnvType.CLIENT)
public final class RealCameraFabric implements ClientModInitializer, RealCamera {
    @Override
    public void onInitializeClient() {
        initialize();

        KeyMapping.Category.register(KeyMappings.GENERAL.id());
        KeyMappings.register(KeyBindingHelper::registerKeyBinding);
        registerPictureInPictureRenderers();

        ClientTickEvents.END_CLIENT_TICK.register(KeyMappings::handle);
    }

    private void registerPictureInPictureRenderers() {
        SpecialGuiElementRegistry.register(ctx -> new GuiCulledModelsRenderer(ctx.vertexConsumers()));
        SpecialGuiElementRegistry.register(ctx -> new GuiFlattenedModelsRenderer(ctx.vertexConsumers()));
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
