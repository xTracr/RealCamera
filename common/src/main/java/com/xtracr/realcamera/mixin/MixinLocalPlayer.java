package com.xtracr.realcamera.mixin;

import com.mojang.authlib.GameProfile;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
    private void realcamera$atGetRopePosHEAD(float delta, CallbackInfoReturnable<Vec3> cInfo) {
        if (RealCameraCore.isRendering()) cInfo.setReturnValue(super.getRopeHoldPosition(delta));
    }

    @Override
    public HitResult pick(double maxDistance, float tickDelta, boolean includeFluids) {
        if (!ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            RaycastUtil.update(this, maxDistance * maxDistance, tickDelta);
            return level().clip(RaycastUtil.getClipContext(ClipContext.Block.OUTLINE,
                    includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
        }
        return super.pick(maxDistance, tickDelta, includeFluids);
    }
}
