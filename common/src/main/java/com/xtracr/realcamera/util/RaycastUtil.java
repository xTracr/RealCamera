package com.xtracr.realcamera.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RaycastUtil {
    /**
     * Updates raycast start and end points ensuring:
     * <li> End point is on camera's viewing direction
     * <li> Square distance from entity's eye to end point ≤ sqDistance
     */
    public static Pair<Vec3, Vec3> getFromAndTo(Entity entity, double sqDistance, float partialTicks) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 eyePos = entity.getEyePosition(partialTicks);
        Vec3 from = camera.position();
        Vec3 direction = Vec3.directionFromRotation(camera.xRot(), camera.yRot());
        Vec3 offset = from.subtract(eyePos);
        Vec3 footPoint = MathUtil.getIntersectionPoint(Vec3.ZERO, direction, offset, direction);
        if (footPoint.lengthSqr() > sqDistance) {
            from = eyePos;
            direction = entity.getViewVector(partialTicks);
            return Pair.of(from, from.add(direction.scale(Math.sqrt(sqDistance))));
        } else if (offset.lengthSqr() > sqDistance) {
            from = from.add(direction.scale(offset.distanceTo(footPoint) - Math.sqrt(sqDistance - footPoint.lengthSqr())));
        }
        return Pair.of(from, eyePos.add(footPoint.add(direction.scale(Math.sqrt(sqDistance - footPoint.lengthSqr())))));
    }
}
