package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DisableHelper {
    public static final Entry MAIN_FEATURE, RENDER_MODEL, RENDER_HANDS;
    private static final Pattern MULTI_STAR = Pattern.compile("\\*+");
    private static final Predicate<Player> FALSE = _ -> false;
    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static int exitTick = 0;

    static {
        MAIN_FEATURE = new Entry("mainFeature", player -> player.isSleeping() || player.isSpectator());
        RENDER_MODEL = new Entry("renderModel", FALSE, Player::isScoping);
        RENDER_HANDS = new Entry("renderHands", _ -> RealCameraCore.isRendering());
        MAIN_FEATURE.registerOrInBinding(player -> ConfigFile.config().binding.disableWhenSneaking && player.isCrouching());
        MAIN_FEATURE.registerOrInClassic(player -> ConfigFile.config().classic.disableWhenSneaking && player.isCrouching());
        MAIN_FEATURE.registerOrInBinding(player -> ConfigFile.config().binding.disableWhenSwimming && checkCondition(player, player.isSwimming(), ConfigFile.config().binding.outTick));
        MAIN_FEATURE.registerOrInClassic(player -> ConfigFile.config().classic.disableWhenSwimming && checkCondition(player, player.isSwimming(), ConfigFile.config().classic.outTick));
        MAIN_FEATURE.registerOrInBinding(player -> ConfigFile.config().binding.disableWhenCrawling && checkCondition(player, player.isVisuallyCrawling(), ConfigFile.config().binding.outTick));
        MAIN_FEATURE.registerOrInBinding(player -> {
            Item mainHand = player.getMainHandItem().getItem();
            Item offHand = player.getOffhandItem().getItem();
            for (String pattern : ConfigFile.config().binding.disableMainFeatureItems)
                if (matchesItemPattern(mainHand, pattern) || matchesItemPattern(offHand, pattern))
                    return true;
            return false;
        });
        RENDER_MODEL.registerOrInBinding(player -> {
            Item mainHand = player.getMainHandItem().getItem();
            Item offHand = player.getOffhandItem().getItem();
            for (String pattern : ConfigFile.config().binding.disableRenderItems)
                if (matchesItemPattern(mainHand, pattern) || matchesItemPattern(offHand, pattern))
                    return true;
            return false;
        });
    }

    private static boolean checkCondition(Player player, boolean condition, int outTick) {
        if (condition) {
            exitTick = player.tickCount;
            return true;
        }
        if (exitTick > 0) {
            int elapsedTicks = player.tickCount - exitTick;
            if (elapsedTicks <= outTick) {
                return true;
            }
            exitTick = 0;
        }
        return false;
    }

    @Deprecated
    public static void registerOr(String name, Predicate<LivingEntity> predicate) {
        ENTRIES.get(name).registerOrInBinding(predicate::test);
    }

    public static boolean matchesItemPattern(Item item, String pattern) {
        if (pattern.startsWith("#")) {
            String tagId = pattern.substring(1);
            TagKey<Item> itemTag = TagKey.create(BuiltInRegistries.ITEM.key(), Identifier.parse(tagId));
            return BuiltInRegistries.ITEM.get(itemTag)
                    .map(tag -> tag.contains(BuiltInRegistries.ITEM.wrapAsHolder(item)))
                    .orElse(false);
        }
        return simpleWildcardMatch(BuiltInRegistries.ITEM.getKey(item).toString(), pattern);
    }

    public static boolean simpleWildcardMatch(String text, String pattern) {
        if (pattern.isEmpty()) return text.isEmpty();
        if (pattern.equals(text)) return true;
        String[] parts = MULTI_STAR.split(pattern);
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
            ENTRIES.put(name, this);
        }

        public void registerOr(Predicate<Player> predicate) {
            registerOrInClassic(predicate);
            registerOrInBinding(predicate);
        }

        public void registerOrInClassic(Predicate<Player> predicate) {
            if (predicateInClassic == FALSE) predicateInClassic = predicate;
            else predicateInClassic = predicateInClassic.or(predicate);
        }

        public void registerOrInBinding(Predicate<Player> predicate) {
            if (predicateInBinding == FALSE) predicateInBinding = predicate;
            else predicateInBinding = predicateInBinding.or(predicate);
        }

        public boolean disabled(Entity cameraEntity) {
            if (!(cameraEntity instanceof Player player)) return false;
            return ConfigFile.config().isClassic ? predicateInClassic.test(player) : predicateInBinding.test(player);
        }
    }
}