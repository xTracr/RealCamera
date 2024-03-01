package com.xtracr.realcamera.config;

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
    private double offsetX, offsetY, offsetZ;
    private float pitch, yaw, roll;

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

    private static ModConfig config() {
        return ConfigFile.modConfig;
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
        return isExperimental() ? bindX : config().isXBound();
    }

    public boolean bindY() {
        return isExperimental() ? bindY : config().isYBound();
    }

    public boolean bindZ() {
        return isExperimental() ? bindZ : config().isZBound();
    }

    public boolean bindRotation() {
        return isExperimental() ? bindRotation : config().isRotationBound();
    }

    public double scale() {
        return isExperimental() ? scale : config().getScale();
    }

    public double offsetX() {
        return isExperimental() ? offsetX * scale : config().getBindingX() * config().getScale();
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double offsetY() {
        return isExperimental() ? offsetY * scale : config().getBindingY() * config().getScale();
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public double offsetZ() {
        return isExperimental() ? offsetZ * scale : config().getBindingZ() * config().getScale();
    }

    public void setOffsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
    }

    public float pitch() {
        return isExperimental() ? pitch : config().getBindingPitch();
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float yaw() {
        return isExperimental() ? yaw : config().getBindingYaw();
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float roll() {
        return isExperimental() ? roll : config().getBindingRoll();
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    private boolean isExperimental() {
        return config().binding.experimental && textureId != null;
    }
}
