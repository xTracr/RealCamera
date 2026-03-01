package com.xtracr.realcamera.mixin.accessor;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ModelFeatureRenderer.Storage.class)
public interface ModelFeatureRenderer$StorageAccessor {
    @Accessor
    List<SubmitNodeStorage.TranslucentModelSubmit<?>> getTranslucentModelSubmits();
}
