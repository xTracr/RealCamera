package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.mixin.accessor.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public class CompatibilityHelper {
    private static PlatformHelper platformHelper;
    private static Class<?> NEA_NEAnimationsLoader;
    private static Method NEA_playerTransformer_setDeltaTick;
    private static Class<?> TACZ_IClientPlayerGunOperator;
    private static Method TACZ_IClientPlayerGunOperator_fromLocalPlayer;

    public static void initialize(PlatformHelper platformHelper) {
        CompatibilityHelper.platformHelper = platformHelper;
        if (isModLoaded("yes_steve_model")) RealCameraCore.setActiveRecorder(YSMCompat.INSTANCE);
        if (isModLoaded("freecam")) try {
            Class<?> FC_Freecam = Class.forName("net.xolt.freecam.Freecam");
            Method FC_Freecam_isEnabled = FC_Freecam.getDeclaredMethod("isEnabled");
            DisableHelper.MAIN_FEATURE.registerOr(player -> {
                try {
                    return (boolean) FC_Freecam_isEnabled.invoke(null);
                } catch (Exception exception) {
                    return false;
                }
            });
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Compatibility with Freecam is outdated: [{}] {}", exception.getClass().getName(), exception.getMessage());
        }
        if (isModLoaded("notenoughanimations")) try {
            NEA_NEAnimationsLoader = Class.forName("dev.tr7zw.notenoughanimations.NEAnimationsLoader");
            Class<?> NEA_PlayerTransformer = Class.forName("dev.tr7zw.notenoughanimations.logic.PlayerTransformer");
            NEA_playerTransformer_setDeltaTick = NEA_PlayerTransformer.getDeclaredMethod("setDeltaTick", float.class);
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Compatibility with Not Enough Animations is outdated: [{}] {}", exception.getClass().getName(), exception.getMessage());
        }
        if (isModLoaded("tacz")) try {
            TACZ_IClientPlayerGunOperator = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator"); 
            TACZ_IClientPlayerGunOperator_fromLocalPlayer = TACZ_IClientPlayerGunOperator.getMethod("fromLocalPlayer", LocalPlayer.class);
            DisableHelper.MAIN_FEATURE.registerOrInBinding(CompatibilityHelper::TACZ_gunsIsAiming);
        } catch (Exception e) {
            RealCamera.LOGGER.warn("TACZ is not loaded correctly: [{}] {}", e.getClass().getName(), e.getMessage());
        }
    }

    private static boolean TACZ_gunsIsAiming(Player player) {
        if (!(player instanceof LocalPlayer localPlayer)) return false;
        try {
            Object operator = TACZ_IClientPlayerGunOperator_fromLocalPlayer.invoke(null, localPlayer);
            Method getProgressMethod = TACZ_IClientPlayerGunOperator.getMethod("getClientAimingProgress", float.class);
            float aimingProgress = (float) getProgressMethod.invoke(operator, Minecraft.getInstance().getFrameTime());
            return aimingProgress > 0;
        } catch (Exception e) {
            return false;
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

    public static boolean isModLoaded(String modId) {
        return platformHelper != null && platformHelper.isModLoaded(modId);
    }
}
