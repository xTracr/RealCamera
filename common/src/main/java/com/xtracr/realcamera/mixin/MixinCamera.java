package com.xtracr.realcamera.mixin;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    private BlockView area;
    @Shadow
    private Entity focusedEntity;
    @Shadow
    private Vec3d pos;
    @Shadow
    private float pitch;
    @Shadow
    private float yaw;

    @Inject(method = "update", at = @At("RETURN"))
    private void realcamera$updateCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo cInfo) {
        if (!RealCameraCore.isActive()) return;
        final ModConfig config = ConfigFile.config();
        Vec3d startVec = pos;
        Box box = focusedEntity.getBoundingBox();
        if (config.isClassic()) {
            EntityDimensions playerDimensions = EntityType.PLAYER.getDimensions();
            double scale = 2 * box.getAverageSideLength() / (playerDimensions.height + playerDimensions.width);
            Vec3d offset = new Vec3d(config.getClassicX(), config.getClassicY(), config.getClassicZ()).multiply(scale);
            Vec3d center = new Vec3d(config.getCenterX(), config.getCenterY(), config.getCenterZ()).multiply(scale);
            float newPitch = pitch + config.getClassicPitch();
            float newYaw = yaw - config.getClassicYaw();
            setRotation(yaw, 0.0f);
            moveBy(center.getX(), center.getY(), center.getZ());
            setRotation(newYaw, newPitch);
            moveBy(offset.getX(), offset.getY(), offset.getZ());
        } else {
            Vec3d prevPos = RealCameraCore.getPos(pos);
            double restrictedY = MathHelper.clamp(prevPos.getY(), box.minY + 0.1D, box.maxY - 0.1D);
            startVec = new Vec3d(pos.getX(), restrictedY, pos.getZ());
            setPos(prevPos);
            setRotation(RealCameraCore.getYaw(yaw), RealCameraCore.getPitch(pitch));
        }
        realcamera$clipToSpace(startVec);
        RealCameraCore.setCameraPos(pos);
    }

    @Unique
    private void realcamera$clipToSpace(Vec3d startVec) {
        Vec3d offset = pos.subtract(startVec);
        final float depth = 0.085F;
        for (int i = 0; i < 8; ++i) {
            float f = depth * ((i & 1) * 2 - 1);
            float g = depth * ((i >> 1 & 1) * 2 - 1);
            float h = depth * ((i >> 2 & 1) * 2 - 1);
            Vec3d start = startVec.add(f, g, h);
            Vec3d end = startVec.add(offset).add(f, g, h);
            HitResult hitResult = area.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, focusedEntity));
            double l = hitResult.getPos().distanceTo(start);
            if (hitResult.getType() == HitResult.Type.MISS || l >= offset.length()) continue;
            offset = offset.multiply(l / offset.length());
        }
        setPos(startVec.add(offset));
    }

    @Shadow
    protected abstract void moveBy(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(Vec3d pos);
}
