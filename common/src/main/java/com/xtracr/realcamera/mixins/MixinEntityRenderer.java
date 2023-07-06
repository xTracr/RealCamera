package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void onShouldRenderHEAD(T entity, Frustum frustum, double x, double y, double z,
            CallbackInfoReturnable<Boolean> cInfo) {
        if (ConfigFile.modConfig.isRendering() && RealCameraCore.isActive() && entity instanceof ClientPlayerEntity) {
            cInfo.setReturnValue(true);
        }
    }
}
