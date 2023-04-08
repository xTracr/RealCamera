package com.xtracr.realcamera.config;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

public class ModConfig {

    public static final double minVALUE = -64.0D;
    public static final double maxVALUE = 64.0D;

    public General general = new General();
    public BindingMode bindingMode = new BindingMode();
    public ClassicMode classicMode = new ClassicMode();
    public Compats compats = new Compats();
    public Disables disables = new Disables();

    public class General {

        public boolean enabled = false;
        public boolean classic = false;
        public boolean renderModel = true;
        public double cameraStep = 0.25D;
        public double scale = 1.0D;

        private void clamp() {
            cameraStep = clampValue(cameraStep, 0.0D, maxVALUE);
            scale = clampValue(scale, 0.0D, maxVALUE);
        }

    }

    public class BindingMode {

        public AcceptableModelParts modelPart = AcceptableModelParts.HEAD;
        public boolean bindDirection = true;
        public boolean lockRolling = false;
        public double bindingX = 3.25D;
        public double bindingY = 2.0D;
        public double bindingZ = 0.0D;
        public double pitch = 0.0D;
        public double yaw = 0.0D;
        public double roll = 0.0D;

        private void clamp() {
            if (!(modelPart instanceof AcceptableModelParts)) { modelPart = AcceptableModelParts.HEAD; }
            bindingX = clampValue(bindingX, minVALUE, maxVALUE);
            bindingY = clampValue(bindingY, minVALUE, maxVALUE);
            bindingZ = clampValue(bindingZ, minVALUE, maxVALUE);
            pitch = clampValue(pitch, -180.0D, 180.0D);
            yaw = clampValue(yaw, -180.0D, 180.0D);
            roll = clampValue(roll, -180.0D, 180.0D);
        }

    }

    public class ClassicMode {

        public double cameraX = 3.25D;
        public double cameraY = 2.0D;
        public double cameraZ = 0.0D;
        public double centerY = -3.4D;
        public double centerStep = 0.25D;

        private void clamp() {
            cameraX = clampValue(cameraX, minVALUE, maxVALUE);
            cameraY = clampValue(cameraY, minVALUE, maxVALUE);
            cameraZ = clampValue(cameraZ, minVALUE, maxVALUE);
            centerY = clampValue(centerY, minVALUE, maxVALUE);
            centerStep = clampValue(centerStep, 0.0D, maxVALUE);
        }

    }

    public class Compats {

        public boolean pehkui = true;

    }
    
    public class Disables {

        public boolean fallFlying = true;
        public boolean swiming = false;
        public boolean crawling = false;
        public boolean sneaking = false;
        public boolean sleeping = false;
        public boolean scoping = true;
    
    }

    public void set(ModConfig modConfig) {
        this.general = modConfig.general;
        this.bindingMode = modConfig.bindingMode;
        this.classicMode = modConfig.classicMode;
        this.compats = modConfig.compats;
        this.disables = modConfig.disables;
    }

    public void clamp() {
        this.general.clamp();
        this.bindingMode.clamp();
        this.classicMode.clamp();
    }

    private static double clampValue(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }
    
    public boolean isEnabled() {
        return this.general.enabled;
    }
    public boolean isClassic() {
        return this.general.classic;
    }
    public boolean isRendering() {
        return this.general.renderModel;
    }
    public double getCameraStep() {
        return this.general.cameraStep;
    }
    public double getScale() {
        return this.general.scale * 0.0625D;
    }

    public void setEnabled(boolean value) {
        this.general.enabled = value;
        ConfigFile.save();
    }
    public void setClassic(boolean value) {
        this.general.classic = value;
        ConfigFile.save();
    }
    public void setRendering(boolean value) {
        this.general.renderModel = value;
        ConfigFile.save();
    }
    public void setCameraStep(double value) {
        this.general.cameraStep = value;
        ConfigFile.save();
    }
    public void setScale(double value) {
        this.general.scale = value;
        ConfigFile.save();
    }

    public boolean isDisabledWhen(ClientPlayerEntity player) {
        return (player.isFallFlying() && this.disables.fallFlying)
            || (player.isSwimming() && this.disables.swiming)
            || (player.isCrawling() && this.disables.crawling)
            || (player.isSneaking() && this.disables.sneaking)
            || (player.isSleeping() && this.disables.sleeping);
    }
    public boolean onlyDisableRenderingWhen(ClientPlayerEntity player) {
        return player.isUsingSpyglass() && this.disables.scoping;
    }

    public boolean compatPehkui() {
        return this.compats.pehkui;
    }

    // binding
    public ModelPart getModelPartFrom(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
        return this.bindingMode.modelPart.getTarget(playerModel);
    }
    public boolean isDirectionBound() {
        return this.bindingMode.bindDirection;
    }
    public boolean isRollingLocked() {
        return this.bindingMode.lockRolling;
    }
    public double getBindingX() {
        return this.bindingMode.bindingX;
    }
    public double getBindingY() {
        return this.bindingMode.bindingY;
    }
    public double getBindingZ() {
        return this.bindingMode.bindingZ;
    }
    public float getPitch() {
        return (float)this.bindingMode.pitch;
    }
    public float getYaw() {
        return (float)this.bindingMode.yaw;
    }
    public float getRoll() {
        return (float)this.bindingMode.roll;
    }

    public void setModelPart(AcceptableModelParts modelPart) {
        this.bindingMode.modelPart = modelPart;
        ConfigFile.save();
    }
    public void setBindingX(double value) {
        this.bindingMode.bindingX = value;
        ConfigFile.save();
    }
    public void setBindingY(double value) {
        this.bindingMode.bindingY = value;
        ConfigFile.save();
    }
    public void setBindingZ(double value) {
        this.bindingMode.bindingZ = value;
        ConfigFile.save();
    }
    public void setPitch(float value) {
        this.bindingMode.pitch = value;
        ConfigFile.save();
    }
    public void setYaw(float value) {
        this.bindingMode.yaw = value;
        ConfigFile.save();
    }
    public void setRoll(float value) {
        this.bindingMode.roll = value;
        ConfigFile.save();
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
        return this.classicMode.cameraX;
    }
    public double getCameraY() {
        return this.classicMode.cameraY;
    }
    public double getCameraZ() {
        return this.classicMode.cameraZ;
    }
    public double getCenterY() {
        return this.classicMode.centerY;
    }
    public double getCenterStep() {
        return this.classicMode.centerStep;
    }
    
    public void setCameraX(double value) {
        this.classicMode.cameraX = value;
        ConfigFile.save();
    }
    public void setCameraY(double value) {
        this.classicMode.cameraY = value;
        ConfigFile.save();
    }
    public void setCameraZ(double value) {
        this.classicMode.cameraZ = value;
        ConfigFile.save();
    }
    public void setCenterY(double value) {
        this.classicMode.centerY = value;
        ConfigFile.save();
    }
    public void setCenterStep(double value) {
        this.classicMode.centerStep = value;
        ConfigFile.save();
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
