package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.compat.SableCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.mixin.accessor.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
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
    private Vec3 position;
    @Shadow
    private float xRot;
    @Shadow
    private float yRot;

    @Inject(method = "setup", at = @At("RETURN"))
    private void realcamera$setupCamera(BlockGetter blockGetter, Entity entity, boolean bl, boolean bl2, float f, CallbackInfo ci) {
        if (!RealCameraCore.isActive()) return;
        ModConfig config = ConfigFile.config();
        Vec3 startVec = position;
        if (config.isClassic) {
            double scale = entity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1;
            Vec3 offset = new Vec3(config.getClassicX(), config.getClassicY(), -config.getClassicZ()).scale(scale);
            Vec3 center = new Vec3(config.getCenterX(), config.getCenterY(), -config.getCenterZ()).scale(scale);
            float newPitch = xRot + config.getClassicPitch();
            float newYaw = yRot - config.getClassicYaw();
            setRotation(yRot, 0.0f);
            move((float) center.x(), (float) center.y(), (float) center.z());
            setRotation(newYaw, newPitch);
            move((float) offset.x(), (float) offset.y(), (float) offset.z());
        } else {
            Vec3 entityPos = entity.position().add(entity.position().subtract(entity.xOld, entity.yOld, entity.zOld).scale(entity.tickCount == 0 ? 0 : f - 1));
            Vec3 rawPos = RealCameraCore.getRawPos(position, entityPos);
            AABB box = entity.getBoundingBox();
            double restrictedY = Mth.clamp(rawPos.y(), box.minY + 0.1D, box.maxY - 0.1D);
            startVec = new Vec3(position.x(), restrictedY, position.z());
            setPosition(rawPos);
            Vec3 eulerAngle = RealCameraCore.getEulerAngle(xRot, yRot, 0);
            setRotation((float) eulerAngle.y(), (float) eulerAngle.x());
        }
        realcamera$clipToSpace(startVec, entity, realcamera$getFov(f));
    }

    @Unique
    private double realcamera$getSqDistance(Level level, Vec3 a, Vec3 b) {
        if (CompatibilityHelper.isModLoaded("sable")) return SableCompat.distanceSquaredWithSubLevels(level, a, b);
        return a.distanceToSqr(b);
    }

    @Unique
    private void realcamera$clipToSpace(Vec3 startVec, Entity entity, double fov) {
        Vec3 offset = position.subtract(startVec);
        final float depth = 0.05f + (float) (fov * (0.0001 + 0.000005 * fov));
        for (int i = 0; i < 8; ++i) {
            float offsetX = depth * ((i & 1) * 2 - 1);
            float offsetY = depth * ((i >> 1 & 1) * 2 - 1);
            float offsetZ = depth * ((i >> 2 & 1) * 2 - 1);
            Vec3 start = startVec.add(offsetX, offsetY, offsetZ);
            Vec3 end = startVec.add(offset).add(offsetX, offsetY, offsetZ);
            HitResult hitResult = level.clip(new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
            if (hitResult.getType() == HitResult.Type.MISS) continue;
            double sqDistance = realcamera$getSqDistance(entity.level(), hitResult.getLocation(), start);
            double sqOffsetL = offset.lengthSqr();
            if (sqDistance >= sqOffsetL) continue;
            offset = offset.scale(Math.sqrt(sqDistance / sqOffsetL));
        }
        setPosition(startVec.add(offset));
    }

    @Unique
    private static float realcamera$getFov(float partialTicks) {
        Minecraft client = Minecraft.getInstance();
        float fovModifier = Mth.lerp(partialTicks, ((GameRendererAccessor) client.gameRenderer).getOldFov(), ((GameRendererAccessor) client.gameRenderer).getFov());
        return client.options.fov().get() * fovModifier;
    }

    @Shadow
    protected abstract void move(float f, float g, float h);

    @Shadow
    protected abstract void setRotation(float f, float g);

    @Shadow
    protected abstract void setPosition(Vec3 vec3);
}
