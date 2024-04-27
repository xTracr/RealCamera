package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinItem {
    @Inject(method = "raycast", at = @At("HEAD"), cancellable = true)
    private static void realcamera$coverRaycast(World world, PlayerEntity player, RaycastContext.FluidHandling fluidHandling,
                                                CallbackInfoReturnable<BlockHitResult> cInfo) {
        if (!ConfigFile.modConfig.dynamicCrosshair() && RealCameraCore.isActive()) {
            RaycastUtil.update(player, 25.0d, 1.0f);
            cInfo.setReturnValue(world.raycast(RaycastUtil.getRaycastContext(RaycastContext.ShapeType.OUTLINE, fluidHandling, player)));
        }
    }
}
