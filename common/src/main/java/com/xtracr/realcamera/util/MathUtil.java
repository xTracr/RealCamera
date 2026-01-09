package com.xtracr.realcamera.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class MathUtil {
    public static double round(double d, int digits) {
        return Math.round(d * Math.pow(10, digits)) / Math.pow(10, digits);
    }

    public static double floor(double... ds) {
        double d = ds[0];
        for (int i = 1; i < ds.length; i++) {
            d = Math.min(d, ds[i]);
        }
        return Math.floor(d);
    }

    public static double ceil(double... ds) {
        double d = ds[0];
        for (int i = 1; i < ds.length; i++) {
            d = Math.max(d, ds[i]);
        }
        return Math.ceil(d);
    }


    public static Vec3 getEulerAngleYXZ(Matrix3f normal) {
        if (normal.m21 <= -1.0) return new Vec3(Math.PI / 2, Math.atan2(normal.m10, normal.m00), 0.0);
        else if (normal.m21 >= 1.0) return new Vec3(-Math.PI / 2, -Math.atan2(normal.m10, normal.m00), 0.0);
        double xRot = Math.asin(-normal.m21);
        double yRot = Math.atan2(normal.m20, normal.m22);
        double zRot = Math.atan2(normal.m01, normal.m11);
        return new Vec3(xRot, yRot, zRot);
    }

    public static Vec3 getIntersectionPoint(Vec3 planePoint, Vec3 planeNormal, Vec3 linePoint, Vec3 lineNormal) {
        double distance = planeNormal.dot(planePoint.subtract(linePoint)) / planeNormal.dot(lineNormal);
        return linePoint.add(lineNormal.scale(distance));
    }

    public static Vec3 projectToVec2(Vec3 vec3, Matrix4fc... projectionMatrices) {
        Vector4f vector4f = new Vector4f((float) vec3.x(), (float) vec3.y(), (float) vec3.z(), 1.0f);
        for (Matrix4fc matrix4f : projectionMatrices) vector4f.mul(matrix4f);
        if (vector4f.w() == 0.0) return Vec3.ZERO;
        return new Vec3(vector4f.x(), vector4f.y(), 0).scale(1 / (double) vector4f.w());
    }
}
