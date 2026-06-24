package com.xtracr.realcamera.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
    public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "getRopeHoldPosition", at = @At("HEAD"), cancellable = true)
    private void realcamera$atGetRopePosHEAD(float f, CallbackInfoReturnable<Vec3> cir) {
        if (DisableHelper.RENDER_HANDS.disabled(this)) cir.setReturnValue(super.getRopeHoldPosition(f));
    }

    @SuppressWarnings("resource")
    @Override
    public @NotNull HitResult pick(double maxDistance, float partialTicks, boolean includeFluids) {
        if (!ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
            Pair<Vec3, Vec3> fromAndTo = RaycastUtil.getFromAndTo(this, maxDistance * maxDistance, partialTicks);
            return level().clip(new ClipContext(fromAndTo.getFirst(), fromAndTo.getSecond(), ClipContext.Block.OUTLINE, includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
        }
        return super.pick(maxDistance, partialTicks, includeFluids);
    }
}
