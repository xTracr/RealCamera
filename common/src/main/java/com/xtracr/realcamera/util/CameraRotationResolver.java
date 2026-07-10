package com.xtracr.realcamera.util;

import com.xtracr.realcamera.config.BindTarget.BindConfig;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3d;

/** Resolves each camera Y-X-Z coordinate from the adjusted model candidate or vanilla camera. */
public final class CameraRotationResolver {
    private final ContinuousEulerAngleTracker tracker = new ContinuousEulerAngleTracker();

    public Vector3d resolve(
            Matrix3f adjustedModelRotation,
            double vanillaPitch,
            double vanillaYaw,
            double vanillaRoll,
            BindConfig bindConfig
    ) {
        Vector3d reference = new Vector3d(
                Math.toRadians(vanillaPitch),
                Math.toRadians(-vanillaYaw),
                Math.toRadians(vanillaRoll)
        );
        Vector3d tracked = tracker.update(adjustedModelRotation, reference);

        if (bindConfig.bindNoRotation()) {
            return new Vector3d(vanillaPitch, vanillaYaw, vanillaRoll);
        }
        if (bindConfig.bindRotation()) {
            double scale = Math.toDegrees(1);
            Vec3 existing = MathUtil.getEulerAngleYXZ(adjustedModelRotation).multiply(scale, -scale, scale);
            return new Vector3d(existing.x, existing.y, existing.z);
        }

        double modelPitch = Math.toDegrees(tracked.x);
        double modelYaw = -Math.toDegrees(tracked.y);
        double modelRoll = Math.toDegrees(tracked.z);
        return new Vector3d(
                bindConfig.bindPitch() ? modelPitch : vanillaPitch,
                bindConfig.bindYaw() ? modelYaw : vanillaYaw,
                bindConfig.bindRoll() ? modelRoll : vanillaRoll
        );
    }

    public void reset() {
        tracker.reset();
    }
}
