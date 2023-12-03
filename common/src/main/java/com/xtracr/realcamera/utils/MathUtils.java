package com.xtracr.realcamera.utils;

import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vector4f;

public class MathUtils {

    public static Vec3d getEulerAngleYXZ(Matrix3fc normal) {
        if (normal.m21 <= -1.0D) {
            return new Vec3d(Math.PI/2, Math.atan2(normal.m10, normal.m00), 0.0D);
        } else if (normal.m21 >= 1.0D) {
            return new Vec3d(-Math.PI/2, -Math.atan2(normal.m10, normal.m00), 0.0D);
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
        Vector4f vector4f = new Vector4f((float)vec3d.getX(), (float)vec3d.getY(), (float)vec3d.getZ(), 1.0F);
        for (Matrix4f matrix4f : projectionMatrices) vector4f.transform(matrix4f);
        if (vector4f.getW() == 0.0D) return Vec3d.ZERO;
        return new Vec3d(vector4f.getX(), vector4f.getY(), 0).multiply(1/(double)vector4f.getW());
    }
}
