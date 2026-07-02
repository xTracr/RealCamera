package com.xtracr.realcamera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    @Final Minecraft minecraft;
    @Shadow
    @Final
    private Camera mainCamera;

    @WrapOperation(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
    private EntityHitResult realcamera$modifyEntityHitResult(Entity entity, Vec3 vec3, Vec3 vec32, AABB aABB, Predicate<Entity> predicate, double d, Operation<EntityHitResult> original) {
        if (!ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
            double interactionRange = Math.max(minecraft.player.blockInteractionRange(), minecraft.player.entityInteractionRange());
            Entity cameraEntity = minecraft.getCameraEntity();
            float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
            Pair<Vec3, Vec3> fromAndTo = RaycastUtil.getFromAndTo(cameraEntity, interactionRange * interactionRange, partialTicks);
            Vec3 newFrom = fromAndTo.getFirst();
            Vec3 newTo = fromAndTo.getSecond();
            double sqDistance = newTo.distanceToSqr(newFrom);
            AABB newBox = cameraEntity.getBoundingBox().expandTowards(newTo.subtract(newFrom)).inflate(1.0, 1.0, 1.0);
            CrosshairUtil.capturedEntityHitResult = ProjectileUtil.getEntityHitResult(entity, newFrom, newTo, newBox, predicate, sqDistance);
        } else {
            CrosshairUtil.capturedEntityHitResult = original.call(entity, vec3, vec32, aABB, predicate, d);
        }
        return CrosshairUtil.capturedEntityHitResult;
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"))
    private void realcamera$atCameraSetup(DeltaTracker deltaTracker, CallbackInfo ci) {
        final float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
        CompatibilityHelper.NEA_setDeltaTick(partialTicks);
        RealCameraCore.initialize(minecraft);
        if (RealCameraCore.isActive() && !ConfigFile.config().isClassic) {
            EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            dispatcher.prepare(minecraft.level, mainCamera,  minecraft.crosshairPickEntity);
            RealCameraCore.computeCamera(minecraft, partialTicks);
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareCullFrustum(Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
    private void realcamera$atPrepareCullFrustum(DeltaTracker deltaTracker, CallbackInfo ci, @Local(ordinal = 1) Matrix4f modelView) {
        if (RealCameraCore.isActive()) {
            modelView.rotateLocalZ(RealCameraCore.getRoll(0) * (float) (Math.PI / 180.0));
        }
    }
}
