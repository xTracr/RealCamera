package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class DisableHelper {
    private static final Map<String, Predicate<LivingEntity>> predicates = new HashMap<>();

    public static void initialize() {
        registerOr("disableMod", LivingEntity::isSleeping);
        registerOr("disableRender", entity -> entity instanceof PlayerEntity player && player.isUsingSpyglass());
        registerOr("disableRender", entity -> config().getDisableRenderItems().contains(Registries.ITEM.getId(entity.getMainHandStack().getItem()).toString()));
        registerOr("disableRender", entity -> config().getDisableRenderItems().contains(Registries.ITEM.getId(entity.getOffHandStack().getItem()).toString()));
    }

    public static void registerOr(String type, Predicate<LivingEntity> predicate) {
        predicates.compute(type, (key, oldPredicate) -> oldPredicate != null ? predicate.or(oldPredicate) : predicate);
    }

    public static boolean check(String type, Entity cameraEntity) {
        Predicate<LivingEntity> predicate = predicates.get(type);
        if (config().isClassic() || predicate == null) return false;
        return cameraEntity instanceof LivingEntity entity && predicate.test(entity);
    }

    private static ModConfig config() {
        return ConfigFile.modConfig;
    }
}
