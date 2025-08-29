package com.xtracr.realcamera.util;

import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public class SmoothUtil {
    private static final Quaternionf lastRotation = new Quaternionf();
    private static Vec3 lastPosition = Vec3.ZERO;

    public static Vec3 smoothPosition(Vec3 position) {
        double displacementSmoothFactor = ConfigFile.config().getDisplacementSmoothFactor();
        lastPosition = lastPosition.scale(displacementSmoothFactor).add(position.scale(1 - displacementSmoothFactor));
        return lastPosition;
    }

    public static Matrix3f smoothRotation(Matrix3f rotation) {
        return smoothRotation(new Quaternionf().setFromNormalized(rotation)).get(new Matrix3f());
    }

    public static Quaternionf smoothRotation(Quaternionf rotation) {
        lastRotation.slerp(rotation, 1 - (float) ConfigFile.config().getRotationSmoothFactor());
        return lastRotation;
    }
}
