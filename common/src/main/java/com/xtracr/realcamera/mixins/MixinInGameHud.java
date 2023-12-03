package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.utils.CrosshairUtils;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Inject(method = "renderCrosshair(Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("HEAD"))
    private void realCamera$onRenderCrosshairHEAD(MatrixStack matrixStack, CallbackInfo cInfo) {
        if (ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
            matrixStack.push();
            CrosshairUtils.translateMatrices(matrixStack);
        }
    }

    @Inject(method = "renderCrosshair(Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("RETURN"))
    private void realCamera$onRenderCrosshairRETURN(MatrixStack matrixStack, CallbackInfo cInfo) {
        if (ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
            matrixStack.pop();
        }
    }
}
