package com.xtracr.realcamera.math;

import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

import net.minecraft.world.phys.Vec3;

public class Matrix3dr extends Matrix3d {

    public Matrix3dr(Matrix3f matrix3f) {
        //super(matrix3f);
        super(matrix3f.setTransposed(matrix3f));
    }

    public Vec3 getEulerAngle() {
        // Rotating order: YXZ
        if (this.m12 <= -1.0D) {
            return new Vec3(Math.PI/2, Math.atan2(m01, m00), 0.0D);
        }
        else if (this.m12 >= 1.0D) {
            return new Vec3(-Math.PI/2, -Math.atan2(m01, m00), 0.0D);
        }
        double xRot = Math.asin(-this.m12);
        double cos = Math.cos(xRot);
        double yRot = Math.atan2(this.m02/cos, this.m22/cos);
        double zRot = Math.atan2(this.m10/cos, this.m11/cos);
        return new Vec3(xRot, yRot, zRot);
    }

    public Vec3 getEulerAngleDegrees() {
        return this.getEulerAngle().scale(180.0D/Math.PI);
    }

    public static Matrix3d multiply(Matrix3d left, Matrix3d right) {
        Matrix3d matrix = new Matrix3d();
        matrix.m00 = left.m00 * right.m00 + left.m01 * right.m10 + left.m02 * right.m20;
        matrix.m01 = left.m00 * right.m01 + left.m01 * right.m11 + left.m02 * right.m21;
        matrix.m02 = left.m00 * right.m02 + left.m01 * right.m12 + left.m02 * right.m22;
        matrix.m10 = left.m10 * right.m00 + left.m11 * right.m10 + left.m12 * right.m20;
        matrix.m11 = left.m10 * right.m01 + left.m11 * right.m11 + left.m12 * right.m21;
        matrix.m12 = left.m10 * right.m02 + left.m11 * right.m12 + left.m12 * right.m22;
        matrix.m20 = left.m20 * right.m00 + left.m21 * right.m10 + left.m22 * right.m20;
        matrix.m21 = left.m20 * right.m01 + left.m21 * right.m11 + left.m22 * right.m21;
        matrix.m22 = left.m20 * right.m02 + left.m21 * right.m12 + left.m22 * right.m22;
        return matrix;
    }

    public void mulByRight(Matrix3d matrix) {
        this.set(multiply(this, matrix));
    }

    public void mulByRight(Matrix3f matrix3f) {
        this.mulByRight(new Matrix3d(matrix3f));
    }

    public void mulByRight(Quaternionf quaternionf) {
        this.mulByRight((new Matrix3d()).set(quaternionf));
    }
    
    public void mulByLeft(Matrix3d matrix) {
        this.set(multiply(matrix, this));
    }

    public void mulByLeft(Matrix3f matrix3f) {
        this.mulByLeft(new Matrix3d(matrix3f));
    }

    public void mulByLeft(Quaternionf quaternionf) {
        this.mulByLeft((new Matrix3d()).set(quaternionf));
    }
    
}
