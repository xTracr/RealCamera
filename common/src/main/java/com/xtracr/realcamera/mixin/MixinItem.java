package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinItem {
    @Inject(method = "getPlayerPOVHitResult", at = @At("HEAD"), cancellable = true)
    private static void realcamera$coverRaycast(Level world, Player player, ClipContext.Fluid fluidHandling,
                                                CallbackInfoReturnable<BlockHitResult> cInfo) {
        if (!ConfigFile.config().dynamicCrosshair() && RealCameraCore.isActive()) {
            RaycastUtil.update(player, 25.0d, 1.0f);
            cInfo.setReturnValue(world.clip(RaycastUtil.getRaycastContext(ClipContext.Block.OUTLINE, fluidHandling, player)));
        }
    }
}
