package com.xtracr.realcamera.mixin.accessor;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelFeatureRenderer.Storage.class)
public interface ModelFeatureRenderer$StorageAccessor {
    @Accessor
    Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> getSolidModelSubmits();

    @Accessor
    List<SubmitNodeStorage.TranslucentModelSubmit<?>> getTranslucentModelSubmits();
}
