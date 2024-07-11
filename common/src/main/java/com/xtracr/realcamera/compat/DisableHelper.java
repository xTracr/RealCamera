package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class DisableHelper {
    private static final Map<String, Predicate<LivingEntity>> predicates = new HashMap<>();
    private static final String EXPOSURE_CAMERA = "exposure:camera";

    public static void initialize() {
        registerOr("mainFeature", LivingEntity::isSleeping);
        registerOr("renderModel", entity -> entity instanceof Player player && player.isScoping());
        registerOr("renderModel", entity -> ConfigFile.config().getDisableRenderItems().contains(BuiltInRegistries.ITEM.getKey(entity.getMainHandItem().getItem()).toString()));
        registerOr("renderModel", entity -> ConfigFile.config().getDisableRenderItems().contains(BuiltInRegistries.ITEM.getKey(entity.getOffhandItem().getItem()).toString()));
        registerOr("renderModel", entity -> {
            if (CompatibilityHelper.Exposure_CameraItem_isActive == null) return false;
            final ItemStack itemStack;
            if (EXPOSURE_CAMERA.equals(BuiltInRegistries.ITEM.getKey(entity.getMainHandItem().getItem()).toString())) itemStack = entity.getMainHandItem();
            else if (EXPOSURE_CAMERA.equals(BuiltInRegistries.ITEM.getKey(entity.getOffhandItem().getItem()).toString())) itemStack = entity.getOffhandItem();
            else return false;
            try {
                return (boolean) CompatibilityHelper.Exposure_CameraItem_isActive.invoke(itemStack.getItem(), itemStack);
            } catch (Exception ignored) {
                return false;
            }
        });
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
