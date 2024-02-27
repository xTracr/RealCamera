package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.util.ReflectUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.util.Optional;

public class PehkuiCompat {
    public static final boolean loaded = ReflectUtil.isLoaded("virtuoel.pehkui.Pehkui");

    private static final Optional<Method> getModelWidthScale;
    private static final Optional<Method> getModelHeightScale;

    static {
        if (loaded) {
            final Optional<Class<?>> scaleUtilsClass = ReflectUtil.getClass("virtuoel.pehkui.util.ScaleUtils");
            getModelWidthScale = ReflectUtil.getMethod(scaleUtilsClass, "getModelWidthScale", Entity.class, float.class);
            getModelHeightScale = ReflectUtil.getMethod(scaleUtilsClass, "getModelHeightScale", Entity.class, float.class);
        } else {
            getModelWidthScale = Optional.empty();
            getModelHeightScale = Optional.empty();
        }
    }

    public static void scaleMatrices(MatrixStack matrixStack, Entity entity, float tickDelta) {
        if (!loaded) return;
        final float widthScale = (float) ReflectUtil.invokeMethod(getModelWidthScale, null, entity, tickDelta).orElse(1.0f);
        final float heightScale = (float) ReflectUtil.invokeMethod(getModelHeightScale, null, entity, tickDelta).orElse(1.0f);
        matrixStack.peek().getPositionMatrix().scale(widthScale, heightScale, widthScale);
    }

    public static Vec3d scaleVec3d(Vec3d vec3d, Entity entity, float tickDelta) {
        if (!loaded) return vec3d;
        final float widthScale = (float) ReflectUtil.invokeMethod(getModelWidthScale, null, entity, tickDelta).orElse(1.0f);
        final float heightScale = (float) ReflectUtil.invokeMethod(getModelHeightScale, null, entity, tickDelta).orElse(1.0f);
        return vec3d.multiply(widthScale, heightScale, widthScale);
    }
}
