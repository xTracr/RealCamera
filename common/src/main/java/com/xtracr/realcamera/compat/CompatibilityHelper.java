package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.mixin.CameraAccessor;
import net.minecraft.client.Camera;

import java.lang.reflect.Method;

public class CompatibilityHelper {
    private static Class<?> NEA_NEAnimationsLoader = null;
    private static Method NEA_playerTransformer_setDeltaTick = null;

    static {
        if (isClassLoaded("dev.tr7zw.notenoughanimations.versionless.NEABaseMod")) try {
            NEA_NEAnimationsLoader = Class.forName("dev.tr7zw.notenoughanimations.NEAnimationsLoader");
            Class<?> NEA_PlayerTransformer = Class.forName("dev.tr7zw.notenoughanimations.logic.PlayerTransformer");
            NEA_playerTransformer_setDeltaTick = NEA_PlayerTransformer.getDeclaredMethod("setDeltaTick", float.class);
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Compatibility with Not Enough Animations is outdated: [{}] {}", exception.getClass().getName(), exception.getMessage());
        }
    }

    public static void NEA_setDeltaTick(float deltaTick) {
        if (NEA_NEAnimationsLoader != null) try {
            Object NEA_NEAnimationsLoader_INSTANCE = NEA_NEAnimationsLoader.getDeclaredField("INSTANCE").get(null);
            Object NEA_playerTransformer = NEA_NEAnimationsLoader.getDeclaredField("playerTransformer").get(NEA_NEAnimationsLoader_INSTANCE);
            NEA_playerTransformer_setDeltaTick.invoke(NEA_playerTransformer, deltaTick);
        } catch (Exception ignored) {
        }
    }

    public static void forceSetCameraPos(Camera camera) {
        if (RealCameraCore.isActive() && !ConfigFile.config().isClassic()) {
            ((CameraAccessor) camera).invokeSetPosition(RealCameraCore.getCameraPos(camera.getPosition()));
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
