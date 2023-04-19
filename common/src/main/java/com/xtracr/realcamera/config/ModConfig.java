package com.xtracr.realcamera.config;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class ModConfig {

    public static final double minVALUE = -64.0D;
    public static final double maxVALUE = 64.0D;

    public General general = new General();
    public Binding binding = new Binding();
    public Classic classic = new Classic();
    public Compats compats = new Compats();
    public Disables disables = new Disables();

    public class General {

        public boolean enabled = false;
        public boolean classic = false;
        public boolean renderModel = true;
        public double adjustStep = 0.25D;
        public double scale = 1.0D;

        private void clamp() {
            adjustStep = MathHelper.clamp(adjustStep, 0.0D, maxVALUE);
            scale = MathHelper.clamp(scale, 0.0D, maxVALUE);
        }

    }

    public class Binding {

        public VanillaModelPart vanillaModelPart = VanillaModelPart.head;
        public boolean bindDirection = true;
        public boolean lockRolling = false;
        public double bindingX = 3.25D;
        public double bindingY = 2.0D;
        public double bindingZ = 0.0D;
        public double pitch = 0.0D;
        public double yaw = 0.0D;
        public double roll = 0.0D;

        private void clamp() {
            if (!(vanillaModelPart instanceof VanillaModelPart)) { vanillaModelPart = VanillaModelPart.head; }
            bindingX = MathHelper.clamp(bindingX, minVALUE, maxVALUE);
            bindingY = MathHelper.clamp(bindingY, minVALUE, maxVALUE);
            bindingZ = MathHelper.clamp(bindingZ, minVALUE, maxVALUE);
            pitch = MathHelper.clamp(pitch, -180.0D, 180.0D);
            yaw = MathHelper.clamp(yaw, -180.0D, 180.0D);
            roll = MathHelper.clamp(roll, -180.0D, 180.0D);
        }

    }

    public class Classic {

        public boolean adjustCamera = true;
        public double cameraX = 3.25D;
        public double cameraY = 2.0D;
        public double cameraZ = 0.0D;
        public double centerX = 0.0D;
        public double centerY = -3.4D;
        public double centerZ = 0.0D;

        private void clamp() {
            cameraX = MathHelper.clamp(cameraX, minVALUE, maxVALUE);
            cameraY = MathHelper.clamp(cameraY, minVALUE, maxVALUE);
            cameraZ = MathHelper.clamp(cameraZ, minVALUE, maxVALUE);
            centerX = MathHelper.clamp(centerX, minVALUE, maxVALUE);
            centerY = MathHelper.clamp(centerY, minVALUE, maxVALUE);
            centerZ = MathHelper.clamp(centerZ, minVALUE, maxVALUE);
        }

    }

    public class Compats {

        public boolean useModModel = false;
        public String modelModID = "minecraft";
        public String modModelPart = "head";
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
        this.binding = modConfig.binding;
        this.classic = modConfig.classic;
        this.compats = modConfig.compats;
        this.disables = modConfig.disables;
    }

    public void clamp() {
        this.general.clamp();
        this.binding.clamp();
        this.classic.clamp();
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
    public double getAdjustStep() {
        return this.general.adjustStep;
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
    public void setAdjustStep(double value) {
        this.general.adjustStep = value;
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

    public boolean isUsingModModel() {
        return this.compats.useModModel;
    }
    public String getModelModID() {
        return this.compats.modelModID;
    }
    public String getModModelPartName() {
        return this.compats.modModelPart;
    }
    public boolean compatPehkui() {
        return this.compats.pehkui;
    }

    // binding
    public VanillaModelPart getVanillaModelPart() {
        return this.binding.vanillaModelPart;
    }
    public boolean isDirectionBound() {
        return this.binding.bindDirection;
    }
    public boolean isRollingLocked() {
        return this.binding.lockRolling;
    }
    public double getBindingX() {
        return this.binding.bindingX;
    }
    public double getBindingY() {
        return this.binding.bindingY;
    }
    public double getBindingZ() {
        return this.binding.bindingZ;
    }
    public float getPitch() {
        return (float)this.binding.pitch;
    }
    public float getYaw() {
        return (float)this.binding.yaw;
    }
    public float getRoll() {
        return (float)this.binding.roll;
    }

    public void setModelPart(VanillaModelPart modelPart) {
        this.binding.vanillaModelPart = modelPart;
        ConfigFile.save();
    }
    public void setBindingX(double value) {
        this.binding.bindingX = value;
        ConfigFile.save();
    }
    public void setBindingY(double value) {
        this.binding.bindingY = value;
        ConfigFile.save();
    }
    public void setBindingZ(double value) {
        this.binding.bindingZ = value;
        ConfigFile.save();
    }
    public void setPitch(float value) {
        this.binding.pitch = value;
        ConfigFile.save();
    }
    public void setYaw(float value) {
        this.binding.yaw = value;
        ConfigFile.save();
    }
    public void setRoll(float value) {
        this.binding.roll = value;
        ConfigFile.save();
    }

    public void addBindingX() {
        setBindingX(Math.min(getBindingX() + getAdjustStep(), maxVALUE));
    }
    public void subBindingX() {
        setBindingX(Math.max(getBindingX() - getAdjustStep(), minVALUE));
    }
    public void addBindingY() {
        setBindingY(Math.min(getBindingY() + getAdjustStep(), maxVALUE));
    }
    public void subBindingY() {
        setBindingY(Math.max(getBindingY() - getAdjustStep(), minVALUE));
    }
    public void addBindingZ() {
        setBindingZ(Math.min(getBindingZ() + getAdjustStep(), maxVALUE));
    }
    public void subBindingZ() {
        setBindingZ(Math.max(getBindingZ() - getAdjustStep(), minVALUE));
    }

    // classic
    public boolean isAdjustCamera() {
        return this.classic.adjustCamera;
    }
    public double getCameraX() {
        return this.classic.cameraX;
    }
    public double getCameraY() {
        return this.classic.cameraY;
    }
    public double getCameraZ() {
        return this.classic.cameraZ;
    }
    public double getCenterX() {
        return this.classic.centerX;
    }
    public double getCenterY() {
        return this.classic.centerY;
    }
    public double getCenterZ() {
        return this.classic.centerZ;
    }
    
    public void setAdjustCamera(boolean value) {
        this.classic.adjustCamera = value;
        ConfigFile.save();
    }
    public void setCameraX(double value) {
        this.classic.cameraX = value;
        ConfigFile.save();
    }
    public void setCameraY(double value) {
        this.classic.cameraY = value;
        ConfigFile.save();
    }
    public void setCameraZ(double value) {
        this.classic.cameraZ = value;
        ConfigFile.save();
    }
    public void setCenterX(double value) {
        this.classic.centerX = value;
        ConfigFile.save();
    }
    public void setCenterY(double value) {
        this.classic.centerY = value;
        ConfigFile.save();
    }
    public void setCenterZ(double value) {
        this.classic.centerZ = value;
        ConfigFile.save();
    }

    public void addClassicX() {
        if (isAdjustCamera()) setCameraX(Math.min(getCameraX() + getAdjustStep(), maxVALUE));
        else setCenterX(Math.min(getCenterX() + getAdjustStep(), maxVALUE));
    }
    public void subClassicX() {
        if (isAdjustCamera()) setCameraX(Math.max(getCameraX() - getAdjustStep(), minVALUE));
        else setCenterX(Math.max(getCenterX() - getAdjustStep(), minVALUE));
    }
    public void addClassicY() {
        if (isAdjustCamera()) {setCameraY(Math.min(getCameraY() + getAdjustStep(), maxVALUE));}
        else setCenterY(Math.min(getCenterY() + getAdjustStep(), maxVALUE));
    }
    public void subClassicY() {
        if (isAdjustCamera()) setCameraY(Math.max(getCameraY() - getAdjustStep(), minVALUE));
        else setCenterY(Math.max(getCenterY() - getAdjustStep(), minVALUE));
    }
    public void addClassicZ() {
        if (isAdjustCamera()) setCameraZ(Math.min(getCameraZ() + getAdjustStep(), maxVALUE));
        else setCenterZ(Math.min(getCenterZ() + getAdjustStep(), maxVALUE));
    }
    public void subClassicZ() {
        if (isAdjustCamera()) setCameraZ(Math.max(getCameraZ() - getAdjustStep(), minVALUE));
        else setCenterZ(Math.max(getCenterZ() - getAdjustStep(), minVALUE));
    }

}
