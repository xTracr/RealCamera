package com.xtracr.realcamera.compat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import com.xtracr.realcamera.utils.ClassUtils;

public class DoABarrelRollCompat {
    
    public static final boolean loaded = ClassUtils.isLoaded("nl.enjarai.doabarrelroll.DoABarrelRollClient");

    private static final Optional<Class<?>> modConfigClass;
    private static final Optional<Object> modConfigInstance;
    private static final Optional<Method> getModEnabled;

    static {
        if (loaded) {
            modConfigClass = ClassUtils.getClass("nl.enjarai.doabarrelroll.config.ModConfig");
            final Optional<Field> instanceField = ClassUtils.getField(modConfigClass, "INSTANCE");
            modConfigInstance = ClassUtils.getFieldValue(instanceField, null);
            getModEnabled = ClassUtils.getMethod(modConfigClass, "getModEnabled");
        } else {
            modConfigClass = Optional.empty();
            modConfigInstance = Optional.empty();
            getModEnabled = Optional.empty();
        }
    }

    public static boolean modEnabled() {
        return loaded && getModEnabled.map(mtd -> {
            try {
                return (boolean)mtd.invoke(modConfigInstance.get());
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                return null;
            }
        }).orElse(false); 
    }

}
