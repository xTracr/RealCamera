package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class DisableHelper {
    private static final Predicate<Player> FALSE = player -> false;
    private static final Map<String, Entry> entries = new HashMap<>();
    public static final Entry MAIN_FEATURE = new Entry("mainFeature", player -> player.isSleeping() || player.isSpectator());
    public static final Entry RENDER_MODEL = new Entry("renderModel", FALSE, Player::isScoping);
    public static final Entry RENDER_HANDS = new Entry("renderHands", player -> RealCameraCore.isRendering());
    public static int exitTick = 0;

    static {
        MAIN_FEATURE.registerOrInBinding(player -> ConfigFile.config().bindingDisableWhenSneaking() && player.isCrouching());
        MAIN_FEATURE.registerOrInClassic(player -> ConfigFile.config().classicDisableWhenSneaking() && player.isCrouching());
        MAIN_FEATURE.registerOrInBinding(player -> ConfigFile.config().bindingDisableWhenSwimming() && swimmingRecently(player, ConfigFile.config().getBindingSwimOutTick()));
        MAIN_FEATURE.registerOrInClassic(player -> ConfigFile.config().classicDisableWhenSwimming() && swimmingRecently(player, ConfigFile.config().getClassicSwimOutTick()));
        MAIN_FEATURE.registerOrInBinding(player -> {
            Item mainHand = player.getMainHandItem().getItem();
            Item offHand = player.getOffhandItem().getItem();
            for (String pattern : ConfigFile.config().getDisableMainFeatureItems())
                if (matchesItemPattern(mainHand, pattern) || matchesItemPattern(offHand, pattern))
                    return true;
            return false;
        });
        RENDER_MODEL.registerOrInBinding(player -> {
            Item mainHand = player.getMainHandItem().getItem();
            Item offHand = player.getOffhandItem().getItem();
            for (String pattern : ConfigFile.config().getDisableMainFeatureItems())
                if (matchesItemPattern(mainHand, pattern) || matchesItemPattern(offHand, pattern))
                    return true;
            return false;
        });
    }

    private static boolean swimmingRecently(Player player, int swimOutTick) {
        if (player.isSwimming()) {
            exitTick = player.tickCount;
            return true;
        }
        if (exitTick > 0 && !player.isSwimming()) {
            int elapsedTicks = player.tickCount - exitTick;
            if (elapsedTicks <= swimOutTick) {
                return true;
            }
            exitTick = 0;
        }
        return false;
    }

    @Deprecated
    public static void registerOr(String name, Predicate<LivingEntity> predicate) {
        entries.get(name).registerOrInBinding(predicate::test);
    }

    public static boolean matchesItemPattern(Item item, String pattern) {
        if (pattern.startsWith("#")) {
            String tagId = pattern.substring(1);
            TagKey<Item> itemTag = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.tryParse(tagId));
            return BuiltInRegistries.ITEM.getTag(itemTag)
                .map(tag -> {
                    ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(item);
                    ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), itemLocation);
                    Optional<Holder.Reference<Item>> itemRef = BuiltInRegistries.ITEM.getHolder(itemKey);
                    return itemRef.map(tag::contains).orElse(false);
                })
                .orElse(false);
        }
        return simpleWildcardMatch(BuiltInRegistries.ITEM.getKey(item).toString(), pattern);
    }

    public static boolean simpleWildcardMatch(String text, String pattern) {
        if (pattern.isEmpty()) return text.isEmpty();
        if (pattern.equals(text)) return true;
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
            return ConfigFile.config().isClassic() ? predicateInClassic.test(player) : predicateInBinding.test(player);
        }
    }
}
