package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.utils.CrosshairUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"))
    private void onRenderCrosshairHEAD(DrawContext context, CallbackInfo cInfo) {
        if (ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
            context.getMatrices().push();
            CrosshairUtils.translateMatrices(context.getMatrices());
        }
    }

    @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("RETURN"))
    private void onRenderCrosshairRETURN(DrawContext context, CallbackInfo cInfo) {
        if (ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
            context.getMatrices().pop();
        }
    }
}
