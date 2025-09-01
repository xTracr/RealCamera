package com.xtracr.realcamera.util;

import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaterniond;

public class SmoothUtil {
    private static final Quaterniond lastRotation = new Quaterniond();
    private static Vec3 lastPosition = Vec3.ZERO;

    public static Vec3 smoothPosition(Vec3 position) {
        lastPosition = position.add(lastPosition.subtract(position).scale(ConfigFile.config().getDisplacementSmoothFactor()));
        return lastPosition;
    }

    public static Matrix3f smoothRotation(Matrix3f rotation) {
        return smoothRotation(new Quaterniond().setFromNormalized(rotation)).get(new Matrix3f());
    }

    public static Quaterniond smoothRotation(Quaterniond rotation) {
        lastRotation.slerp(rotation, 1 - (float) ConfigFile.config().getRotationSmoothFactor());
        return lastRotation;
    }
}
