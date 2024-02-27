package com.xtracr.realcamera.util;

import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class MathUtil {
    public static double round(double d, int digits) {
        return Math.round(d * Math.pow(10, digits)) / Math.pow(10, digits);
    }

    public static boolean isFinite(Vec3d vec3d) {
        return Double.isFinite(vec3d.getX()) && Double.isFinite(vec3d.getY()) && Double.isFinite(vec3d.getZ());
    }

    public static Vec3d getEulerAngleYXZ(Matrix3f normal) {
        if (normal.m21 <= -1.0d) {
            return new Vec3d(Math.PI / 2, Math.atan2(normal.m10, normal.m00), 0.0d);
        } else if (normal.m21 >= 1.0d) {
            return new Vec3d(-Math.PI / 2, -Math.atan2(normal.m10, normal.m00), 0.0d);
        }
        double xRot = Math.asin(-normal.m21);
        double yRot = Math.atan2(normal.m20, normal.m22);
        double zRot = Math.atan2(normal.m01, normal.m11);
        return new Vec3d(xRot, yRot, zRot);
    }

    public static Vec3d getIntersectionPoint(Vec3d planePoint, Vec3d planeNormal, Vec3d linePoint, Vec3d lineNormal) {
        double distance = planeNormal.dotProduct(planePoint.subtract(linePoint)) / planeNormal.dotProduct(lineNormal);
        return linePoint.add(lineNormal.multiply(distance));
    }

    public static Vec3d projectToVec2d(Vec3d vec3d, Matrix4f... projectionMatrices) {
        Vector4f vector4f = new Vector4f((float) vec3d.getX(), (float) vec3d.getY(), (float) vec3d.getZ(), 1.0f);
        for (Matrix4f matrix4f : projectionMatrices) vector4f.mul(matrix4f);
        if (vector4f.w() == 0.0d) return Vec3d.ZERO;
        return new Vec3d(vector4f.x(), vector4f.y(), 0).multiply(1 / (double) vector4f.w());
    }
}
