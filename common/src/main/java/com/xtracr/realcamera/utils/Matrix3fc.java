package com.xtracr.realcamera.utils;

import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Vec3f;

public class Matrix3fc {

    public float m00, m10, m20;
    public float m01, m11, m21;
    public float m02, m12, m22;


    public Matrix3fc(Matrix3f matrix3f) {
        Vec3f column0 = new Vec3f(1.0F, 0.0F, 0.0F);
        Vec3f column1 = new Vec3f(0.0F, 1.0F, 0.0F);
        Vec3f column2 = new Vec3f(0.0F, 0.0F, 1.0F);
        column0.transform(matrix3f);
        column1.transform(matrix3f);
        column2.transform(matrix3f);
        this.m00 = column0.getX();
        this.m01 = column0.getY();
        this.m02 = column0.getZ();
        this.m10 = column1.getX();
        this.m11 = column1.getY();
        this.m12 = column1.getZ();
        this.m20 = column2.getX();
        this.m21 = column2.getY();
        this.m22 = column2.getZ();
    }

    public Matrix3fc scale(float x,float y, float z) {
        this.m00 *= x;
        this.m01 *= x;
        this.m02 *= x;
        this.m10 *= y;
        this.m11 *= y;
        this.m12 *= y;
        this.m20 *= z;
        this.m21 *= z;
        this.m22 *= z;
        return this;
    }

    public Matrix3fc rotateLocal(float angle, float x, float y, float z) {
        float s = (float)Math.sin(angle);
        float c = (float)Math.cos(angle);
        float C = 1.0f - c;
        float xx = x * x, xy = x * y, xz = x * z;
        float yy = y * y, yz = y * z;
        float zz = z * z;
        float lm00 = xx * C + c;
        float lm01 = xy * C + z * s;
        float lm02 = xz * C - y * s;
        float lm10 = xy * C - z * s;
        float lm11 = yy * C + c;
        float lm12 = yz * C + x * s;
        float lm20 = xz * C + y * s;
        float lm21 = yz * C - x * s;
        float lm22 = zz * C + c;
        float nm00 = lm00 * m00 + lm10 * m01 + lm20 * m02;
        float nm01 = lm01 * m00 + lm11 * m01 + lm21 * m02;
        float nm02 = lm02 * m00 + lm12 * m01 + lm22 * m02;
        float nm10 = lm00 * m10 + lm10 * m11 + lm20 * m12;
        float nm11 = lm01 * m10 + lm11 * m11 + lm21 * m12;
        float nm12 = lm02 * m10 + lm12 * m11 + lm22 * m12;
        float nm20 = lm00 * m20 + lm10 * m21 + lm20 * m22;
        float nm21 = lm01 * m20 + lm11 * m21 + lm21 * m22;
        float nm22 = lm02 * m20 + lm12 * m21 + lm22 * m22;
        this.m00 = nm00;
        this.m01 = nm01;
        this.m02 = nm02;
        this.m10 = nm10;
        this.m11 = nm11;
        this.m12 = nm12;
        this.m20 = nm20;
        this.m21 = nm21;
        this.m22 = nm22;
        return this;
    }
}
