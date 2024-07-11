package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class DisableHelper {
    private static final Map<String, Predicate<LivingEntity>> predicates = new HashMap<>();

    public static void initialize() {
        registerOr("mainFeature", LivingEntity::isSleeping);
        registerOr("renderModel", entity -> entity instanceof Player player && player.isScoping());
        registerOr("renderModel", entity -> ConfigFile.config().getDisableRenderItems().contains(BuiltInRegistries.ITEM.getKey(entity.getMainHandItem().getItem()).toString()));
        registerOr("renderModel", entity -> ConfigFile.config().getDisableRenderItems().contains(BuiltInRegistries.ITEM.getKey(entity.getOffhandItem().getItem()).toString()));
    }

    public static void registerOr(String type, Predicate<LivingEntity> predicate) {
        predicates.merge(type, predicate, Predicate::or);
    }

    public static boolean isDisabled(String type, Entity cameraEntity) {
        Predicate<LivingEntity> predicate = predicates.get(type);
        if (ConfigFile.config().isClassic() || predicate == null) return false;
        return cameraEntity instanceof LivingEntity entity && predicate.test(entity);
    }
}
