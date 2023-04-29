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
            cameraX = MathHelper.clamp(cameraX, minVALUE, maxVALUE);
            cameraY = MathHelper.clamp(cameraY, minVALUE, maxVALUE);
            cameraZ = MathHelper.clamp(cameraZ, minVALUE, maxVALUE);
            pitch = MathHelper.wrapDegrees(pitch);
            yaw = MathHelper.wrapDegrees(yaw);
            roll = MathHelper.wrapDegrees(roll);
        }

    }

    public class Classic {

        public AdjustMode adjustMode = AdjustMode.camera;
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
            if (!(adjustMode instanceof AdjustMode)) adjustMode = AdjustMode.camera;
            cameraX = MathHelper.clamp(cameraX, minVALUE, maxVALUE);
            cameraY = MathHelper.clamp(cameraY, minVALUE, maxVALUE);
            cameraZ = MathHelper.clamp(cameraZ, minVALUE, maxVALUE);
            centerX = MathHelper.clamp(centerX, minVALUE, maxVALUE);
            centerY = MathHelper.clamp(centerY, minVALUE, maxVALUE);
            centerZ = MathHelper.clamp(centerZ, minVALUE, maxVALUE);
            pitch = MathHelper.wrapDegrees(pitch);
            yaw = MathHelper.wrapDegrees(yaw);
            roll = MathHelper.wrapDegrees(roll);
        }

        public enum AdjustMode {
            camera,
            center,
            rotation;

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
            || (player.isCrawling() && this.disables.crawling)
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
    public boolean isAdjustOffset() {
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
    public void setBindingX(double value) {
        this.binding.cameraX = value;
        this.binding.clamp();
        ConfigFile.save();
    }
    public void setBindingY(double value) {
        this.binding.cameraY = value;
        this.binding.clamp();
        ConfigFile.save();
    }
    public void setBindingZ(double value) {
        this.binding.cameraZ = value;
        this.binding.clamp();
        ConfigFile.save();
    }
    public void setBindingPitch(float value) {
        this.binding.pitch = value;
        this.binding.clamp();
        ConfigFile.save();
    }
    public void setBindingYaw(float value) {
        this.binding.yaw = value;
        this.binding.clamp();
        ConfigFile.save();
    }
    public void setBindingRoll(float value) {
        this.binding.roll = value;
        this.binding.clamp();
        ConfigFile.save();
    }

    public void adjustBindingX(boolean add) {
        int s = add ? 1 : -1;
        if (this.isAdjustOffset()) setBindingX(getBindingX() + s*getAdjustStep());
        else setBindingRoll(getBindingRoll() + s*4*(float)getAdjustStep());
    }
    public void adjustBindingY(boolean add) {
        int s = add ? 1 : -1;
        if (this.isAdjustOffset()) setBindingY(getBindingY() + s*getAdjustStep());
        else setBindingYaw(getBindingYaw() + s*4*(float)getAdjustStep());
    }
    public void adjustBindingZ(boolean add) {
        int s = add ? 1 : -1;
        if (this.isAdjustOffset()) setBindingZ(getBindingZ() + s*getAdjustStep());
        else setBindingPitch(getBindingPitch() + s*4*(float)getAdjustStep());
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
    public void setClassicX(double value) {
        this.classic.cameraX = value;
        this.classic.clamp();
        ConfigFile.save();
    }
    public void setClassicY(double value) {
        this.classic.cameraY = value;
        this.classic.clamp();
        ConfigFile.save();
    }
    public void setClassicZ(double value) {
        this.classic.cameraZ = value;
        this.classic.clamp();
        ConfigFile.save();
    }
    public void setCenterX(double value) {
        this.classic.centerX = value;
        this.classic.clamp();
        ConfigFile.save();
    }
    public void setCenterY(double value) {
        this.classic.centerY = value;
        this.classic.clamp();
        ConfigFile.save();
    }
    public void setCenterZ(double value) {
        this.classic.centerZ = value;
        this.classic.clamp();
        ConfigFile.save();
    }
    public void setClassicPitch(float value) {
        this.classic.pitch = value;
        this.classic.clamp();
        ConfigFile.save();
    }
    public void setClassicYaw(float value) {
        this.classic.yaw = value;
        this.classic.clamp();
        ConfigFile.save();
    }
    public void setClassicRoll(float value) {
        this.classic.roll = value;
        this.classic.clamp();
        ConfigFile.save();
    }

    public void adjustClassicX(boolean add) {
        int s = add ? 1 : -1;
        switch (this.classic.adjustMode) {
            case center:
                setCenterX(getCenterX() + s*getAdjustStep());
                break;
            case rotation:
                setClassicRoll(getClassicRoll() + s*4*(float)getAdjustStep());
                break;
            default:
                setClassicX(getClassicX() + s*getAdjustStep());
        }
    }
    public void adjustClassicY(boolean add) {
        int s = add ? 1 : -1;
        switch (this.classic.adjustMode) {
            case center:
                setCenterY(getCenterY() + s*getAdjustStep());
                break;
            case rotation:
                setClassicYaw(getClassicYaw() + s*4*(float)getAdjustStep());
                break;
            default:
                setClassicY(getClassicY() + s*getAdjustStep());
        }
    }
    public void adjustClassicZ(boolean add) {
        int s = add ? 1 : -1;
        switch (this.classic.adjustMode) {
            case center:
                setCenterZ(getCenterZ() + s*getAdjustStep());
                break;
            case rotation:
                setClassicPitch(getClassicPitch() + s*4*(float)getAdjustStep());
                break;
            default:
                setClassicZ(getClassicZ() + s*getAdjustStep());
        }
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
