package com.xtracr.realcamera.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;

public class RaycastUtil {
    private static Vec3 startVec = Vec3.ZERO;
    private static Vec3 endVec = Vec3.ZERO;

    public static Vec3 getStartVec() {
        return startVec;
    }

    public static Vec3 getEndVec() {
        return endVec;
    }

    public static ClipContext getClipContext(Block shapeType, Fluid fluidHandling, Entity entity) {
        return new ClipContext(startVec, endVec, shapeType, fluidHandling, entity);
    }

    public static void update(Entity entity, double sqDistance, float deltaTick) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        // Camera world position
        Vec3 camPosVec = camera.getPosition();
        final double cx = camPosVec.x();
        final double cy = camPosVec.y();
        final double cz = camPosVec.z();

        // Player eye world position
        Vec3 eyePosVec = entity.getEyePosition(deltaTick);
        final double ex = eyePosVec.x();
        final double ey = eyePosVec.y();
        final double ez = eyePosVec.z();

        // Camera forward direction (unit vector)
        Vec3 dirVec = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
        final double dx = dirVec.x();
        final double dy = dirVec.y();
        final double dz = dirVec.z();

        // Offset from player eye to camera position
        final double ox = cx - ex;
        final double oy = cy - ey;
        final double oz = cz - ez;

        // Project offset onto the plane normal to the camera direction to get foot point
        // foot = offset - dir * (dot(dir, offset) / |dir|^2). For normalized dir, |dir|^2 == 1
        final double dot = dx * ox + dy * oy + dz * oz;
        final double fx = ox - dx * dot;
        final double fy = oy - dy * dot;
        final double fz = oz - dz * dot;
        final double fLenSq = fx * fx + fy * fy + fz * fz;

        if (fLenSq > sqDistance) {
            // Start from eye and use entity view direction
            Vec3 view = entity.getViewVector(deltaTick);
            final double scale = Math.sqrt(sqDistance);
            startVec = eyePosVec;
            endVec = new Vec3(ex + view.x() * scale, ey + view.y() * scale, ez + view.z() * scale);
            return;
        }

        final double oLenSq = ox * ox + oy * oy + oz * oz;
        if (oLenSq > sqDistance) {
            // Move startVec along camera direction to stay within range
            final double along = Math.abs(dot); // since dir is unit length
            final double remain = Math.sqrt(sqDistance - fLenSq);
            final double advance = along - remain;
            startVec = new Vec3(cx + dx * (-advance), cy + dy * (-advance), cz + dz * (-advance));
        } else {
            startVec = camPosVec;
        }

        final double remain2 = Math.sqrt(sqDistance - fLenSq);
        endVec = new Vec3(ex + fx + dx * remain2, ey + fy + dy * remain2, ez + fz + dz * remain2);
    }
}
