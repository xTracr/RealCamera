package com.xtracr.realcamera.config;

import net.minecraft.util.math.MathHelper;

import java.util.List;

public class BindingTarget {
    protected static final List<BindingTarget> defaultTargets = List.of(createDefaultTarget("minecraft_head", "minecraft:textures/entity/player/", 5, false),
            createDefaultTarget("skin_head", "minecraft:skins/", 5, false),
            createDefaultTarget("minecraft_head_2", "minecraft:textures/entity/player/", 0, true),
            createDefaultTarget("skin_head_2", "minecraft:skins/", 0, true));
    public final String name, textureId;
    public final int priority;
    public final float forwardU, forwardV, upwardU, upwardV, posU, posV, disablingDepth;
    public final boolean bindX, bindY, bindZ, bindRotation;
    public double scale, offsetX, offsetY, offsetZ;
    public float pitch, yaw, roll;

    public BindingTarget() {
        this(null, null, 0, 0, 0, 0, 0, 0, 0, 0.2f, false, true, false, false, 1, 0, 0, 0, 0, 0, 0);
    }

    public BindingTarget(String name, String textureId, int priority, float forwardU, float forwardV, float upwardU, float upwardV, float posU, float posV, float disablingDepth, boolean bindX, boolean bindY, boolean bindZ, boolean bindRotation, double scale, double offsetX, double offsetY, double offsetZ, float pitch, float yaw, float roll) {
        this.name = name;
        this.textureId = textureId;
        this.priority = priority;
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

    private static BindingTarget createDefaultTarget(String name, String textureId, int priority, boolean shouldBind) {
        return new BindingTarget(name, textureId, priority, 0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f, 0.2f, shouldBind, true, shouldBind, shouldBind, 1, -0.12, 0, 0, 0, 0, 0);
    }

    public boolean isEmpty() {
        return name == null;
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
