package com.xtracr.realcamera.mixin.accessor;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelPartFeatureRenderer.Storage.class)
public interface ModelPartFeatureRenderer$StorageAccessor {
    @Accessor
    Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> getModelPartSubmits();
}
