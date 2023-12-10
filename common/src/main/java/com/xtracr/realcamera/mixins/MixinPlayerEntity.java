package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.api.VirtualRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Inject(method = "getEquippedStack", at = @At("HEAD"), cancellable = true)
    private void realCamera$onGetEquippedStackHEAD(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cInfo) {
        if (VirtualRenderer.shouldDisableRender("slot_head") && slot == EquipmentSlot.HEAD ||
                VirtualRenderer.shouldDisableRender("slot_chest") && slot == EquipmentSlot.CHEST ||
                VirtualRenderer.shouldDisableRender("slot_legs") && slot == EquipmentSlot.LEGS ||
                VirtualRenderer.shouldDisableRender("slot_feet") && slot == EquipmentSlot.FEET) {
            cInfo.setReturnValue(ItemStack.EMPTY);
        }
    }
}
