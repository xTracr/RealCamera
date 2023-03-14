package com.xtracr.betterfpcam.config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

public class ConfigController {

    public static final ConfigController configController; 
    public static final ForgeConfigSpec forgeConfigSpec;

    static {
        Pair<ConfigController, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ConfigController::new); 
        configController = pair.getLeft();
        forgeConfigSpec = pair.getRight();
    }

    private static final double minVALUE = -64.0D;
    private static final double maxVALUE = 64.0D;

    private final BooleanValue enabled;
    private final BooleanValue classic;
    private final BooleanValue thirdPersonMode;
    private final DoubleValue scale;
    private final DoubleValue cameraX;
    private final DoubleValue cameraY;
    private final DoubleValue cameraStep;
    private final DoubleValue centerY;
    private final DoubleValue centerStep;

    public ConfigController(ForgeConfigSpec.Builder builder) {

        this.enabled = builder.comment("Whether the Mod's main function is enabled")
            .translation("translation.xtracr_betterfpcam_enabled")
            .define("xtracr_betterfpcam_enabled", false);
        this.classic = builder.comment("Whether the camera is in classic mode or binding mode")
            .translation("translation.xtracr_betterfpcam_classic")
            .define("xtracr_betterfpcam_classic", true);
        this.thirdPersonMode = builder.comment("Whether Third Person Rendering Mode is enabled")
            .translation("translation.xtracr_betterfpcam_thirdpersonmode")
            .define("xtracr_betterfpcam_thirdpersonmode", false);
        
        this.scale = builder.comment("Control the size of the config values")
            .translation("translation.xtracr_betterfpcam_scale")
            .defineInRange("ClassicModeConfig.xtracr_betterfpcam_scale", 0.0625D, 0.0D, 8.0D);
        this.cameraX = builder.comment("Camera's X relative to the rotation center")
            .translation("translation.xtracr_betterfpcam_cameraX")
            .defineInRange("ClassicModeConfig.xtracr_betterfpcam_cameraX", 3.25D, minVALUE, maxVALUE);
        this.cameraY = builder.comment("Camera's Y relative to the rotation center")
            .translation("translation.xtracr_betterfpcam_cameraY")
            .defineInRange("ClassicModeConfig.xtracr_betterfpcam_cameraY", 2.0D, minVALUE, maxVALUE);
        this.cameraStep = builder.comment("The length of the camera adjustment per step")
            .translation("translation.xtracr_betterfpcam_camerastep")
            .defineInRange("ClassicModeConfig.xtracr_betterfpcam_camerastep", 0.25D, minVALUE, maxVALUE);
        this.centerY = builder.comment("Rotation center's Y relative to the head")
            .translation("translation.xtracr_betterfpcam_centerY")
            .defineInRange("ClassicModeConfig.xtracr_betterfpcam_centerY", -3.4D, minVALUE, maxVALUE);
        this.centerStep = builder.comment("The length of the rotation center adjustment per step")
            .translation("translation.xtracr_betterfpcam_centerstep")
            .defineInRange("ClassicModeConfig.xtracr_betterfpcam_centerstep", 0.25D, minVALUE, maxVALUE);

    }
    
    public void setDisabled() {
        this.enabled.set(false);
    }
    public void setEnabled() {
        this.enabled.set(true);
    }
    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setClassic() {
        this.classic.set(true);
    }
    public void setBinding() {
        this.classic.set(false);
    }
    public boolean isClassic() {
        return this.classic.get();
    }

    public void setThirdPerson() {
        this.thirdPersonMode.set(true);
    }
    public void setFirstPerson() {
        this.thirdPersonMode.set(false);
    }
    public boolean isThirdPersonMode() {
        return this.thirdPersonMode.get();
    }

    public double getScale() {
        return this.scale.get();
    }
    public double getCameraX() {
        return this.cameraX.get();
    }
    public double getCameraY() {
        return this.cameraY.get();
    }
    public double getCameraStep() {
        return this.cameraStep.get();
    }
    public double getCenterY() {
        return this.centerY.get();
    }
    public double getCenterStep() {
        return this.centerStep.get();
    }

    public void setCameraX(double value) {
        this.cameraX.set(value);
    }
    public void setCameraY(double value) {
        this.cameraY.set(value);
    }
    public void setCameraStep(double value) {
        this.cameraStep.set(value);
    }
    public void setCenterY(double value) {
        this.centerY.set(value);
    }
    public void setCenterStep(double value) {
        this.centerStep.set(value);
    }

    public void addCameraX() {
        this.cameraX.set(Math.min(getCameraX() + getCameraStep(), maxVALUE));
    }
    public void subCameraX() {
        this.cameraX.set(Math.max(getCameraX() - getCameraStep(), minVALUE));
    }
    public void addCameraY() {
        this.cameraY.set(Math.min(getCameraY() + getCameraStep(), maxVALUE));
    }
    public void subCameraY() {
        this.cameraY.set(Math.max(getCameraY() - getCameraStep(), minVALUE));
    }
    public void addCenterY() {
        this.centerY.set(Math.min(getCenterY() + getCenterStep(), maxVALUE));
    }
    public void subCenterY() {
        this.centerY.set(Math.max(getCenterY() - getCenterStep(), minVALUE));
    }
}
