package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class DisableHelper {
    private static final Map<String, Entry> entries = new HashMap<>();
    public static final Entry MAIN_FEATURE = new Entry("mainFeature", LivingEntity::isSleeping);
    public static final Entry RENDER_MODEL = new Entry("renderModel", entity -> entity instanceof Player player && player.isScoping());
    public static final Entry RENDER_HANDS = new Entry("renderHands", entity -> RealCameraCore.isRendering());

    static {
        RENDER_MODEL.registerOr(entity -> {
            String mainHand = BuiltInRegistries.ITEM.getKey(entity.getMainHandItem().getItem()).toString();
            String offHand = BuiltInRegistries.ITEM.getKey(entity.getOffhandItem().getItem()).toString();
            for (String item : ConfigFile.config().getDisableRenderItems())
                if (simpleWildcardMatch(mainHand, item) || simpleWildcardMatch(offHand, item))
                    return true;
            return false;
        });
    }

    @Deprecated
    public static void registerOr(String name, Predicate<LivingEntity> predicate) {
        entries.get(name).registerOr(predicate);
    }

    public static boolean simpleWildcardMatch(String text, String pattern) {
        if (pattern.isEmpty()) return text.isEmpty();
        String[] parts = pattern.split("\\*+");
        if (parts.length == 0) return true;
        int currentIndex = 0;
        if (!pattern.startsWith("*")) {
            String firstPart = parts[0];
            if (!text.startsWith(firstPart)) return false;
            currentIndex = firstPart.length();
        }
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            int foundIndex = text.indexOf(part, currentIndex);
            if (foundIndex == -1) return false;
            currentIndex = foundIndex + part.length();
        }
        if (!pattern.endsWith("*")) {
            String lastPart = parts[parts.length - 1];
            return text.endsWith(lastPart);
        }
        return true;
    }

    public static class Entry {
        protected Predicate<LivingEntity> predicate;

        protected Entry(String name, Predicate<LivingEntity> predicate) {
            this.predicate = predicate;
            entries.put(name, this);
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
