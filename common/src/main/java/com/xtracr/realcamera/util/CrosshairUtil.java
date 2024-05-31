package com.xtracr.realcamera.util;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class CrosshairUtil {
    public static EntityHitResult capturedEntityHitResult;
    private static Vec3 offset = Vec3.ZERO;

    public static void translateMatrices(PoseStack matrixStack) {
        matrixStack.translate(offset.x(), -offset.y(), 0.0d);
    }

    public static void update(Minecraft client, Camera camera, Matrix4f... projectionMatrices) {
        HitResult hitResult = client.hitResult;
        offset = Vec3.ZERO;
        if (client.crosshairPickEntity != null) hitResult = capturedEntityHitResult;
        if (hitResult == null) return;
        Window window = client.getWindow();
        offset = MathUtil.projectToVec2d(hitResult.getLocation().subtract(camera.getPosition()), projectionMatrices)
                .multiply(0.5 * window.getGuiScaledWidth(), 0.5 * window.getGuiScaledHeight(), 0.0d);
    }
}
