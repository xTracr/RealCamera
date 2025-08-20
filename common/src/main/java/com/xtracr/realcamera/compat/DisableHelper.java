package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class DisableHelper {
    private static final Predicate<Player> FALSE = player -> false;
    private static final Map<String, Entry> entries = new HashMap<>();
    public static final Entry MAIN_FEATURE = new Entry("mainFeature", player -> player.isSleeping() || player.isSpectator());
    public static final Entry RENDER_MODEL = new Entry("renderModel", Player::isScoping);
    public static final Entry RENDER_HANDS = new Entry("renderHands", player -> RealCameraCore.isRendering());
    public static int exitTick = 0;
    private static ModConfig config = ConfigFile.config();
    static {    
        MAIN_FEATURE.registerOrInBinding(player -> config.disableWhenSneaking() && player.isCrouching());
        MAIN_FEATURE.registerOrInClassic(player -> config.getDisableWhenSneaking() && player.isCrouching());
        MAIN_FEATURE.registerOrInBinding(player -> config.disableWhenSwimming() && swimmingRecently(player, config.swimOutTick()));
        MAIN_FEATURE.registerOrInClassic(player -> config.getDisableWhenSwimming() && swimmingRecently(player, config.getSwimOutTick()));
        MAIN_FEATURE.registerOr(player -> {
            String mainHand = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            String offHand = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
            for (String item : config.getDisableMainFeatureItems())
                if (simpleWildcardMatch(mainHand, item) || simpleWildcardMatch(offHand, item))
                    return true;
            return false;
        });
        RENDER_MODEL.registerOr(player -> {
            String mainHand = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            String offHand = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
            for (String item : config.getDisableRenderItems())
                if (simpleWildcardMatch(mainHand, item) || simpleWildcardMatch(offHand, item))
                    return true;
            return false;
            });
    }

    private static boolean swimmingRecently(Player player, int disableSwimOutTick) {
        if (player.isSwimming()) {
            exitTick = player.tickCount;
            return true;
        }
        if (exitTick > 0 && !player.isSwimming()) {
            int elapsedTicks = player.tickCount - exitTick;
            if (elapsedTicks <= disableSwimOutTick) {
                return true;
            }
            exitTick = 0;
        }
        return false;
    }
    
    @Deprecated
    public static void registerOr(String name, Predicate<LivingEntity> predicate) {
        entries.get(name).registerOr(predicate::test);
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
        protected Predicate<Player> predicateInClassic;
        protected Predicate<Player> predicateInBinding;

        protected Entry(String name, Predicate<Player> predicate) {
            this(name, predicate, predicate);
        }

        protected Entry(String name, Predicate<Player> predicateInClassic, Predicate<Player> predicateInBinding) {
            this.predicateInClassic = predicateInClassic;
            this.predicateInBinding = predicateInBinding;
            entries.put(name, this);
        }

        public void registerOr(Predicate<Player> predicate) {
            registerOrInClassic(predicate);
            registerOrInBinding(predicate);
        }

        public void registerOrInClassic(Predicate<Player> predicate) {
            this.predicateInClassic = this.predicateInClassic.or(predicate);
        }

        public void registerOrInBinding(Predicate<Player> predicate) {
            this.predicateInBinding = this.predicateInBinding.or(predicate);
        }

        public boolean disabled(Entity cameraEntity) {
            if (!(cameraEntity instanceof Player player)) return false;
            return config.isClassic() ? predicateInClassic.test(player) : predicateInBinding.test(player);
        }
    }
}