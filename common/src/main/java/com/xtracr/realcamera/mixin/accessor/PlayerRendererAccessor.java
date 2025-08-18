package com.xtracr.realcamera.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerRenderer.class)
public interface PlayerRendererAccessor {
    @Invoker
    void invokeSetModelProperties(AbstractClientPlayer abstractClientPlayer);

    @Invoker
    void invokeSetupRotations(AbstractClientPlayer livingEntity, PoseStack poseStack, float f, float g, float deltaTick, float i);

    @Invoker
    void invokeScale(AbstractClientPlayer livingEntity, PoseStack poseStack, float deltaTick);
}
