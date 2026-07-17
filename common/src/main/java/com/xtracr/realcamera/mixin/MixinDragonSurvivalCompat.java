package com.xtracr.realcamera.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "by.dragonsurvivalteam.dragonsurvival.compat.Compat", remap = false)
public abstract class MixinDragonSurvivalCompat {
    @ModifyReturnValue(method = "displayNeck()Z", at = @At("RETURN"), remap = false, require = 0, expect = 1)
    private static boolean realcamera$displayDragonNeck(boolean original) {
        return original || RealCameraCore.isActive() || CompatibilityHelper.isRenderInScreen;
    }
}
