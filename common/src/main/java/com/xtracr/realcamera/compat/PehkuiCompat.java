package com.xtracr.realcamera.compat;

import java.lang.reflect.Method;
import java.util.Optional;

import com.xtracr.realcamera.utils.ReflectUtils;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public class PehkuiCompat {

    public static final boolean loaded = ReflectUtils.isLoaded("virtuoel.pehkui.Pehkui");

    private static final Optional<Method> getModelWidthScale;
    private static final Optional<Method> getModelHeightScale;

    static {
        if (loaded) {
            final Optional<Class<?>> scaleUtilsClass = ReflectUtils.getClass("virtuoel.pehkui.util.ScaleUtils");
            getModelWidthScale = ReflectUtils.getMethod(scaleUtilsClass, "getModelWidthScale", Entity.class, float.class);
            getModelHeightScale = ReflectUtils.getMethod(scaleUtilsClass, "getModelHeightScale", Entity.class, float.class);
        } else {
            getModelWidthScale = Optional.empty();
            getModelHeightScale = Optional.empty();
        }
    }

    public static void scaleMatrices(MatrixStack matrixStack, Entity entity, float tickDelta) {
        if (!loaded) return;
        final float widthScale = (float)ReflectUtils.invokeMethod(getModelWidthScale, null, entity, tickDelta).orElse(1.0F);
        final float heightScale = (float)ReflectUtils.invokeMethod(getModelHeightScale, null, entity, tickDelta).orElse(1.0F);
        matrixStack.peek().getPositionMatrix().multiply(Matrix4f.scale(widthScale, heightScale, widthScale));
    }

    public static Vec3d scaleVec3d(Vec3d vec3d, Entity entity, float tickDelta) {
        if (!loaded) return vec3d;
        final float widthScale = (float)ReflectUtils.invokeMethod(getModelWidthScale, null, entity, tickDelta).orElse(1.0F);
        final float heightScale = (float)ReflectUtils.invokeMethod(getModelHeightScale, null, entity, tickDelta).orElse(1.0F);
        return vec3d.multiply(widthScale, heightScale, widthScale);
    }

}
