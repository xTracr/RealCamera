package com.xtracr.realcamera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    @Final private Minecraft minecraft;
    @Shadow
    @Final private Camera mainCamera;

    @WrapOperation(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult realcamera$wrapRaycast(LocalPlayer instance, float partialTicks, Entity cameraEntity, Operation<HitResult> original) {
        if (!ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            Vec3 startVec = RaycastUtil.getStartVec();
            Vec3 endVec = RaycastUtil.getEndVec();
            double sqDistance = (minecraft.hitResult != null ? minecraft.hitResult.getLocation().distanceToSqr(startVec) : endVec.distanceToSqr(startVec));
            double interactionRange = Math.max(instance.blockInteractionRange(), instance.entityInteractionRange());
            AABB box = cameraEntity.getBoundingBox().expandTowards(cameraEntity.getViewVector(partialTicks).scale(interactionRange)).inflate(1.0, 1.0, 1.0);
            return CrosshairUtil.capturedEntityHitResult = ProjectileUtil.getEntityHitResult(cameraEntity, startVec, endVec, box, entity -> !entity.isSpectator() && entity.isPickable(), sqDistance);
        }
        return original.call(instance, partialTicks, cameraEntity);
    }

    @Inject(method = "updateCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V"))
    private void realcamera$atCameraSetup(DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "cameraDeltaPartialTicks") float cameraDeltaPartialTicks) {
        CompatibilityHelper.NEA_setDeltaTick(cameraDeltaPartialTicks);
        RealCameraCore.initialize(minecraft);
        if (RealCameraCore.isActive() && !ConfigFile.config().isClassic()) {
            minecraft.getEntityRenderDispatcher().prepare(mainCamera,  minecraft.crosshairPickEntity);
            RealCameraCore.computeCamera(minecraft, cameraDeltaPartialTicks);
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;extractCamera(F)V"))
    private void realcamera$atExtractCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
        CompatibilityHelper.forceSetCameraPos(mainCamera);
    }
}
