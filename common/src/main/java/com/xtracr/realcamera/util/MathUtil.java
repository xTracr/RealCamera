package com.xtracr.realcamera.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class MathUtil {
    public static double round(double d, int digits) {
        return Math.round(d * Math.pow(10, digits)) / Math.pow(10, digits);
    }

    public static float cross(float a, float b, float c, float d) {
        return a * d - b * c;
    }

    public static boolean pointInTriangle(float x, float y, float x0, float y0, float x1, float y1, float x2, float y2) {
        float abX = x1 - x0, abY = y1 - y0;
        float acX = x2 - x0, acY = y2 - y0;
        float alpha = cross(abX, abY, acX, acY);
        if (alpha == 0) return false;
        float apX = x - x0, apY = y - y0;
        float beta = cross(apX, apY, acX, acY) / alpha;
        if (beta < 0) return false;
        float gamma = cross(abX, abY, apX, apY) / alpha;
        if (gamma < 0) return false;
        return beta + gamma <= 1;
    }

    public static boolean pointInQuad(float x, float y, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        return pointInTriangle(x, y, x0, y0, x1, y1, x2, y2) || pointInTriangle(x, y, x0, y0, x2, y2, x3, y3);
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
