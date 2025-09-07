package com.xtracr.realcamera.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class MathUtil {
    public static double round(double d, int digits) {
        return Math.round(d * Math.pow(10, digits)) / Math.pow(10, digits);
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

    private static final ThreadLocal<Vector4f> TL_VEC4 = ThreadLocal.withInitial(() -> new Vector4f());

    public static Vec3 projectToVec2(Vec3 vec3, Matrix4f... projectionMatrices) {
        Vector4f v = TL_VEC4.get();
        v.set((float) vec3.x(), (float) vec3.y(), (float) vec3.z(), 1.0f);
        for (Matrix4f matrix4f : projectionMatrices) v.mul(matrix4f);
        if (v.w() == 0.0f) return Vec3.ZERO;
        double invW = 1.0 / v.w();
        return new Vec3(v.x() * invW, v.y() * invW, 0.0);
    }
}
