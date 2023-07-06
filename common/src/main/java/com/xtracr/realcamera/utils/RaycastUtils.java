package com.xtracr.realcamera.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class RaycastUtils {
    private static Vec3d startVec = Vec3d.ZERO;
    private static Vec3d direction = Vec3d.ZERO;
    private static Vec3d endVec = Vec3d.ZERO;

    public static Vec3d getStartVec() {
        return startVec;
    }

    public static Vec3d getDirection() {
        return direction;
    }

    public static Vec3d getEndVec() {
        return endVec;
    }

    public static RaycastContext getRaycastContext(ShapeType shapeType, FluidHandling fluidHandling, Entity entity) {
        return new RaycastContext(startVec, endVec, shapeType, fluidHandling, entity);
    }

    public static void update(Entity entity, double sqDistance, float tickDelta) {
        final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        final Vec3d eyePos = entity.getCameraPosVec(tickDelta);
        startVec = camera.getPos();
        direction = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());

        final Vec3d offset = startVec.subtract(eyePos);
        final Vec3d footPoint = MathUtils.getIntersectionPoint(Vec3d.ZERO, direction, offset, direction);
        if (footPoint.lengthSquared() > sqDistance) {
            startVec = eyePos;
            direction = entity.getRotationVec(tickDelta);
            endVec = startVec.add(direction.multiply(Math.sqrt(sqDistance)));
            return;
        } else if (offset.lengthSquared() > sqDistance) {
            startVec = startVec.add(direction.multiply(offset.distanceTo(footPoint) - Math.sqrt(sqDistance - footPoint.lengthSquared())));
        }
        endVec = eyePos.add(footPoint.add(direction.multiply(Math.sqrt(sqDistance - footPoint.lengthSquared()))));
    }
}
