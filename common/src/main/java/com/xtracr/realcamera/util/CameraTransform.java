package com.xtracr.realcamera.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;

public class CameraTransform {
    protected final Matrix3f rotation = new Matrix3f();
    protected Vec3 position = Vec3.ZERO;

    public final Vec3 getPosition() {
        return position;
    }

    public final void setPosition(Vec3 vec) {
        position = vec;
    }

    public final Matrix3f getRotation() {
        return rotation;
    }

    public final void setRotation(Matrix3fc mat) {
        rotation.set(mat);
    }

    public final void lerpPosition(Vec3 vec, double a) {
        position = position.lerp(vec, a);
    }

    public final void slerpRotation(Matrix3fc mat, double alpha) {
        slerpRotation(new Quaterniond().setFromNormalized(mat), alpha);
    }

    public final void slerpRotation(Quaterniondc quat, double alpha) {
        new Quaterniond().setFromNormalized(rotation).slerp(quat, alpha).get(rotation);
    }
}
