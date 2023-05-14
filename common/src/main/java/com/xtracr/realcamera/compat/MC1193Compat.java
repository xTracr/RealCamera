package com.xtracr.realcamera.compat;

import java.lang.reflect.Field;
import java.util.Optional;

import com.xtracr.realcamera.utils.ReflectUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class MC1193Compat {

    public static final boolean is1193;

    private static final Optional<Field> limbAngle;
    private static final Optional<Field> limbDistance;
    private static final Optional<Field> lastLimbDistance;

    static {
        int last = 4;
        try {
            last = ((SemanticVersion)FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata()
                .getVersion()).getVersionComponent(2);
        } catch (Throwable throwable) {

        }
        is1193 = last == 3;
        if (is1193) {
            final Optional<Class<?>> livingEntityClass = Optional.of(LivingEntity.class);
            limbAngle = ReflectUtils.getField(livingEntityClass, "field_6249");
            limbDistance = ReflectUtils.getField(livingEntityClass, "field_6225");
            lastLimbDistance = ReflectUtils.getField(livingEntityClass, "field_6211");
        } else {
            limbAngle = Optional.empty();
            limbDistance = Optional.empty();
            lastLimbDistance = Optional.empty();
        }
    }

    public static float getLimbAnimatorSpeed(float tickDelta, LivingEntity entity) {
        final float limbDistanceValue = (float)ReflectUtils.getFieldValue(limbDistance, entity).get();
        final float lastLimbDistanceValue = (float)ReflectUtils.getFieldValue(lastLimbDistance, entity).get();
        return MathHelper.lerp(tickDelta, lastLimbDistanceValue, limbDistanceValue);
    }

    public static float getLimbAnimatorPos(float tickDelta, LivingEntity entity) {
        final float limbAngleValue = (float)ReflectUtils.getFieldValue(limbAngle, entity).get();
        final float limbDistanceValue = (float)ReflectUtils.getFieldValue(limbDistance, entity).get();
        return limbAngleValue - limbDistanceValue * (1.0f - tickDelta);
    }
}
