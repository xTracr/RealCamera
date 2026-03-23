package com.xtracr.realcamera.util;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4fc;

public class CrosshairUtil {
    public static EntityHitResult capturedEntityHitResult;
    public static Vec3 offset = Vec3.ZERO;

    public static void translateMatrices(Matrix3x2fStack poseStack) {
        poseStack.translate((float) offset.x, (float) -offset.y);
    }

    public static void update(Minecraft client, Vec3 cameraPos, Matrix4fc... projectionMatrices) {
        HitResult hitResult = client.hitResult;
        offset = Vec3.ZERO;
        if (client.crosshairPickEntity != null) hitResult = capturedEntityHitResult;
        if (hitResult == null) return;
        Window window = client.getWindow();
        offset = MathUtil.projectToVec2(hitResult.getLocation().subtract(cameraPos), projectionMatrices)
                .multiply(0.5 * window.getGuiScaledWidth(), 0.5 * window.getGuiScaledHeight(), 0.0d);
    }
}
