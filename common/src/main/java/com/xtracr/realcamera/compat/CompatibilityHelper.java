package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCameraCore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class CompatibilityHelper {
    private static Class<?> NEA_NEAnimationsLoader = null;
    private static Method NEA_playerTransformer_setDeltaTick = null;

    @SuppressWarnings("unchecked")
    public static void initialize() {
        if (isClassLoaded("net.bettercombat.BetterCombat")) try {
            Class<?> BetterCombat_CompatibilityFlags = Class.forName("net.bettercombat.compatibility.CompatibilityFlags");
            Field firstPersonRenderField = BetterCombat_CompatibilityFlags.getField("firstPersonRender");
            Supplier<Boolean> firstPersonRender = (Supplier<Boolean>) firstPersonRenderField.get(null);
            Supplier<Boolean> newFirstPersonRender = () -> firstPersonRender.get() && !RealCameraCore.isRendering();
            firstPersonRenderField.set(null, newFirstPersonRender);
        } catch (Exception ignored) {
        }
        if (isClassLoaded("dev.tr7zw.notenoughanimations.versionless.NEABaseMod")) try {
            NEA_NEAnimationsLoader = Class.forName("dev.tr7zw.notenoughanimations.NEAnimationsLoader");
            Class<?> NEA_PlayerTransformer = Class.forName("dev.tr7zw.notenoughanimations.logic.PlayerTransformer");
            NEA_playerTransformer_setDeltaTick = NEA_PlayerTransformer.getDeclaredMethod("setDeltaTick", float.class);
        } catch (Exception ignored) {
        }
    }

    public static void NEA_setDeltaTick(float tickDelta) {
        if (NEA_NEAnimationsLoader != null) try {
            Object NEA_NEAnimationsLoader_INSTANCE = NEA_NEAnimationsLoader.getDeclaredField("INSTANCE").get(null);
            Object NEA_playerTransformer = NEA_NEAnimationsLoader.getDeclaredField("playerTransformer").get(NEA_NEAnimationsLoader_INSTANCE);
            NEA_playerTransformer_setDeltaTick.invoke(NEA_playerTransformer, tickDelta);
        } catch (Exception ignored) {
        }
    }

    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
