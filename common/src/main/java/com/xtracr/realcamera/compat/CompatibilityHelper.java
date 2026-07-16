package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

public final class CompatibilityHelper {
    public static boolean isRenderInScreen;
    private static PlatformHelper platformHelper;
    private static Method NEA_playerTransformer_setDeltaTick;
    private static Field NEA_NEAnimationsLoader_INSTANCE;
    private static Field NEA_NEAnimationsLoader_playerTransformer;
    private static Field SBW_ClientEventHandler_zoomTime;
    private static Class<?> SBW_VehicleEntity;

    public static void initialize(PlatformHelper platformHelper) {
        CompatibilityHelper.platformHelper = platformHelper;
        LegacyBindingMode.register();
        if (isModLoaded("yes_steve_model")) YSMCompat.register();
        if (isModLoaded("dragonsurvival")) DragonSurvivalCompat.register();
        if (isModLoaded("freecam")) try {
            Class<?> FC_Freecam = Class.forName("net.xolt.freecam.Freecam");
            Method FC_Freecam_isEnabled = FC_Freecam.getDeclaredMethod("isEnabled");
            DisableHelper.MAIN_FEATURE.registerOr(_ -> {
                try {
                    return (boolean) FC_Freecam_isEnabled.invoke(null);
                } catch (Exception ignored) {
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
        if (isModLoaded("superbwarfare")) try {
            Class<?> SBW_ClientEventHandler = Class.forName("com.atsuishio.superbwarfare.event.ClientEventHandler");
            SBW_ClientEventHandler_zoomTime = SBW_ClientEventHandler.getDeclaredField("zoomTime");
            SBW_VehicleEntity = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            DisableHelper.MAIN_FEATURE.registerOrInBinding(_ -> CompatibilityHelper.SBW_gunsIsZooming());
            DisableHelper.MAIN_FEATURE.registerOrInBinding(CompatibilityHelper::SBW_isDrivingVehicle);
        } catch (Exception e) {
            RealCamera.LOGGER.warn("Compatibility with SuperbWarfare is outdated: [{}] {}", e.getClass().getName(), e.getMessage());
        }
        if (isModLoaded("entity_model_features")) try {
            Class<?> EMF_EMFAnimationApi = Class.forName("traben.entity_model_features.EMFAnimationApi");
            if ((int) EMF_EMFAnimationApi.getMethod("getApiVersion").invoke(null) >= 9) {
                Function<Object, Boolean> function = _ -> isRenderInScreen;
                Method EMF_registerPauseCondition = EMF_EMFAnimationApi.getMethod("registerPauseCondition", Function.class);
                EMF_registerPauseCondition.invoke(null, function);
            } else {
                throw new IllegalStateException("EntityModelFeatures API is outdated, players‘s heads in the modelView may flicker");
            }
        } catch (Exception e) {
            RealCamera.LOGGER.warn("Compatibility with EntityModelFeatures is outdated: [{}] {}", e.getClass().getName(), e.getMessage());
        }
    }

    private static boolean SBW_gunsIsZooming() {
        try {
            return SBW_ClientEventHandler_zoomTime.getDouble(null) > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean SBW_isDrivingVehicle(Player player) {
        try {
            Entity vehicle = player.getVehicle();
            return vehicle != null && SBW_VehicleEntity.isAssignableFrom(vehicle.getClass());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void NEA_setDeltaTick(float partialTicks) {
        if (NEA_playerTransformer_setDeltaTick != null) try {
            Object INSTANCE = NEA_NEAnimationsLoader_INSTANCE.get(null);
            Object playerTransformer = NEA_NEAnimationsLoader_playerTransformer.get(INSTANCE);
            NEA_playerTransformer_setDeltaTick.invoke(playerTransformer, partialTicks);
        } catch (Exception ignored) {
        }
    }

    public static boolean isModLoaded(String modId) {
        return platformHelper.isModLoaded(modId);
    }
}
