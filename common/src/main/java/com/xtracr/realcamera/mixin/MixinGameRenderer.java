package com.xtracr.realcamera.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    @Final private Minecraft minecraft;
    @Shadow
    @Final private Camera mainCamera;

    @ModifyVariable(method = "pick", at = @At("STORE"), name = "entityHitResult")
    private EntityHitResult realcamera$modifyEntityHitResult(EntityHitResult entityHitResult, float a) {
        CrosshairUtil.capturedEntityHitResult = entityHitResult;
        if (!ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            Vec3 startVec = RaycastUtil.getStartVec();
            Vec3 endVec = RaycastUtil.getEndVec();
            double sqDistance = (minecraft.hitResult != null ? minecraft.hitResult.getLocation().distanceToSqr(startVec) : endVec.distanceToSqr(startVec));
            Entity cameraEntity = minecraft.getCameraEntity();
            double interactionRange = Math.max(minecraft.player.blockInteractionRange(), minecraft.player.entityInteractionRange());
            AABB box = cameraEntity.getBoundingBox().expandTowards(cameraEntity.getViewVector(a).scale(interactionRange)).inflate(1.0, 1.0, 1.0);
            CrosshairUtil.capturedEntityHitResult = ProjectileUtil.getEntityHitResult(cameraEntity, startVec, endVec, box, entity -> !entity.isSpectator() && entity.isPickable(), sqDistance);
        }
        return CrosshairUtil.capturedEntityHitResult;
    }

    @Inject(method = "updateCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V"))
    private void realcamera$atCameraSetup(DeltaTracker deltaTracker, CallbackInfo ci) {
        final float deltaTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        CompatibilityHelper.NEA_setDeltaTick(deltaTick);
        RealCameraCore.initialize(minecraft);
        if (RealCameraCore.isActive() && !ConfigFile.config().isClassic()) {
            EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            dispatcher.prepare(mainCamera,  minecraft.crosshairPickEntity);
            RealCameraCore.computeCamera(minecraft, deltaTick);
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;"))
    private void realcamera$atSetupFog(DeltaTracker deltaTracker, CallbackInfo ci, @Local(ordinal = 1) Matrix4f modelView) {
        CompatibilityHelper.forceSetCameraPos(mainCamera);
    }
}
