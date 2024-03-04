package com.xtracr.realcamera.config;

import net.minecraft.util.math.MathHelper;

import java.util.List;

public class BindingTarget {
    protected static final List<BindingTarget> defaultTargets = List.of(createDefaultTarget("minecraft_head", "minecraft:textures/entity/player/", false),
            createDefaultTarget("minecraft_head_2", "minecraft:textures/entity/player/", true),
            createDefaultTarget("skin_head", "minecraft:skins/", false),
            createDefaultTarget("skin_head_2", "minecraft:skins/", true));
    private final String name, textureId;
    private final float forwardU, forwardV, upwardU, upwardV, posU, posV, disablingDepth;
    private final boolean bindX, bindY, bindZ, bindRotation;
    private final double scale;
    protected double offsetX, offsetY, offsetZ;
    protected float pitch, yaw, roll;

    public BindingTarget() {
        this(null, null, 0, 0, 0, 0, 0, 0, 0.2f, false, true, false, false, 1, 0, 0, 0, 0, 0, 0);
    }

    public BindingTarget(String name, String textureId, float forwardU, float forwardV, float upwardU, float upwardV, float posU, float posV, float disablingDepth, boolean bindX, boolean bindY, boolean bindZ, boolean bindRotation, double scale, double offsetX, double offsetY, double offsetZ, float pitch, float yaw, float roll) {
        this.name = name;
        this.textureId = textureId;
        this.forwardU = forwardU;
        this.forwardV = forwardV;
        this.upwardU = upwardU;
        this.upwardV = upwardV;
        this.posU = posU;
        this.posV = posV;
        this.disablingDepth = disablingDepth;
        this.bindX = bindX;
        this.bindY = bindY;
        this.bindZ = bindZ;
        this.bindRotation = bindRotation;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    private static BindingTarget createDefaultTarget(String name, String textureId, boolean shouldBind) {
        return new BindingTarget(name, textureId, 0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f, 0.2f, shouldBind, true, shouldBind, shouldBind, 1, -0.12, 0, 0, 0, 0, 0);
    }

    public boolean isEmpty() {
        return name == null;
    }

    public String name() {
        return name;
    }

    public String textureId() {
        return textureId;
    }

    public float forwardU() {
        return forwardU;
    }

    public float forwardV() {
        return forwardV;
    }

    public float upwardU() {
        return upwardU;
    }

    public float upwardV() {
        return upwardV;
    }

    public float posU() {
        return posU;
    }

    public float posV() {
        return posV;
    }

    public float disablingDepth() {
        return disablingDepth;
    }

    public boolean bindX() {
        return bindX;
    }

    public boolean bindY() {
        return bindY;
    }

    public boolean bindZ() {
        return bindZ;
    }

    public boolean bindRotation() {
        return bindRotation;
    }

    public double scale() {
        return scale;
    }

    public double offsetX() {
        return offsetX * scale;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = MathHelper.clamp(offsetX, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    }

    public double offsetY() {
        return offsetY * scale;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = MathHelper.clamp(offsetY, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    }

    public double offsetZ() {
        return offsetZ * scale;
    }

    public void setOffsetZ(double offsetZ) {
        this.offsetZ = MathHelper.clamp(offsetZ, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    }

    public float pitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = MathHelper.wrapDegrees(pitch);
    }

    public float yaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = MathHelper.wrapDegrees(yaw);
    }

    public float roll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = MathHelper.wrapDegrees(roll);
    }
}
