package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private <E extends Entity> Vec3d realCamera$modifyOffset(Vec3d vec, E entity, double x, double y, double z,
            float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (entity instanceof ClientPlayerEntity && RealCameraCore.isActive()) {
            vec = vec.add(RealCameraCore.getModelOffset());
        }
        return vec;
    }
}
