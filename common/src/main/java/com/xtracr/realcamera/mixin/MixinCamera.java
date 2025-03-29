package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    private BlockGetter level;
    @Shadow
    private Entity entity;
    @Shadow
    private Vec3 position;
    @Shadow
    private float xRot;
    @Shadow
    private float yRot;

    @Inject(method = "setup", at = @At("RETURN"))
    private void realcamera$setupCamera(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!RealCameraCore.isActive()) return;
        ModConfig config = ConfigFile.config();
        Vec3 startVec = position;
        AABB box = focusedEntity.getBoundingBox();
        if (config.isClassic()) {
            double scale = focusedEntity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1;
            Vec3 offset = new Vec3(config.getClassicX(), config.getClassicY(), -config.getClassicZ()).scale(scale);
            Vec3 center = new Vec3(config.getCenterX(), config.getCenterY(), -config.getCenterZ()).scale(scale);
            float newPitch = xRot + config.getClassicPitch();
            float newYaw = yRot - config.getClassicYaw();
            setRotation(yRot, 0.0f);
            move((float) center.x(), (float) center.y(), (float) center.z());
            setRotation(newYaw, newPitch);
            move((float) offset.x(), (float) offset.y(), (float) offset.z());
        } else {
            Vec3 rawPos = RealCameraCore.getRawPos(position);
            double restrictedY = Mth.clamp(rawPos.y(), box.minY + 0.1D, box.maxY - 0.1D);
            startVec = new Vec3(position.x(), restrictedY, position.z());
            setPosition(rawPos);
            setRotation(RealCameraCore.getYaw(yRot), RealCameraCore.getPitch(xRot));
        }
        realcamera$clipToSpace(startVec, realcamera$getFov(tickDelta));
        RealCameraCore.setCameraPos(position);
    }

    @Unique
    private void realcamera$clipToSpace(Vec3 startVec, double fov) {
        Vec3 offset = position.subtract(startVec);
        final float depth = 0.05f + (float) (fov * (0.0001 + 0.000005 * fov));
        for (int i = 0; i < 8; ++i) {
            float f = depth * ((i & 1) * 2 - 1);
            float g = depth * ((i >> 1 & 1) * 2 - 1);
            float h = depth * ((i >> 2 & 1) * 2 - 1);
            Vec3 start = startVec.add(f, g, h);
            Vec3 end = startVec.add(offset).add(f, g, h);
            HitResult hitResult = level.clip(new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
            double l = hitResult.getLocation().distanceTo(start);
            if (hitResult.getType() == HitResult.Type.MISS || l >= offset.length()) continue;
            offset = offset.scale(l / offset.length());
        }
        setPosition(startVec.add(offset));
    }

    @Unique
    private static float realcamera$getFov(float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        float fovModifier = Mth.lerp(tickDelta, ((GameRendererAccessor) client.gameRenderer).getOldFov(), ((GameRendererAccessor) client.gameRenderer).getFov());
        return client.options.fov().get() * fovModifier;
    }

    @Shadow
    protected abstract void move(float x, float y, float z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPosition(Vec3 position);
}
