package com.xtracr.realcamera.config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;

public class ModConfig {

    public static final ModConfig modConfig; 
    public static final ForgeConfigSpec forgeConfigSpec;

    private static final String Option = "config.option.xtracr_realcamera_";
    private static final String General = "";
    private static final String Binding = "bindingMode.";
    private static final String Classic = "classicMode.";
    private static final String Disables = "disables.";
    
    private static final double minVALUE = -64.0D;
    private static final double maxVALUE = 64.0D;

    static {
        Pair<ModConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ModConfig::new); 
        modConfig = pair.getLeft();
        forgeConfigSpec = pair.getRight();
    }

    private final BooleanValue enabled;
    private final BooleanValue classic;
    private final BooleanValue renderModel;
    private final DoubleValue cameraStep;
    private final DoubleValue scale;

    private final EnumValue<AcceptableModelParts> modelPart;
    private final BooleanValue bindDirection;
    private final BooleanValue lockRolling;
    private final DoubleValue bindingX;
    private final DoubleValue bindingY;
    private final DoubleValue bindingZ;
    private final DoubleValue pitch;
    private final DoubleValue yaw;
    private final DoubleValue roll;

    private final DoubleValue cameraX;
    private final DoubleValue cameraY;
    private final DoubleValue cameraZ;
    private final DoubleValue centerY;
    private final DoubleValue centerStep;

    private final BooleanValue fallFlying;
    private final BooleanValue swiming;
    private final BooleanValue crawling;
    private final BooleanValue sneaking;
    private final BooleanValue sleeping;
    private final BooleanValue scoping;


    public ModConfig(ForgeConfigSpec.Builder builder) {

        this.enabled = builder.comment("Whether the Mod's main function is enabled")
            .translation(Option+"enabled")
            .define(General+"enabled", false);
        this.classic = builder.comment("Whether the camera is in classic mode or binding mode")
            .translation(Option+"classic")
            .define(General+"classic", false);
        this.renderModel = builder.comment("Whether to render player model in first-person mode")
            .translation(Option+"rendermodel")
            .define(General+"rendermodel", true);
        this.cameraStep = builder.comment("The length of the camera adjustment per step")
            .translation(Option+"camerastep")
            .defineInRange(General+"camerastep", 0.25D, 0.0D, maxVALUE);
        this.scale = builder.comment("Control the size of the config values, 16 = 1 block")
            .translation(Option+"scale")
            .defineInRange(General+"scale", 1.0D, 0.0D, maxVALUE);
        
        this.modelPart = builder.comment("The model part you want to bind camera to")
            .translation(Option+"modelpart")
            .defineEnum(Binding+"modelpart", AcceptableModelParts.HEAD);
        this.bindDirection = builder.comment("Whether to bind camera direction to the model part")
            .translation(Option+"binddirection")
            .define(Binding+"binddirection", true);
        this.lockRolling = builder.comment("Whether to lock rolling of camera")
            .translation(Option+"lockrolling")
            .define(Binding+"lockrolling", false);
        this.bindingX = builder.comment("Camera's X relative to the modelPart being bound")
            .translation(Option+"bindingX")
            .defineInRange(Binding+"bindingX", 3.25D, minVALUE, maxVALUE);
        this.bindingY = builder.comment("Camera's Y relative to the modelPart being bound")
            .translation(Option+"bindingY")
            .defineInRange(Binding+"bindingY", 2.0D, minVALUE, maxVALUE);
        this.bindingZ = builder.comment("Camera's Z relative to the modelPart being bound")
            .translation(Option+"bindingZ")
            .defineInRange(Binding+"bindingZ", 0.0D, minVALUE, maxVALUE);
        this.pitch = builder.comment("The extra rotation of the camera about the X axis")
            .translation(Option+"pitch")
            .defineInRange(Binding+"pitch", 0.0D, -180.0D, 180.0D);
        this.yaw = builder.comment("The extra rotation of the camera about the Y axis")
            .translation(Option+"yaw")
            .defineInRange(Binding+"yaw", 0.0D, -180.0D, 180.0D);
        this.roll = builder.comment("The extra rotation of the camera about the Z axis")
            .translation(Option+"roll")
            .defineInRange(Binding+"roll", 0.0D, -180.0D, 180.0D);
        
        this.cameraX = builder.comment("Camera's X relative to the rotation center")
            .translation(Option+"cameraX")
            .defineInRange(Classic+"cameraX", 3.25D, minVALUE, maxVALUE);
        this.cameraY = builder.comment("Camera's Y relative to the rotation center")
            .translation(Option+"cameraY")
            .defineInRange(Classic+"cameraY", 2.0D, minVALUE, maxVALUE);
        this.cameraZ = builder.comment("Camera's Z relative to the rotation center")
            .translation(Option+"cameraZ")
            .defineInRange(Classic+"cameraZ", 0.0D, minVALUE, maxVALUE);
        this.centerY = builder.comment("Rotation center's Y relative to the head")
            .translation(Option+"centerY")
            .defineInRange(Classic+"centerY", -3.4D, minVALUE, maxVALUE);
        this.centerStep = builder.comment("The length of the rotation center adjustment per step")
            .translation(Option+"centerstep")
            .defineInRange(Classic+"centerstep", 0.25D, 0.0D, maxVALUE);

        this.fallFlying = builder.comment("")
            .translation(Option+"fallflying")
            .define(Disables+"fallflying", true);
        this.swiming = builder.comment("")
            .translation(Option+"swiming")
            .define(Disables+"swiming", false);
        this.crawling = builder.comment("")
            .translation(Option+"crawling")
            .define(Disables+"crawling", false);
        this.sneaking = builder.comment("")
            .translation(Option+"sneaking")
            .define(Disables+"sneaking", false);
        this.sleeping = builder.comment("")
            .translation(Option+"sleeping")
            .define(Disables+"sleeping", false);
        this.scoping = builder.comment("Only disable rendering player model")
            .translation(Option+"scoping")
            .define(Disables+"scoping", true);
        
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }
    public boolean isClassic() {
        return this.classic.get();
    }
    public boolean isRendering() {
        return this.renderModel.get();
    }
    public double getCameraStep() {
        return this.cameraStep.get();
    }
    public double getScale() {
        return this.scale.get() * 0.0625D;
    }

    public void setEnabled(boolean value) {
        this.enabled.set(value);
    }
    public void setClassic(boolean value) {
        this.classic.set(value);
    }
    public void setRendering(boolean value) {
        this.renderModel.set(value);
    }
    public void setCameraStep(double value) {
        this.cameraStep.set(value);
    }
    public void setScale(double value) {
        this.scale.set(value);
    }

    public boolean isDisabledWhen(LocalPlayer player) {
        return (player.isFallFlying() && this.fallFlying.get())
            || (player.isSwimming() && this.swiming.get())
            || (player.isVisuallyCrawling() && this.crawling.get())
            || (player.isCrouching() && this.sneaking.get())
            || (player.isSleeping() && this.sleeping.get());
    }
    public boolean onlyDisableRenderingWhen(LocalPlayer player) {
        return player.isScoping() && this.scoping.get();
    }

    // binding
    public ModelPart getModelPartFrom(PlayerModel<AbstractClientPlayer> playerModel) {
        return this.modelPart.get().getTarget(playerModel);
    }
    public boolean isDirectionBound() {
        return this.bindDirection.get();
    }
    public boolean isRollingLocked() {
        return this.lockRolling.get();
    }
    public double getBindingX() {
        return this.bindingX.get();
    }
    public double getBindingY() {
        return this.bindingY.get();
    }
    public double getBindingZ() {
        return this.bindingZ.get();
    }
    public float getPitch() {
        return (float)(double)this.pitch.get();
    }
    public float getYaw() {
        return (float)(double)this.yaw.get();
    }
    public float getRoll() {
        return (float)(double)this.roll.get();
    }

    public void setModelPart(AcceptableModelParts modelPart) {
        this.modelPart.set(modelPart);
    }
    public void setBindingX(double value) {
        this.bindingX.set(value);
    }
    public void setBindingY(double value) {
        this.bindingY.set(value);
    }
    public void setBindingZ(double value) {
        this.bindingZ.set(value);
    }
    public void setPitch(float value) {
        this.pitch.set((double)value);
    }
    public void setYaw(float value) {
        this.yaw.set((double)value);
    }
    public void setRoll(float value) {
        this.roll.set((double)value);
    }

    public void addBindingX() {
        setBindingX(Math.min(getBindingX() + getCameraStep(), maxVALUE));
    }
    public void subBindingX() {
        setBindingX(Math.max(getBindingX() - getCameraStep(), minVALUE));
    }
    public void addBindingY() {
        setBindingY(Math.min(getBindingY() + getCameraStep(), maxVALUE));
    }
    public void subBindingY() {
        setBindingY(Math.max(getBindingY() - getCameraStep(), minVALUE));
    }
    public void addBindingZ() {
        setBindingZ(Math.min(getBindingZ() + getCameraStep(), maxVALUE));
    }
    public void subBindingZ() {
        setBindingZ(Math.max(getBindingZ() - getCameraStep(), minVALUE));
    }

    // classic
    public double getCameraX() {
        return this.cameraX.get();
    }
    public double getCameraY() {
        return this.cameraY.get();
    }
    public double getCameraZ() {
        return this.cameraZ.get();
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
    public void setCameraZ(double value) {
        this.cameraZ.set(value);
    }
    public void setCenterY(double value) {
        this.centerY.set(value);
    }
    public void setCenterStep(double value) {
        this.centerStep.set(value);
    }

    public void addCameraX() {
        setCameraX(Math.min(getCameraX() + getCameraStep(), maxVALUE));
    }
    public void subCameraX() {
        setCameraX(Math.max(getCameraX() - getCameraStep(), minVALUE));
    }
    public void addCameraY() {
        setCameraY(Math.min(getCameraY() + getCameraStep(), maxVALUE));
    }
    public void subCameraY() {
        setCameraY(Math.max(getCameraY() - getCameraStep(), minVALUE));
    }
    public void addCameraZ() {
        setCameraZ(Math.min(getCameraZ() + getCameraStep(), maxVALUE));
    }
    public void subCameraZ() {
        setCameraZ(Math.max(getCameraZ() - getCameraStep(), minVALUE));
    }
    public void addCenterY() {
        setCenterY(Math.min(getCenterY() + getCenterStep(), maxVALUE));
    }
    public void subCenterY() {
        setCenterY(Math.max(getCenterY() - getCenterStep(), minVALUE));
    }

}
