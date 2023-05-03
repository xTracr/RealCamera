package com.xtracr.realcamera.config;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class ModConfig {

    public static final double MIN_DOUBLE = -64.0D;
    public static final double MAX_DOUBLE = 64.0D;

    public General general = new General();
    public Binding binding = new Binding();
    public Classic classic = new Classic();
    public Compats compats = new Compats();
    public Disables disables = new Disables();

    public class General {

        public boolean enabled = false;
        public boolean classic = false;
        public boolean dynamicCrosshair = false;
        public boolean renderModel = true;
        public double adjustStep = 0.25D;
        public double scale = 1.0D;

        private void clamp() {
            adjustStep = MathHelper.clamp(adjustStep, 0.0D, MAX_DOUBLE);
            scale = MathHelper.clamp(scale, 0.0D, MAX_DOUBLE);
        }

    }

    public class Binding {

        public VanillaModelPart vanillaModelPart = VanillaModelPart.head;
        public boolean adjustOffset = true;
        public boolean bindDirection = true;
        public boolean lockRolling = false;
        public double cameraX = 3.25D;
        public double cameraY = 2.0D;
        public double cameraZ = 0.0D;
        public float pitch = 0.0F;
        public float yaw = 0.0F;
        public float roll = 0.0F;

        private void clamp() {
            if (!(vanillaModelPart instanceof VanillaModelPart)) vanillaModelPart = VanillaModelPart.head;
            cameraX = MathHelper.clamp(cameraX, MIN_DOUBLE, MAX_DOUBLE);
            cameraY = MathHelper.clamp(cameraY, MIN_DOUBLE, MAX_DOUBLE);
            cameraZ = MathHelper.clamp(cameraZ, MIN_DOUBLE, MAX_DOUBLE);
            pitch = MathHelper.wrapDegrees(pitch);
            yaw = MathHelper.wrapDegrees(yaw);
            roll = MathHelper.wrapDegrees(roll);
        }

    }

    public class Classic {

        public AdjustMode adjustMode = AdjustMode.CAMERA;
        public double cameraX = 3.25D;
        public double cameraY = 2.0D;
        public double cameraZ = 0.0D;
        public double centerX = 0.0D;
        public double centerY = -3.4D;
        public double centerZ = 0.0D;
        public float pitch = 0.0F;
        public float yaw = 0.0F;
        public float roll = 0.0F;

        private void clamp() {
            if (!(adjustMode instanceof AdjustMode)) adjustMode = AdjustMode.CAMERA;
            cameraX = MathHelper.clamp(cameraX, MIN_DOUBLE, MAX_DOUBLE);
            cameraY = MathHelper.clamp(cameraY, MIN_DOUBLE, MAX_DOUBLE);
            cameraZ = MathHelper.clamp(cameraZ, MIN_DOUBLE, MAX_DOUBLE);
            centerX = MathHelper.clamp(centerX, MIN_DOUBLE, MAX_DOUBLE);
            centerY = MathHelper.clamp(centerY, MIN_DOUBLE, MAX_DOUBLE);
            centerZ = MathHelper.clamp(centerZ, MIN_DOUBLE, MAX_DOUBLE);
            pitch = MathHelper.wrapDegrees(pitch);
            yaw = MathHelper.wrapDegrees(yaw);
            roll = MathHelper.wrapDegrees(roll);
        }

        public enum AdjustMode {
            CAMERA,
            CENTER,
            ROTATION;

            private static final AdjustMode[] VALUES = values();
            
            public AdjustMode cycle() {
                return VALUES[(this.ordinal() + 1) % VALUES.length];
            }
        }

    }

    public class Compats {

        public boolean useModModel = false;
        public String modelModID = "minecraft";
        public String modModelPart = "head";
        public boolean doABarrelRoll = true;
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
    public boolean isCrosshairDynamic() {
        return this.general.dynamicCrosshair;
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

    public boolean isDisabledWhen(ClientPlayerEntity player) {
        return (player.isFallFlying() && this.disables.fallFlying)
            || (player.isSwimming() && this.disables.swiming)
            || (player.shouldLeaveSwimmingPose() && this.disables.crawling)
            || (player.isSneaking() && this.disables.sneaking)
            || (player.isSleeping() && this.disables.sleeping);
    }
    public boolean onlyDisableRenderingWhen(ClientPlayerEntity player) {
        return player.isUsingSpyglass() && this.disables.scoping;
    }

    // binding
    public VanillaModelPart getVanillaModelPart() {
        return this.binding.vanillaModelPart;
    }
    public boolean isAdjustingOffset() {
        return this.binding.adjustOffset;
    }
    public boolean isDirectionBound() {
        return this.binding.bindDirection;
    }
    public boolean isRollingLocked() {
        return this.binding.lockRolling;
    }
    public double getBindingX() {
        return this.binding.cameraX;
    }
    public double getBindingY() {
        return this.binding.cameraY;
    }
    public double getBindingZ() {
        return this.binding.cameraZ;
    }
    public float getBindingPitch() {
        return this.binding.pitch;
    }
    public float getBindingYaw() {
        return this.binding.yaw;
    }
    public float getBindingRoll() {
        return this.binding.roll;
    }

    public void setAdjustOffset(boolean value) {
        this.binding.adjustOffset = value;
        ConfigFile.save();
    }
    public void adjustBindingX(boolean add) {
        int s = add ? 1 : -1;
        if (this.isAdjustingOffset()) this.binding.cameraX += s * getAdjustStep();
        else this.binding.roll += s * 4*(float)getAdjustStep();
        this.binding.clamp();
        ConfigFile.save();
    }
    public void adjustBindingY(boolean add) {
        int s = add ? 1 : -1;
        if (this.isAdjustingOffset()) this.binding.cameraY += s * getAdjustStep();
        else this.binding.yaw += s * 4*(float)getAdjustStep();
        this.binding.clamp();
        ConfigFile.save();
    }
    public void adjustBindingZ(boolean add) {
        int s = add ? 1 : -1;
        if (this.isAdjustingOffset()) this.binding.cameraZ += s * getAdjustStep();
        else this.binding.pitch += s * 4*(float)getAdjustStep();
        this.binding.clamp();
        ConfigFile.save();
    }

    // classic
    public Classic.AdjustMode getClassicAdjustMode() {
        return this.classic.adjustMode;
    }
    public double getClassicX() {
        return this.classic.cameraX;
    }
    public double getClassicY() {
        return this.classic.cameraY;
    }
    public double getClassicZ() {
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
    public float getClassicPitch() {
        return this.classic.pitch;
    }
    public float getClassicYaw() {
        return this.classic.yaw;
    }
    public float getClassicRoll() {
        return this.classic.roll;
    }
    
    public void cycleClassicAdjustMode() {
        this.classic.adjustMode = this.classic.adjustMode.cycle();
        ConfigFile.save();
    }
    public void adjustClassicX(boolean add) {
        int s = add ? 1 : -1;
        switch (this.classic.adjustMode) {
            case CENTER:
                this.classic.centerX += s * getAdjustStep();
                break;
            case ROTATION:
                this.classic.roll += s * 4*(float)getAdjustStep();
                break;
            default:
                this.classic.cameraX += s * getAdjustStep();
        }
        this.classic.clamp();
        ConfigFile.save();
    }
    public void adjustClassicY(boolean add) {
        int s = add ? 1 : -1;
        switch (this.classic.adjustMode) {
            case CENTER:
                this.classic.centerY += s * getAdjustStep();
                break;
            case ROTATION:
                this.classic.yaw += s * 4*(float)getAdjustStep();
                break;
            default:
                this.classic.cameraY += s * getAdjustStep();
        }
        this.classic.clamp();
        ConfigFile.save();
    }
    public void adjustClassicZ(boolean add) {
        int s = add ? 1 : -1;
        switch (this.classic.adjustMode) {
            case CENTER:
                this.classic.centerZ += s * getAdjustStep();
                break;
            case ROTATION:
                this.classic.pitch += s * 4*(float)getAdjustStep();
                break;
            default:
                this.classic.cameraZ += s * getAdjustStep();
        }
        this.classic.clamp();
        ConfigFile.save();
    }

    // compats
    public boolean isUsingModModel() {
        return this.compats.useModModel;
    }
    public String getModelModID() {
        return this.compats.modelModID;
    }
    public String getModModelPartName() {
        return this.compats.modModelPart;
    }
    public boolean compatDoABarrelRoll() {
        return this.compats.doABarrelRoll;
    }
    public boolean compatPehkui() {
        return this.compats.pehkui;
    }

}
