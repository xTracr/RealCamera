package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.DoABarrelRollCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.utils.RaycastUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    
    private static boolean toggle = false;

    @Shadow
    @Final
    MinecraftClient client;

    /* 
    @ModifyArgs(...)
    private void modifyEntityHitResult(Args args) {
        if (CameraController.isActive()) {
            Vec3d startVec = RaycastUtils.getStartVec();
            args.set(1, startVec);
            args.set(2, RaycastUtils.getEndVec());
            if (client.crosshairTarget != null) args.set(5, client.crosshairTarget.getPos().squaredDistanceTo(startVec));
        }
    }*/

    @ModifyVariable(method = "updateTargetedEntity", at = @At("STORE"), ordinal = 0)
    private EntityHitResult modifyEntityHitResult(EntityHitResult entityHitResult) {
        if (RealCameraCore.isActive()) {
            Vec3d startVec = RaycastUtils.getStartVec();
            Vec3d endVec = RaycastUtils.getEndVec();
            double sqDistance = (this.client.crosshairTarget != null ? 
                this.client.crosshairTarget.getPos().squaredDistanceTo(startVec) : endVec.squaredDistanceTo(startVec));
            Entity cameraEntity = this.client.getCameraEntity();
            Box box = cameraEntity.getBoundingBox().stretch(cameraEntity.getRotationVec(this.client.getTickDelta())
                    .multiply(this.client.interactionManager.getReachDistance())).expand(1.0, 1.0, 1.0);
            return ProjectileUtil.raycast(cameraEntity, startVec, endVec, box, entity -> !entity.isSpectator() && entity.canHit(), sqDistance);
        }
        return entityHitResult;
    }

    @Inject(
        method = "renderHand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;push()V"
        )
    )
    private void setThirdPerson(CallbackInfo cInfo) {
        if (ConfigFile.modConfig.isRendering() && RealCameraCore.isActive()) {
            this.client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            toggle = true;
        }
    }

    @Inject(
        method = "renderHand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"
        )
    )
    private void setFirstPerson(CallbackInfo cInfo) {
        if (toggle) {
            this.client.options.setPerspective(Perspective.FIRST_PERSON);
            toggle = false;
        }
    }

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
            shift = At.Shift.BY,
            by = -2
        )
    )
    private void beforeCameraUpdate(float tickDelta, long limitTime, MatrixStack matrixStack, CallbackInfo cInfo) {
        if (ConfigFile.modConfig.compatDoABarrelRoll() && DoABarrelRollCompat.modEnabled()) {
            matrixStack.loadIdentity();
        }
    }

}
