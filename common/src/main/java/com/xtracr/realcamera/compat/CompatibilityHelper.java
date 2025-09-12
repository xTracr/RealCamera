package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.mixin.accessor.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CompatibilityHelper {
    private static PlatformHelper platformHelper;
    private static Method NEA_playerTransformer_setDeltaTick;
    private static Field NEA_NEAnimationsLoader_INSTANCE;
    private static Field NEA_NEAnimationsLoader_playerTransformer;
    private static Class<?> SW_ClientEventHandler;
    private static Field SW_ClientEventHandler_zoomTime;

    public static void initialize(PlatformHelper platformHelper) {
        CompatibilityHelper.platformHelper = platformHelper;
        LegacyBindingMode.register();
        if (isModLoaded("yes_steve_model")) YSMCompat.register();
        if (isModLoaded("freecam")) try {
            Class<?> FC_Freecam = Class.forName("net.xolt.freecam.Freecam");
            Method FC_Freecam_isEnabled = FC_Freecam.getDeclaredMethod("isEnabled");
            DisableHelper.MAIN_FEATURE.registerOr(player -> {
                try {
                    return (boolean) FC_Freecam_isEnabled.invoke(null);
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (Exception e) {
            RealCamera.LOGGER.warn("Compatibility with Freecam is outdated: [{}] {}", e.getClass().getName(), e.getMessage());
        }
        if (isModLoaded("notenoughanimations")) try {
            Class<?> NEA_NEAnimationsLoader = Class.forName("dev.tr7zw.notenoughanimations.NEAnimationsLoader");
            Class<?> NEA_PlayerTransformer = Class.forName("dev.tr7zw.notenoughanimations.logic.PlayerTransformer");
            NEA_playerTransformer_setDeltaTick = NEA_PlayerTransformer.getDeclaredMethod("setDeltaTick", float.class);
            NEA_NEAnimationsLoader_INSTANCE = NEA_NEAnimationsLoader.getDeclaredField("INSTANCE");
            NEA_NEAnimationsLoader_playerTransformer = NEA_NEAnimationsLoader.getDeclaredField("playerTransformer");
        } catch (Exception e) {
            RealCamera.LOGGER.warn("Compatibility with Not Enough Animations is outdated: [{}] {}", e.getClass().getName(), e.getMessage());
        }
        if(isModLoaded("superbwarfare")) try{
            SW_ClientEventHandler = Class.forName("com.atsuishio.superbwarfare.event.ClientEventHandler");
            SW_ClientEventHandler_zoomTime = SW_ClientEventHandler.getDeclaredField("zoomTime");
            DisableHelper.MAIN_FEATURE.registerOrInBinding(player -> CompatibilityHelper.SW_gunsIsZooming());
        } catch (Exception e) {
            RealCamera.LOGGER.warn("SuperbWarfare is not loaded correctly: [{}] {}", e.getClass().getName(), e.getMessage());
        }
    }

    private static boolean SW_gunsIsZooming(){
        try {
            double zoomTimeValue = SW_ClientEventHandler_zoomTime.getDouble(null);
            return zoomTimeValue > 0;
        } catch (Exception e) {
            RealCamera.LOGGER.error("Failed to access SuperbWarfare's zoomTime field", e);
            return false;
        }
    }

    public static void NEA_setDeltaTick(float deltaTick) {
        if (NEA_playerTransformer_setDeltaTick != null) try {
            Object INSTANCE = NEA_NEAnimationsLoader_INSTANCE.get(null);
            Object playerTransformer = NEA_NEAnimationsLoader_playerTransformer.get(INSTANCE);
            NEA_playerTransformer_setDeltaTick.invoke(playerTransformer, deltaTick);
        } catch (Exception ignored) {
        }
    }

    public static void forceSetCameraPos(Camera camera) {
        if (RealCameraCore.isActive() && !ConfigFile.config().isClassic()) {
            ((CameraAccessor) camera).invokeSetPosition(RealCameraCore.getCameraPos(camera.getPosition()));
        }
    }

    public static boolean isModLoaded(String modId) {
        return platformHelper != null && platformHelper.isModLoaded(modId);
    }
}
