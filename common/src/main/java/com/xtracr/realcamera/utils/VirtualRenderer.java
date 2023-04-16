package com.xtracr.realcamera.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;

import net.minecraft.client.util.math.MatrixStack;

public class VirtualRenderer {
    
    private static final ModConfig config = ConfigFile.modConfig;

    private static final Map<String, Optional<Method>> methodProvider = new HashMap<>();
    private static final Map<String, Map<String, String>> fieldNameProvider = new HashMap<>();

    /**
     * 
     * @param rendererClass
     * 
     */
    @SuppressWarnings("unchecked")
    public static void register(final Class<?> rendererClass) {
        register((String)getFieldValue(rendererClass, "modid", null), rendererClass, "virtualRender", 
            (Map<String, String>)getFieldValue(rendererClass, "nameMap", null));
    }

    /**
     * 
     * @param modid
     * @param rendererClass
     * @param methodName
     * @param nameMap
     * 
     */
    public static void register(final String modid, final Class<?> rendererClass, String methodName, @Nullable final Map<String, String> nameMap) {
        methodProvider.put(modid, ClassUtils.getMethod(Optional.of(rendererClass), methodName, float.class, MatrixStack.class));
        if (nameMap != null) fieldNameProvider.put(modid, nameMap);
    }

    /**
     * 
     * @return
     * 
     */
    public static String getModelPartName() {
        return config.getModModelPartName();
    }

    /**
     * 
     * @return
     * 
     */
    public static String getVanillaModelPartName() {
        return config.getVanillaModelPart().name();
    }

    /**
     * 
     * @return
     * 
     */
    public static String getModelPartFieldName() {
        String modid = config.getModelModID();
        String modelPartName = config.getModModelPartName();
        return (fieldNameProvider.containsKey(modid) && fieldNameProvider.get(modid).containsKey(modelPartName) ? 
            fieldNameProvider.get(modid).get(modelPartName) : modelPartName);
    }

    /**
     * 
     * @param model
     * @return 
     * 
     */
    public static Object getModelPart(final Object model) {
        return getFieldValue(model.getClass(), getModelPartFieldName(), model);
    }

    public static void virtualRender(float particalTick, MatrixStack matrixStack)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NullPointerException {
        methodProvider.get(config.getModelModID()).get().invoke(null, particalTick, matrixStack);
    }

    private static Object getFieldValue(final Class<?> renderClass, final String fieldName, @Nullable final Object object) {
        return ClassUtils.getFieldValue(ClassUtils.getField(Optional.of(renderClass), fieldName), object).get();
    }

}
