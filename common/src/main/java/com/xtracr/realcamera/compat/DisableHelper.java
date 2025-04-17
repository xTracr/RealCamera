package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class DisableHelper {
    private static final Set<Entry> entries = new HashSet<>();
    public static final Entry MAIN_FEATURE = new Entry("mainFeature");
    public static final Entry RENDER_MODEL = new Entry("renderModel");

    public static void initialize() {
        MAIN_FEATURE.registerOr(LivingEntity::isSleeping);
        RENDER_MODEL.registerOr(entity -> entity instanceof Player player && player.isScoping());
        RENDER_MODEL.registerOr(entity -> ConfigFile.config().getDisableRenderItems().stream().anyMatch(
                pattern -> simpleWildcardMatch(BuiltInRegistries.ITEM.getKey(entity.getMainHandItem().getItem()).toString(), pattern)));
        RENDER_MODEL.registerOr(entity -> ConfigFile.config().getDisableRenderItems().stream().anyMatch(
                pattern -> simpleWildcardMatch(BuiltInRegistries.ITEM.getKey(entity.getOffhandItem().getItem()).toString(), pattern)));
    }

    @Deprecated
    public static void registerOr(String name, Predicate<LivingEntity> predicate) {
        entries.stream().filter(entry -> entry.name.equals(name)).findAny().ifPresent(entry -> entry.registerOr(predicate));
    }

    public static boolean simpleWildcardMatch(String text, String pattern) {
        if (pattern.isEmpty()) return text.isEmpty();
        String[] parts = pattern.split("\\*+");
        boolean startsWithStar = pattern.startsWith("*");
        boolean endsWithStar = pattern.endsWith("*");
        if (parts.length == 0) return true;
        int currentIndex = 0;
        if (!startsWithStar) {
            String firstPart = parts[0];
            if (!text.startsWith(firstPart)) return false;
            currentIndex = firstPart.length();
        }
        for (int i = (startsWithStar ? 0 : 1); i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            int foundIndex = text.indexOf(part, currentIndex);
            if (foundIndex == -1) return false;
            currentIndex = foundIndex + part.length();
        }
        if (!endsWithStar) {
            String lastPart = parts[parts.length - 1];
            return text.endsWith(lastPart);
        }
        return true;
    }

    public static class Entry {
        private static final Predicate<LivingEntity> FALSE = entity -> false;
        protected final String name;
        protected Predicate<LivingEntity> predicate = FALSE;

        protected Entry(String name) {
            this.name = name;
            entries.add(this);
        }

        public void registerOr(Predicate<LivingEntity> predicate) {
            this.predicate = this.predicate.or(predicate);
        }

        public boolean disabled(Entity cameraEntity) {
            if (ConfigFile.config().isClassic()) return false;
            return cameraEntity instanceof LivingEntity entity && predicate.test(entity);
        }
    }
}
