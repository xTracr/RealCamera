package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.compat.DisableHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHookRenderer.class)
public abstract class MixinFishingHookRenderer {
    @Redirect(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean realcamera$atGetPlayerHandPos(CameraType cameraType, FishingHook fishingHook) {
        if (DisableHelper.RENDER_HANDS.disabled(fishingHook.getPlayerOwner())) return false;
        return cameraType.isFirstPerson();
    }
}
