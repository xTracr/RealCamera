package com.xtracr.realcamera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xtracr.realcamera.compat.DisableHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHookRenderer.class)
public abstract class MixinFishingHookRenderer {
    @WrapOperation(method = "getPlayerHandPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean realcamera$redirectIsFirstPerson(CameraType instance, Operation<Boolean> original, Player player) {
        if (DisableHelper.RENDER_HANDS.disabled(player)) return false;
        return original.call(instance);
    }
}
