package com.xtracr.realcamera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
    public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @WrapOperation(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
    private static @Nullable EntityHitResult realcamera$wrapGetEntityHitResult(Entity except, Vec3 from, Vec3 _to, AABB box, Predicate<Entity> matching, double maxValue, Operation<EntityHitResult> original, Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float partialTicks) {
        if (!ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
            double interactionRange = Math.max(blockInteractionRange, entityInteractionRange);
            Pair<Vec3, Vec3> fromAndTo = RaycastUtil.getFromAndTo(cameraEntity, interactionRange * interactionRange, partialTicks);
            Vec3 newFrom = fromAndTo.getFirst();
            Vec3 newTo = fromAndTo.getSecond();
            double sqDistance = newTo.distanceToSqr(newFrom);
            AABB newBox = cameraEntity.getBoundingBox().expandTowards(newTo.subtract(newFrom)).inflate(1.0, 1.0, 1.0);
            CrosshairUtil.capturedEntityHitResult = ProjectileUtil.getEntityHitResult(except, newFrom, newTo, newBox, matching, sqDistance);
            return CrosshairUtil.capturedEntityHitResult;
        }
        return original.call(except, from, _to, box, matching, maxValue);
    }

    @Inject(method = "getRopeHoldPosition", at = @At("HEAD"), cancellable = true)
    private void realcamera$atGetRopePosHEAD(float partialTickTime, CallbackInfoReturnable<Vec3> cir) {
        if (DisableHelper.RENDER_HANDS.disabled(this)) cir.setReturnValue(super.getRopeHoldPosition(partialTickTime));
    }

    @SuppressWarnings("resource")
    @Override
    public @NonNull HitResult pick(double maxDistance, float partialTicks, boolean includeFluids) {
        if (!ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
            Pair<Vec3, Vec3> fromAndTo = RaycastUtil.getFromAndTo(this, maxDistance * maxDistance, partialTicks);
            return level().clip(new ClipContext(fromAndTo.getFirst(), fromAndTo.getSecond(), ClipContext.Block.OUTLINE, includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
        }
        return super.pick(maxDistance, partialTicks, includeFluids);
    }
}
