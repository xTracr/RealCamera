package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;update(Lnet/minecraft/client/DeltaTracker;)V"))
    private void realcamera$atCameraUpdate(DeltaTracker deltaTracker, CallbackInfo ci) {
        Entity entity = minecraft.getCameraEntity();
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(entity == null || !minecraft.level.tickRateManager().isEntityFrozen(entity));
        CompatibilityHelper.NEA_setDeltaTick(partialTicks);
        RealCameraCore.initialize(minecraft);
        if (RealCameraCore.isActive() && !ConfigFile.config().isClassic) {
            minecraft.getEntityRenderDispatcher().prepare(mainCamera, minecraft.crosshairPickEntity);
            RealCameraCore.computeCamera(minecraft, partialTicks);
        }
    }
}
