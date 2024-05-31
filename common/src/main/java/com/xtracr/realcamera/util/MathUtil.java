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

    public static Vec3 projectToVec2d(Vec3 vec3d, Matrix4f... projectionMatrices) {
        Vector4f vector4f = new Vector4f((float) vec3d.x(), (float) vec3d.y(), (float) vec3d.z(), 1.0f);
        for (Matrix4f matrix4f : projectionMatrices) vector4f.mul(matrix4f);
        if (vector4f.w() == 0.0) return Vec3.ZERO;
        return new Vec3(vector4f.x(), vector4f.y(), 0).scale(1 / (double) vector4f.w());
    }
}
