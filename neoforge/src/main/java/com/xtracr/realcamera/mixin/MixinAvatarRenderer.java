package com.xtracr.realcamera.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.compat.DragonSurvivalCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.renderer.RoutingSubmitCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {
    @WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    private void realcamera$filterDragonGeometry(
            AvatarRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector output,
            CameraRenderState cameraRenderState,
            Operation<Void> original) {
        BiFunction<RenderType, CustomGeometryRenderer, CustomGeometryRenderer> filter = realcamera$dragonFilter(renderState, cameraRenderState);
        if (filter == null) {
            original.call(renderState, poseStack, output, cameraRenderState);
            return;
        }

        SubmitNodeCollector routing = new RoutingSubmitCollector(output, output, filter);
        original.call(renderState, poseStack, routing, cameraRenderState);
    }

    @Unique
    private static @Nullable BiFunction<RenderType, CustomGeometryRenderer, CustomGeometryRenderer> realcamera$dragonFilter(
            AvatarRenderState renderState,
            CameraRenderState cameraRenderState) {
        if (!RealCameraCore.isActive()
                || ConfigFile.config().isClassic
                || !ConfigFile.config().renderModel
                || CompatibilityHelper.isRenderInScreen) return null;

        Minecraft client = Minecraft.getInstance();
        if (cameraRenderState != client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState) return null;
        Player player = client.player;
        if (player == null
                || client.getCameraEntity() != player
                || renderState.id != player.getId()
                || !DragonSurvivalCompat.ownsFirstPersonBody(player)) return null;

        return DragonSurvivalCompat.createGeometryFilter(RealCameraCore.currentTarget(), cameraRenderState.viewRotationMatrix);
    }
}
