package com.xtracr.realcamera.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import com.xtracr.realcamera.utils.ClassUtils;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class PehkuiCompat {

    public static final boolean loaded = ClassUtils.isLoaded("virtuoel.pehkui.Pehkui");

    private static final Optional<Class<?>> scaleUtilsClass;
    private static final Optional<Method> getModelWidthScale;
    private static final Optional<Method> getModelHeightScale;
    
    static {
        if (loaded) {
            scaleUtilsClass = ClassUtils.getClass("virtuoel.pehkui.util.ScaleUtils");
            getModelWidthScale = ClassUtils.getMethod(scaleUtilsClass, "getModelWidthScale", Entity.class, float.class);
            getModelHeightScale = ClassUtils.getMethod(scaleUtilsClass, "getModelHeightScale", Entity.class, float.class);
        } else {
            scaleUtilsClass = Optional.empty();
            getModelWidthScale = Optional.empty();
            getModelHeightScale = Optional.empty();
        }
    }

    public static void scaleMatrices(MatrixStack matrixStack, Entity entity, float tickDelta) {
        if (!loaded) return;
        final float widthScale = getModelScaleValue(getModelWidthScale, entity, tickDelta);
        final float heightScale = getModelScaleValue(getModelHeightScale, entity, tickDelta);
        matrixStack.peek().getPositionMatrix().scale(widthScale, heightScale, widthScale);
    }

    public static Vec3d scaleVec3d(Vec3d vec3d, Entity entity, float tickDelta) {
        if (!loaded) return vec3d;
        final float widthScale = getModelScaleValue(getModelWidthScale, entity, tickDelta);
        final float heightScale = getModelScaleValue(getModelHeightScale, entity, tickDelta);
        return vec3d.multiply(widthScale, heightScale, widthScale);
    }
    
    private static float getModelScaleValue(Optional<Method> method, Entity entity, float tickDelta) {
        return method.map(mtd -> {
            try {
                return (float)mtd.invoke(null, entity, tickDelta);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                return null;
            }
        }).orElse(1.0F);
    }

}
