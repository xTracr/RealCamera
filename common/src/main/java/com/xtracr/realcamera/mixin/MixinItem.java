package com.xtracr.realcamera.mixin;

import com.mojang.datafixers.util.Pair;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinItem {
    @Inject(method = "getPlayerPOVHitResult", at = @At("HEAD"), cancellable = true)
    private static void realcamera$coverHitResult(Level level, Player player, ClipContext.Fluid fluid, CallbackInfoReturnable<BlockHitResult> cir) {
        if (!ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
            Pair<Vec3, Vec3> fromAndTo = RaycastUtil.getFromAndTo(player, 25.0d, 1.0f);
            cir.setReturnValue(level.clip(new ClipContext(fromAndTo.getFirst(), fromAndTo.getSecond(), ClipContext.Block.OUTLINE, fluid, player)));
        }
    }
}
