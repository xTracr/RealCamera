package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {
    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void realcamera$atRenderCrosshairHEAD(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            guiGraphics.pose().pushPose();
            CrosshairUtil.translateMatrices(guiGraphics.pose());
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void realcamera$atRenderCrosshairRETURN(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            guiGraphics.pose().popPose();
        }
    }
}
