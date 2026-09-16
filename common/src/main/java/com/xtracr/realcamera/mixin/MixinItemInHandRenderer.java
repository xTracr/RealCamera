package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.compat.DisableHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void realcamera$cancelRender(CallbackInfo ci) {
        if (DisableHelper.RENDER_HANDS.disabled(Minecraft.getInstance().getCameraEntity())) ci.cancel();
    }
}
