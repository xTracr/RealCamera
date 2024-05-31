package com.xtracr.realcamera.config;

import net.minecraft.util.Mth;

import java.util.List;

public class BindingTarget {
    protected static final List<BindingTarget> defaultTargets = List.of(createDefaultTarget("minecraft_head", "minecraft:textures/entity/player/", 5, false),
            createDefaultTarget("skin_head", "minecraft:skins/", 5, false),
            createDefaultTarget("minecraft_head_2", "minecraft:textures/entity/player/", 0, true),
            createDefaultTarget("skin_head_2", "minecraft:skins/", 0, true));
    public final String name, textureId;
    public int priority = 0;
    public float forwardU = 0, forwardV = 0, upwardU = 0, upwardV = 0, posU = 0, posV = 0, disablingDepth = 0.2f;
    public boolean bindX = false, bindY = true, bindZ = false, bindRotation = false;
    public double scale = 1, offsetX = 0, offsetY = 0, offsetZ = 0;
    public float pitch = 0, yaw = 0, roll = 0;
    public List<String> disabledTextureIds = List.of();

    public BindingTarget() {
        this(null, null);
    }

    public BindingTarget(String name, String textureId) {
        this.name = name;
        this.textureId = textureId;
    }

    private static BindingTarget createDefaultTarget(String name, String textureId, int priority, boolean shouldBind) {
        return new BindingTarget(name, textureId).priority(priority)
                .forwardU(0.1875f).forwardV(0.2f)
                .upwardU(0.1875f).upwardV(0.075f)
                .posU(0.1875f).posV(0.2f)
                .bindX(shouldBind).bindZ(shouldBind).bindRotation(shouldBind)
                .offsetX(-0.1)
                .disabledTextureIds(List.of("minecraft:textures/entity/enderdragon/dragon.png"));
    }

    public boolean isEmpty() {
        return name == null;
    }

    public double getOffsetX() {
        return offsetX * scale;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = Mth.clamp(offsetX, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    }

    public double getOffsetY() {
        return offsetY * scale;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = Mth.clamp(offsetY, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    }

    public double getOffsetZ() {
        return offsetZ * scale;
    }

    public void setOffsetZ(double offsetZ) {
        this.offsetZ = Mth.clamp(offsetZ, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = Mth.wrapDegrees(pitch);
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = Mth.wrapDegrees(yaw);
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = Mth.wrapDegrees(roll);
    }

    public BindingTarget priority(int priority) {
        this.priority = priority;
        return this;
    }

    public BindingTarget forwardU(float forwardU) {
        this.forwardU = forwardU;
        return this;
    }

    public BindingTarget forwardV(float forwardV) {
        this.forwardV = forwardV;
        return this;
    }

    public BindingTarget upwardU(float upwardU) {
        this.upwardU = upwardU;
        return this;
    }

    public BindingTarget upwardV(float upwardV) {
        this.upwardV = upwardV;
        return this;
    }

    public BindingTarget posU(float posU) {
        this.posU = posU;
        return this;
    }

    public BindingTarget posV(float posV) {
        this.posV = posV;
        return this;
    }

    public BindingTarget disablingDepth(float disablingDepth) {
        this.disablingDepth = disablingDepth;
        return this;
    }

    public BindingTarget bindX(boolean bindX) {
        this.bindX = bindX;
        return this;
    }

    public BindingTarget bindY(boolean bindY) {
        this.bindY = bindY;
        return this;
    }

    public BindingTarget bindZ(boolean bindZ) {
        this.bindZ = bindZ;
        return this;
    }

    public BindingTarget bindRotation(boolean bindRotation) {
        this.bindRotation = bindRotation;
        return this;
    }

    public BindingTarget scale(double scale) {
        this.scale = scale;
        return this;
    }

    public BindingTarget offsetX(double offsetX) {
        this.offsetX = offsetX;
        return this;
    }

    public BindingTarget offsetY(double offsetY) {
        this.offsetY = offsetY;
        return this;
    }

    public BindingTarget offsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
        return this;
    }

    public BindingTarget pitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public BindingTarget yaw(float yaw) {
        this.yaw = yaw;
        return this;
    }

    public BindingTarget roll(float roll) {
        this.roll = roll;
        return this;
    }

    public BindingTarget disabledTextureIds(List<String> disabledTextureIds) {
        this.disabledTextureIds = disabledTextureIds;
        return this;
    }
}
