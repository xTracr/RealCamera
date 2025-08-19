package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCameraCore;
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
    private static final Map<String, Entry> entries = new HashMap<>();
    public static final Entry CLASSIC_FEATURE = new Entry("classicFeature", player -> false);
    public static final Entry MAIN_FEATURE = new Entry("mainFeature", player -> player.isSleeping() || player.isSpectator());
    public static final Entry RENDER_MODEL = new Entry("renderModel", Player::isScoping);
    public static final Entry RENDER_HANDS = new Entry("renderHands", player -> RealCameraCore.isRendering());
    private static int exitTick = 0;
    private static int exitTickClassic = 0;
    static {
        CLASSIC_FEATURE.registerOr(player -> ConfigFile.config().getDisableWhenSwimming() && classicSwimmingRecently((Player)player));          
        CLASSIC_FEATURE.registerOr(player -> ConfigFile.config().getDisableWhenSneaking() && player.isCrouching());
        MAIN_FEATURE.registerOr(player -> ConfigFile.config().disableWhenSwimming() && bindingSwimmingRecently((Player)player));
        MAIN_FEATURE.registerOr(player -> ConfigFile.config().disableWhenSneaking() && player.isCrouching());
        MAIN_FEATURE.registerOr(player -> {
            String mainHand = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            String offHand = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
            for (String item : ConfigFile.config().getDisableMainFeatureItems())
                if (simpleWildcardMatch(mainHand, item) || simpleWildcardMatch(offHand, item))
                    return true;
            return false;
        });
        RENDER_MODEL.registerOr(player -> {
            String mainHand = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            String offHand = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
            for (String item : ConfigFile.config().getDisableRenderItems())
                if (simpleWildcardMatch(mainHand, item) || simpleWildcardMatch(offHand, item))
                    return true;
            return false;
        });
    }

    @Deprecated
    public static void registerOr(String name, Predicate<LivingEntity> predicate) {
        entries.get(name).registerOr(predicate::test);
    }

    public static boolean bindingSwimmingRecently(Player player) {        
        if(ConfigFile.config().disableWhenSwimming()
        && Minecraft.getInstance() != null){
            if (player.isSwimming()){
                exitTick = player.tickCount;
                return true;
            }   
            if (exitTick > 0 && !player.isSwimming()) {
                int elapsedTicks = player.tickCount - exitTick;
                    if (elapsedTicks <= ConfigFile.config().swimOutTick()) {
                        return true;
                    }
                    exitTick = 0; 
            }}
        return false;   
    } 

    public static boolean classicSwimmingRecently(Player player) {   
        if(ConfigFile.config().getDisableWhenSwimming()
        && Minecraft.getInstance() != null){
            if (player.isSwimming()){
                exitTickClassic = player.tickCount;
                return true;
            }   
            if (exitTickClassic > 0 && !player.isSwimming()) {
                int elapsedTicksClassic = player.tickCount - exitTickClassic;
                    if (elapsedTicksClassic <= ConfigFile.config().getSwimOutTick()) {
                        return true;
                    }
                    exitTickClassic = 0; 
            }}
        return false;   
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
        protected Predicate<Player> bindingPredicate;
        protected Predicate<Player> classicPredicate;;

        protected Entry(String name, Predicate<Player> predicate) {
            this.bindingPredicate = predicate;
            this.classicPredicate = predicate;
            entries.put(name, this);
        }

        public void registerOr(Predicate<Player> predicate) {
            this.bindingPredicate = this.bindingPredicate.or(predicate);
            this.classicPredicate = this.classicPredicate.or(predicate);
        }

        public void registerOrInBindingMode(Predicate<Player> predicate) {
            if (!ConfigFile.config().isClassic()) {
                this.bindingPredicate = this.bindingPredicate.or(predicate);
            }
        }

        public void registerOrInClassicMode(Predicate<Player> predicate) {
            if (ConfigFile.config().isClassic()) {
                this.classicPredicate = this.classicPredicate.or(predicate);
            }
        }

        public boolean disabled(Entity cameraEntity) {
            if (ConfigFile.config().isClassic()) {
                return  cameraEntity instanceof Player player &&classicPredicate.test(player);
            } 
            return cameraEntity instanceof Player player &&bindingPredicate.test(player);
        }
    }
}