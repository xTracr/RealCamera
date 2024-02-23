package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
import com.xtracr.realcamera.util.RaycastUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
    @Final MinecraftClient client;

    @Shadow @Final private Camera camera;

    @ModifyVariable(method = "updateTargetedEntity", at = @At("STORE"), ordinal = 0)
    private EntityHitResult realCamera$modifyEntityHitResult(EntityHitResult entityHitResult) {
        CrosshairUtil.capturedEntityHitResult = entityHitResult;
        if (!ConfigFile.modConfig.isCrosshairDynamic() && RealCameraCore.isActive()) {
            Vec3d startVec = RaycastUtil.getStartVec();
            Vec3d endVec = RaycastUtil.getEndVec();
            double sqDistance = (client.crosshairTarget != null ?
                    client.crosshairTarget.getPos().squaredDistanceTo(startVec) : endVec.squaredDistanceTo(startVec));
            Entity cameraEntity = client.getCameraEntity();
            Box box = cameraEntity.getBoundingBox().stretch(cameraEntity.getRotationVec(client.getTickDelta())
                    .multiply(client.interactionManager.getReachDistance())).expand(1.0, 1.0, 1.0);
            CrosshairUtil.capturedEntityHitResult = ProjectileUtil.raycast(cameraEntity, startVec, endVec, box, entity -> !entity.isSpectator() && entity.canHit(), sqDistance);
        }
        return CrosshairUtil.capturedEntityHitResult;
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void realCamera$onRenderWorldHEAD(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo cInfo) {
        RealCameraCore.init(client);
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"))
    private void realCamera$onBeforeCameraUpdate(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo cInfo) {
        if (RealCameraCore.isActive()) {
            RealCameraCore.computeCamera(client, tickDelta);
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;setupFrustum(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lorg/joml/Matrix4f;)V"))
    private void realCamera$onBeforeSetupFrustum(CallbackInfo cInfo) {
        if (RealCameraCore.isActive() && !ConfigFile.modConfig.isClassic()) {
            ((CameraAccessor) camera).invokeSetPos(RealCameraCore.getCameraPos());
        }
    }
}
