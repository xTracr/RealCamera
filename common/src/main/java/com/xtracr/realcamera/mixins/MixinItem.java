package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.utils.RaycastUtils;
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
    private static void coverRaycast(World world, PlayerEntity player, RaycastContext.FluidHandling fluidHandling,
            CallbackInfoReturnable<BlockHitResult> cInfo) {
        if (!ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
            RaycastUtils.update(player, 25.0D, 1.0F);
            cInfo.setReturnValue(world.raycast(RaycastUtils.getRaycastContext(RaycastContext.ShapeType.OUTLINE, fluidHandling, player)));
        }
    }
}
