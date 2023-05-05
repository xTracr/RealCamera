package com.xtracr.realcamera.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import com.xtracr.realcamera.command.ClientCommand;
import com.xtracr.realcamera.config.ConfigFile;

import net.minecraft.client.util.math.MatrixStack;

/**
 * @see com.xtracr.realcamera.api.CompatExample example
 */
public class VirtualRenderer {
    
    private static final Map<String, BiPredicate<Float, MatrixStack>> functionProvider = new HashMap<>();

    /**
     * 
     * @param modid
     * @param function {@link com.xtracr.realcamera.api.CompatExample#virtualRender See example here}
     * 
     */
    public static void register(String modid, BiPredicate<Float, MatrixStack> function) {
        functionProvider.put(modid, function);
    }

    /**
     * 
     * @param modid
     * @param function {@link com.xtracr.realcamera.api.CompatExample#virtualRender See example here}
     * @param feedback sent when command {@code \realcamera config} is executed
     * 
     */
    public static void register(String modid, BiPredicate<Float, MatrixStack> function, Supplier<String> feedback) {
        functionProvider.put(modid, function);
        ClientCommand.registerFeedback(() -> "[" + modid + "]: " + feedback.get());
    }

    /**
     * 
     * @return the value of {@link com.xtracr.realcamera.config.ModConfig.Compats#modModelPart modModelPart} option in the config
     * 
     */
    public static String getModelPartName() {
        return ConfigFile.modConfig.getModModelPartName();
    }

    public static boolean virtualRender(float tickDelta, MatrixStack matrixStack) {
        return functionProvider.get(ConfigFile.modConfig.getModelModID()).test(tickDelta, matrixStack);
    }

    public static Set<String> getRegisteredMods() {
        return functionProvider.keySet();
    }

}
