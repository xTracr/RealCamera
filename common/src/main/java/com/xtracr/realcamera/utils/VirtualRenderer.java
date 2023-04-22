package com.xtracr.realcamera.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;

import net.minecraft.client.util.math.MatrixStack;

/**
 * @see com.xtracr.realcamera.compat.CompatExample example
 */
public class VirtualRenderer {
    
    private static final ModConfig config = ConfigFile.modConfig;

    private static final Map<String, VirtualRenderFunction> functionProvider = new HashMap<>();
    private static final Map<String, Optional<Method>> methodProvider = new HashMap<>();
    private static final Map<String, Map<String, String>> fieldNameProvider = new HashMap<>();

    /**
     * 
     * @param modid
     * @param function  a {@link VirtualRenderFunction} return a boolean
     * @param nameMap   a mapping from the {@link com.xtracr.realcamera.config.ModConfig.Compats#modModelPart name} of {@code ModelPart}
     * to the name of the {@link java.lang.reflect.Field field} of {@code ModelPart} in the code.
     * {@link com.xtracr.realcamera.compat.CompatExample#nameMap See example}
     * 
     */
    public static void register(final String modid, final VirtualRenderFunction function, @Nullable final Map<String, String> nameMap) {
        functionProvider.put(modid, function);
        if (nameMap != null) fieldNameProvider.put(modid, nameMap);
    }

    /**
     * 
     * @param rendererClass containing a {@link String} {@code modid}, a {@link Method boolean Method} {@code virtualRender} 
     * and a {@link Map} {@code nameMap} from String to  String.These should all be {@code static}.
     * {@link com.xtracr.realcamera.compat.CompatExample See example}
     * 
     */
    @SuppressWarnings("unchecked")
    public static void register(final Class<?> rendererClass) {
        try {
            register((String)getFieldValue(rendererClass, "modid", null), rendererClass, "virtualRender", 
                (Map<String, String>)getFieldValue(rendererClass, "nameMap", null));
        } catch (Exception exception) {
            register((String)getFieldValue(rendererClass, "modid", null), rendererClass, "virtualRender", null);
        }
    }

    /**
     * 
     * @param modid
     * @param rendererClass containing a {@link Method boolean Method} {@code methodName}.
     * {@link com.xtracr.realcamera.compat.CompatExample See example}
     * @param methodName    {@code virtualRender} default
     * @param nameMap       a mapping from the {@link com.xtracr.realcamera.config.ModConfig.Compats#modModelPart name} of {@code ModelPart}
     * to the name of the {@link java.lang.reflect.Field field} of {@code ModelPart} in the code.
     * {@link com.xtracr.realcamera.compat.CompatExample#nameMap See example}
     * 
     */
    public static void register(final String modid, final Class<?> rendererClass, String methodName, @Nullable final Map<String, String> nameMap) {
        methodProvider.put(modid, ClassUtils.getMethod(Optional.of(rendererClass), methodName, float.class, MatrixStack.class));
        if (nameMap != null) fieldNameProvider.put(modid, nameMap);
    }

    /**
     * 
     * @return the model part name entered by the user in the config interface
     * 
     */
    public static String getModelPartName() {
        return config.getModModelPartName();
    }

    /**
     * 
     * @return the mapped model part field name
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
     * {@link #getModelPart(Object)} provides you with a convenient way to obtain model parts based on config information.
     * <p>However, It is recommended to use the {@link #getModelPartName()} method to directly obtain the config information 
     * and write a method to decide which model part to get in each case.
     * 
     * @param model player model
     * @return {@code ModelPart} obtained based on the mapped name.
     * 
     */
    public static Object getModelPart(final Object model) {
        return getFieldValue(model.getClass(), getModelPartFieldName(), model);
    }

    public static boolean virtualRender(float tickDelta, MatrixStack matrixStack) throws Exception {
        if (functionProvider.containsKey(config.getModelModID())) {
            return functionProvider.get(config.getModelModID()).virtualRender(tickDelta, matrixStack);
        } else {
            return (boolean)methodProvider.get(config.getModelModID()).get().invoke(null, tickDelta, matrixStack);
        }
    }

    public static Set<String> getFunctionsKeys() {
        return functionProvider.keySet();
    }

    public static Set<String> getMethodsKeys() {
        return methodProvider.keySet();
    }

    private static Object getFieldValue(final Class<?> renderClass, final String fieldName, @Nullable final Object object) {
        return ClassUtils.getFieldValue(ClassUtils.getField(Optional.of(renderClass), fieldName), object).get();
    }

}
