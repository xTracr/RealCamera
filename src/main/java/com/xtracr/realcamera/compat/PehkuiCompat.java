package com.xtracr.realcamera.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class PehkuiCompat {

    private static final boolean loaded = ModUtils.isLoaded("pehkui");

    private static final Optional<Class<?>> scaleUtilsClass;
    private static final Optional<Method> getModelWidthScale;
    private static final Optional<Method> getModelHeightScale;
    
    static {
        if (loaded) {
            scaleUtilsClass = ModUtils.getClass("virtuoel.pehkui.util.ScaleUtils");
            getModelWidthScale = ModUtils.getMethod(scaleUtilsClass, "getModelWidthScale", Entity.class, float.class);
            getModelHeightScale = ModUtils.getMethod(scaleUtilsClass, "getModelHeightScale", Entity.class, float.class);
        } else {
            scaleUtilsClass = Optional.empty();
            getModelWidthScale = Optional.empty();
            getModelHeightScale = Optional.empty();
        }
    }

    public static void scaleMatrices(PoseStack matrices, Entity entity, float tickDelta) {
        if (!loaded) return;
        final float widthScale = getModelScaleValue(getModelWidthScale, entity, tickDelta);
        final float heightScale = getModelScaleValue(getModelHeightScale, entity, tickDelta);
        matrices.scale(widthScale, heightScale, widthScale);
    }

    public static Vec3 scaleVec3d(Vec3 vec3d, Entity entity, float tickDelta) {
        if (!loaded) return vec3d;
        final float widthScale = getModelScaleValue(getModelWidthScale, entity, tickDelta);
        final float heightScale = getModelScaleValue(getModelHeightScale, entity, tickDelta);
        return vec3d.multiply(widthScale, heightScale, widthScale);
    }
    
    public static <T extends Entity> Vec3 getScaledRenderOffset(EntityRenderer<T> renderer, T entity, float tickDelta) {
        return (!loaded ? renderer.getRenderOffset(entity, tickDelta) : 
            renderer.getRenderOffset(entity, tickDelta).scale(getModelScaleValue(getModelHeightScale, entity, tickDelta)));
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
