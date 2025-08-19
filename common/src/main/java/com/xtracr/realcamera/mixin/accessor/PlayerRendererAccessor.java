package com.xtracr.realcamera.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerRenderer.class)
public interface PlayerRendererAccessor {
    @Invoker
    void invokeSetupRotations(PlayerRenderState renderState, PoseStack poseStack, float f, float deltaTick);

    @Invoker
    void invokeScale(PlayerRenderState renderState, PoseStack poseStack);
}
