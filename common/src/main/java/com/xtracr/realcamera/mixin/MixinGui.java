package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {
    @Inject(method = "extractCrosshair", at = @At("HEAD"))
    private void realcamera$atRenderCrosshairHEAD(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
            graphics.pose().pushMatrix();
            CrosshairUtil.translateMatrices(graphics.pose());
        }
    }

    @Inject(method = "extractCrosshair", at = @At("RETURN"))
    private void realcamera$atRenderCrosshairRETURN(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
            graphics.pose().popMatrix();
        }
    }
}
