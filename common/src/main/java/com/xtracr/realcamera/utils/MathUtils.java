package com.xtracr.realcamera.utils;

import net.minecraft.util.math.Vec3d;

public class MathUtils {
    
    public static Vec3d getEulerAngleYXZ(Matrix3fc normal) {
        if (normal.m21 <= -1.0D) {
            return new Vec3d(Math.PI/2, Math.atan2(normal.m10, normal.m00), 0.0D);
        } else if (normal.m21 >= 1.0D) {
            return new Vec3d(-Math.PI/2, -Math.atan2(normal.m10, normal.m00), 0.0D);
        }
        double xRot = Math.asin(-normal.m21);
        double cos = Math.cos(xRot);
        double yRot = Math.atan2(normal.m20/cos, normal.m22/cos);
        double zRot = Math.atan2(normal.m01/cos, normal.m11/cos);
        return new Vec3d(xRot, yRot, zRot);
    }

    public static Vec3d getIntersectionPoint(Vec3d planePoint, Vec3d planeNormal, Vec3d linePoint, Vec3d lineNormal) {
        double distance = planeNormal.dotProduct(planePoint.subtract(linePoint)) / planeNormal.dotProduct(lineNormal);
		return linePoint.add(lineNormal.multiply(distance));
    }

}
