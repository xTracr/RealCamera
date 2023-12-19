package com.xtracr.realcamera.mixin;

import com.mojang.authlib.GameProfile;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Redirect(method = "getLeashPos", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"))
    private boolean realCamera$returnFalse(Perspective perspective) {
        if (ConfigFile.modConfig.isRendering() && RealCameraCore.isActive()) return false;
        return perspective.isFirstPerson();
    }

    @Override
    public HitResult raycast(double maxDistance, float tickDelta, boolean includeFluids) {
        if (!ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
            RaycastUtil.update(this, maxDistance * maxDistance, tickDelta);
            return getWorld().raycast(RaycastUtil.getRaycastContext(RaycastContext.ShapeType.OUTLINE,
                    includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, this));
        }
        return super.raycast(maxDistance, tickDelta, includeFluids);
    }
}
