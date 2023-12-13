package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
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
    @Shadow
    private boolean thirdPerson;

    @Inject(method = "update", at = @At("RETURN"))
    private void realCamera$updateCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
            float tickDelta, CallbackInfo cInfo) {
        if (RealCameraCore.isActive()) {
            final ModConfig config = ConfigFile.modConfig;
            if (config.isRendering() && !config.shouldDisableRendering(MinecraftClient.getInstance())) {
                this.thirdPerson = true;
            }

            Vec3d startVec = pos;
            if (config.isClassic()) {
                Vec3d offset = new Vec3d(config.getClassicX(), config.getClassicY(), config.getClassicZ()).multiply(config.getScale());
                Vec3d center = new Vec3d(config.getCenterX(), config.getCenterY(), config.getCenterZ()).multiply(config.getScale());
                float newPitch = pitch + config.getClassicPitch();
                float newYaw = yaw - config.getClassicYaw();
                if (config.compatPehkui()) {
                    offset = PehkuiCompat.scaleVec3d(offset, focusedEntity, tickDelta);
                    center = PehkuiCompat.scaleVec3d(center, focusedEntity, tickDelta);
                }

                setRotation(yaw, 0.0F);
                moveBy(center.getX(), center.getY(), center.getZ());
                setRotation(newYaw, newPitch);
                RealCameraCore.setRoll(config.getClassicRoll());
                moveBy(offset.getX(), offset.getY(), offset.getZ());
                realCamera$clipToSpace(startVec);
            } else {
                Vec3d prevPos = RealCameraCore.getPos();
                Box box = focusedEntity.getBoundingBox();
                double restrictedY = MathHelper.clamp(prevPos.getY(), box.minY + 0.1D, box.maxY - 0.1D);
                startVec = new Vec3d(pos.getX(), restrictedY, pos.getZ());
                if (!config.doOffsetModel()) {
                    setPos(prevPos);
                    realCamera$clipToSpace(startVec);
                }
                RealCameraCore.setModelOffset(pos.subtract(prevPos));
                setRotation(config.isYawingBound() ? RealCameraCore.getYaw() : yaw - config.getBindingYaw(),
                        config.isPitchingBound() ? RealCameraCore.getPitch() : pitch + config.getBindingPitch());
            }
        }
    }

    @Unique
    private void realCamera$clipToSpace(Vec3d startVec) {
        if (!ConfigFile.modConfig.doClipToSpace()) return;
        Vec3d offset = pos.subtract(startVec);
        final float depth = 0.085F;
        for (int i = 0; i < 8; ++i) {
            float f = depth * ((i & 1) * 2 - 1);
            float g = depth * ((i >> 1 & 1) * 2 - 1);
            float h = depth * ((i >> 2 & 1) * 2 - 1);
            Vec3d start = startVec.add(f, g, h);
            Vec3d end = startVec.add(offset).add(f, g, h);
            HitResult hitResult = area.raycast(new RaycastContext(start, end,
                    RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, focusedEntity));
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
