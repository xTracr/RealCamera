package com.xtracr.realcamera.config;

public class BindingTarget {
    public static final BindingTarget MINECRAFT_HEAD = new BindingTarget("minecraft_head", "minecraft:textures/entity/player/", 0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f, false, true, false, false, 1, -0.15f, 0, 0, 0, 0, 0, 0.2f);
    public static final BindingTarget MINECRAFT_HEAD_2 = new BindingTarget("minecraft_head_2", "minecraft:textures/entity/player/", 0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f, true, true, true, true, 1, -0.15f, 0, 0, 0, 0, 0, 0.2f);
    String name, textureId;
    float forwardU, forwardV, upwardU, upwardV, posU, posV;
    boolean bindX, bindY, bindZ, bindRotation;
    double scale, offsetX, offsetY, offsetZ;
    float pitch, yaw, roll, disablingDepth;

    public BindingTarget() {
        this(null, null, 0, 0, 0, 0, 0, 0, true, true, true, true, 1, 0, 0, 0, 0, 0, 0, 0);
    }

    public BindingTarget(String name, String textureId, float forwardU, float forwardV, float upwardU, float upwardV, float posU, float posV, boolean bindX, boolean bindY, boolean bindZ, boolean bindRotation, double scale, double offsetX, double offsetY, double offsetZ, float pitch, float yaw, float roll, float disablingDepth) {
        this.name = name;
        this.textureId = textureId;
        this.forwardU = forwardU;
        this.forwardV = forwardV;
        this.upwardU = upwardU;
        this.upwardV = upwardV;
        this.posU = posU;
        this.posV = posV;
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
        this.disablingDepth = disablingDepth;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String textureId() {
        return textureId;
    }

    public void setTextureId(String textureId) {
        this.textureId = textureId;
    }

    public float forwardU() {
        return forwardU;
    }

    public void setForwardU(float forwardU) {
        this.forwardU = forwardU;
    }

    public float forwardV() {
        return forwardV;
    }

    public void setForwardV(float forwardV) {
        this.forwardV = forwardV;
    }

    public float upwardU() {
        return upwardU;
    }

    public void setUpwardU(float upwardU) {
        this.upwardU = upwardU;
    }

    public float upwardV() {
        return upwardV;
    }

    public void setUpwardV(float upwardV) {
        this.upwardV = upwardV;
    }

    public float posU() {
        return posU;
    }

    public void setPosU(float posU) {
        this.posU = posU;
    }

    public float posV() {
        return posV;
    }

    public void setPosV(float posV) {
        this.posV = posV;
    }

    public boolean bindX() {
        return isExperimental() ? bindX : config().isXBound();
    }

    public void setBindX(boolean bindX) {
        this.bindX = bindX;
    }

    public boolean bindY() {
        return isExperimental() ? bindY : config().isYBound();
    }

    public void setBindY(boolean bindY) {
        this.bindY = bindY;
    }

    public boolean bindZ() {
        return isExperimental() ? bindZ : config().isZBound();
    }

    public void setBindZ(boolean bindZ) {
        this.bindZ = bindZ;
    }

    public boolean bindRotation() {
        return isExperimental() ? bindRotation : config().isRotationBound();
    }

    public void setBindRotation(boolean bindRotation) {
        this.bindRotation = bindRotation;
    }

    public double scale() {
        return isExperimental() ? scale : config().getScale();
    }

    public void setScale(double scale) {
        this.scale = scale;
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

    public float disablingDepth() {
        return disablingDepth;
    }

    public void setDisablingDepth(float disablingDepth) {
        this.disablingDepth = disablingDepth;
    }

    private boolean isExperimental() {
        return config().binding.experimental && textureId != null;
    }
    
    private static ModConfig config() {
        return ConfigFile.modConfig;
    }
}
