package com.xtracr.realcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    @Final Minecraft minecraft;
    @Shadow
    @Final private Camera mainCamera;

    @ModifyVariable(method = "pick", at = @At("STORE"), name = "entityHitResult")
    private EntityHitResult realcamera$modifyEntityHitResult(EntityHitResult entityHitResult) {
        CrosshairUtil.capturedEntityHitResult = entityHitResult;
        if (!ConfigFile.config().dynamicCrosshair && RealCameraCore.isActive()) {
            Entity cameraEntity = minecraft.getCameraEntity();
            Pair<Vec3, Vec3> fromAndTo = RaycastUtil.getFromAndTo(cameraEntity, minecraft.gameMode.getPickRange() * minecraft.gameMode.getPickRange(), minecraft.getFrameTime());
            Vec3 from = fromAndTo.getFirst();
            Vec3 to = fromAndTo.getSecond();
            double sqDistance = minecraft.hitResult != null ? minecraft.hitResult.getLocation().distanceToSqr(from) : to.distanceToSqr(from);
            AABB box = cameraEntity.getBoundingBox().expandTowards(to.subtract(from)).inflate(1.0, 1.0, 1.0);
            CrosshairUtil.capturedEntityHitResult = ProjectileUtil.getEntityHitResult(cameraEntity, from, to, box, entity -> !entity.isSpectator() && entity.isPickable(), sqDistance);
        }
        return CrosshairUtil.capturedEntityHitResult;
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"))
    private void realcamera$atBeforeCameraSetup(float deltaTick, long l, PoseStack poseStack, CallbackInfo ci) {
        CompatibilityHelper.NEA_setDeltaTick(deltaTick);
        RealCameraCore.initialize(minecraft);
        if (RealCameraCore.isActive() && !ConfigFile.config().isClassic) {
            EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            dispatcher.prepare(minecraft.level, mainCamera,  minecraft.crosshairPickEntity);
            RealCameraCore.computeCamera(minecraft, deltaTick);
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareCullFrustum(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;)V"))
    private void realcamera$atPrepareCullFrustum(CallbackInfo ci) {
        CompatibilityHelper.forceSetCameraPos(mainCamera);
    }
}
