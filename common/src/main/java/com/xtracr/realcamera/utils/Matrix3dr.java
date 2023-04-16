package com.xtracr.realcamera.utils;

import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

public class Matrix3dr {

    protected double m00, m01, m02;
    protected double m10, m11, m12;
    protected double m20, m21, m22;

    public Matrix3dr() {
    }

    public Matrix3dr(double a, double b, double c) {
        this.m00 = a;
        this.m01 = 0.0D;
        this.m02 = 0.0D;
        this.m10 = 0.0D;
        this.m11 = b;
        this.m12 = 0.0D;
        this.m20 = 0.0D;
        this.m21 = 0.0D;
        this.m22 = c;
    }
    
    public Matrix3dr(Matrix3f matrix3f) {
        Vec3f column0 = new Vec3f(1.0F, 0.0F, 0.0F);
        Vec3f column1 = new Vec3f(0.0F, 1.0F, 0.0F);
        Vec3f column2 = new Vec3f(0.0F, 0.0F, 1.0F);
        column0.transform(matrix3f);
        column1.transform(matrix3f);
        column2.transform(matrix3f);
        this.m00 = column0.getX();
        this.m10 = column0.getY();
        this.m20 = column0.getZ();
        this.m01 = column1.getX();
        this.m11 = column1.getY();
        this.m21 = column1.getZ();
        this.m02 = column2.getX();
        this.m12 = column2.getY();
        this.m22 = column2.getZ();
    }

    public Matrix3dr(Quaternion quaternion) {
        this(new Matrix3f(quaternion));
    }

    public void set(Matrix3dr matrix3dr) {
        this.m00 = matrix3dr.m00;
        this.m01 = matrix3dr.m01;
        this.m02 = matrix3dr.m02;
        this.m10 = matrix3dr.m10;
        this.m11 = matrix3dr.m11;
        this.m12 = matrix3dr.m12;
        this.m20 = matrix3dr.m20;
        this.m21 = matrix3dr.m21;
        this.m22 = matrix3dr.m22;
    } 

    public Vec3d getEulerAngle() {
        // Rotating order: ZXY
        if (this.m12 <= -1.0D) {
            return new Vec3d(Math.PI/2, Math.atan2(m01, m00), 0.0D);
        } else if (this.m12 >= 1.0D) {
            return new Vec3d(-Math.PI/2, -Math.atan2(m01, m00), 0.0D);
        }
        double xRot = Math.asin(-this.m12);
        double cos = Math.cos(xRot);
        double yRot = Math.atan2(this.m02/cos, this.m22/cos);
        double zRot = Math.atan2(this.m10/cos, this.m11/cos);
        return new Vec3d(xRot, yRot, zRot);
    }

    public Vec3d getEulerAngleDegrees() {
        return this.getEulerAngle().multiply(180.0D/Math.PI);
    }

    public static Matrix3dr multiply(Matrix3dr left, Matrix3dr right) {
        Matrix3dr matrix = new Matrix3dr();
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

    public void mulByRight(Matrix3dr matrix) {
        this.set(multiply(this, matrix));
    }

    public void mulByRight(Matrix3f matrix3f) {
        this.mulByRight(new Matrix3dr(matrix3f));
    }

    public void mulByRight(Quaternion quaternion) {
        this.mulByRight(new Matrix3dr(quaternion));
    }
    
    public void mulByLeft(Matrix3dr matrix) {
        this.set(multiply(matrix, this));
    }

    public void mulByLeft(Matrix3f matrix3f) {
        this.mulByLeft(new Matrix3dr(matrix3f));
    }

    public void mulByLeft(Quaternion quaternion) {
        this.mulByLeft(new Matrix3dr(quaternion));
    }
    
}
