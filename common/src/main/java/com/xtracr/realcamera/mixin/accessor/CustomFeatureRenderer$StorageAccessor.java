package com.xtracr.realcamera.mixin.accessor;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(CustomFeatureRenderer.Storage.class)
public interface CustomFeatureRenderer$StorageAccessor {
    @Accessor
    Map<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> getCustomGeometrySubmits();
}
