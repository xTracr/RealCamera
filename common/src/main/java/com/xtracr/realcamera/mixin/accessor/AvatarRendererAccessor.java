package com.xtracr.realcamera.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AvatarRenderer.class)
public interface AvatarRendererAccessor {
    @Invoker
    void invokeSetupRotations(AvatarRenderState renderState, PoseStack poseStack, float f, float partialTicks);

    @Invoker
    void invokeScale(AvatarRenderState renderState, PoseStack poseStack);
}
