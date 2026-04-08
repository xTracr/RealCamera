package com.xtracr.realcamera.mixin;

import com.mojang.authlib.GameProfile;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
    @Shadow
    @Final
    protected Minecraft minecraft;

    public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "getRopeHoldPosition", at = @At("HEAD"), cancellable = true)
    private void realcamera$atGetRopePosHEAD(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        if (DisableHelper.RENDER_HANDS.disabled(this)) cir.setReturnValue(super.getRopeHoldPosition(partialTicks));
    }

    @Inject(method = "raycastHitResult", at = @At("HEAD"), cancellable = true)
    private void realcamera$atRaycastHitResultHEAD(float partialTicks, Entity cameraEntity, CallbackInfoReturnable<HitResult> cir) {
        if (!ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            Vec3 startVec = RaycastUtil.getStartVec();
            Vec3 endVec = RaycastUtil.getEndVec();
            double sqDistance = (minecraft.hitResult != null ? minecraft.hitResult.getLocation().distanceToSqr(startVec) : endVec.distanceToSqr(startVec));
            double interactionRange = Math.max(blockInteractionRange(), entityInteractionRange());
            AABB box = cameraEntity.getBoundingBox().expandTowards(cameraEntity.getViewVector(partialTicks).scale(interactionRange)).inflate(1.0, 1.0, 1.0);
            CrosshairUtil.capturedEntityHitResult = ProjectileUtil.getEntityHitResult(cameraEntity, startVec, endVec, box, entity -> !entity.isSpectator() && entity.isPickable(), sqDistance);
            cir.setReturnValue(CrosshairUtil.capturedEntityHitResult);
        }
    }

    @Override
    public @NonNull HitResult pick(double maxDistance, float partialTicks, boolean includeFluids) {
        if (!ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            RaycastUtil.update(this, maxDistance * maxDistance, partialTicks);
            return level().clip(RaycastUtil.getClipContext(ClipContext.Block.OUTLINE,
                    includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
        }
        return super.pick(maxDistance, partialTicks, includeFluids);
    }
}
