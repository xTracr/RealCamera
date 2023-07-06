package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.utils.ReflectUtils;

import java.lang.reflect.Method;
import java.util.Optional;

public class DoABarrelRollCompat {
    public static final boolean loaded = ReflectUtils.isLoaded("nl.enjarai.doabarrelroll.DoABarrelRollClient");

    private static final Optional<Object> modConfigInstance;
    private static final Optional<Method> getModEnabled;

    static {
        if (loaded) {
            final Optional<Class<?>> modConfigClass = ReflectUtils.getClass("nl.enjarai.doabarrelroll.config.ModConfig");
            modConfigInstance = ReflectUtils.getFieldValue(ReflectUtils.getField(modConfigClass, "INSTANCE"), null);
            getModEnabled = ReflectUtils.getMethod(modConfigClass, "getModEnabled");
        } else {
            modConfigInstance = Optional.empty();
            getModEnabled = Optional.empty();
        }
    }

    public static boolean modEnabled() {
        return loaded && (boolean) ReflectUtils.invokeMethod(getModEnabled, modConfigInstance.get()).orElse(false);
    }
}
