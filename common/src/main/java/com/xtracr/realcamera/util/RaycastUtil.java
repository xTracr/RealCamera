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
        Vec3 eyePos = entity.getEyePosition(deltaTick);
        startVec = camera.getPosition();
        Vec3 direction = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
        Vec3 offset = startVec.subtract(eyePos);
        Vec3 footPoint = MathUtil.getIntersectionPoint(Vec3.ZERO, direction, offset, direction);
        if (footPoint.lengthSqr() > sqDistance) {
            startVec = eyePos;
            direction = entity.getViewVector(deltaTick);
            endVec = startVec.add(direction.scale(Math.sqrt(sqDistance)));
            return;
        } else if (offset.lengthSqr() > sqDistance) {
            startVec = startVec.add(direction.scale(offset.distanceTo(footPoint) - Math.sqrt(sqDistance - footPoint.lengthSqr())));
        }
        endVec = eyePos.add(footPoint.add(direction.scale(Math.sqrt(sqDistance - footPoint.lengthSqr()))));
    }
}
