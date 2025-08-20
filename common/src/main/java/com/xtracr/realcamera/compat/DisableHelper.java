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
import net.minecraft.network.chat.Component;//测试

public class DisableHelper {
    private static final Map<String, Entry> entries = new HashMap<>();
    public static final Entry CLASSIC_FEATURE = new Entry("classicFeature", player -> false);
    public static final Entry MAIN_FEATURE = new Entry("mainFeature", player -> player.isSleeping() || player.isSpectator());
    public static final Entry RENDER_MODEL = new Entry("renderModel", Player::isScoping);
    public static final Entry RENDER_HANDS = new Entry("renderHands", player -> RealCameraCore.isRendering());
    public static int exitTick = 0;
    private static ModConfig config = ConfigFile.config();
    static {      
        MAIN_FEATURE.registerOr(player -> DisableWhenSneaking() && player.isCrouching());
        MAIN_FEATURE.registerOr(player ->swimmingRecently((Player)player));
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

    @Deprecated
    public static void registerOr(String name, Predicate<LivingEntity> predicate) {
        entries.get(name).registerOr(predicate::test);
    }

    private static boolean swimmingRecently(Player player) {        
        if(DisableWhenSwimming("binding") || DisableWhenSwimming("classic")) {
        if (player.isSwimming()){
            exitTick = player.tickCount;
            return true;
        }   
        if (exitTick > 0 && !player.isSwimming()) {
            int elapsedTicks = player.tickCount - exitTick;
                if (elapsedTicks <= DisableSwimOutTick()) {
                    return true;
                }
                exitTick = 0; 
        }}
        return false;
    }     
    private static boolean DisableWhenSneaking(){
        if(config.isClassic() && config.getDisableWhenSneaking()){
            return true;
        }
        if(!config.isClassic() && config.disableWhenSneaking()){
            return true;
        }
        return false;
    }
    public static boolean DisableWhenSwimming(String model) {
        return switch(model) {
            case "classic" -> config.isClassic() && config.getDisableWhenSwimming();
            case "binding" -> !config.isClassic() && config.disableWhenSwimming();
            default -> false;
        };
    }
    
    private static int DisableSwimOutTick(){
        if(DisableWhenSwimming("classic")){
            return config.getSwimOutTick();
        }
        if(DisableWhenSwimming("binding")){
            return config.swimOutTick();
        }
        return 0;
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
        protected Predicate<Player> predicate;

        protected Entry(String name, Predicate<Player> predicate) {
            this.predicate = predicate;
            entries.put(name, this);
        }

        public void registerOr(Predicate<Player> predicate) {
            this.predicate = this.predicate.or(predicate);
        }

        public boolean disabled(Entity cameraEntity) {
            return cameraEntity instanceof Player player && predicate.test(player);
        }
    }
}