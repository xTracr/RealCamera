package com.xtracr.realcamera.mixin.accessor;

import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.class)
public interface FeatureRenderDispatcherAccessor {
    @Accessor
    CustomFeatureRenderer getCustomFeatureRenderer();
}
