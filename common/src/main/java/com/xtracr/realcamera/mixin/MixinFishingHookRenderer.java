package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.compat.DisableHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHookRenderer.class)
public abstract class MixinFishingHookRenderer {
    @Redirect(method = "getPlayerHandPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean realcamera$atGetPlayerHandPos(CameraType cameraType, Player player) {
        if (DisableHelper.RENDER_HANDS.disabled(player)) return false;
        return cameraType.isFirstPerson();
    }
}
