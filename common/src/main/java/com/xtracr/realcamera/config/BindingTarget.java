package com.xtracr.realcamera.config;

import net.minecraft.util.Mth;

import java.util.List;

public class BindingTarget {
    protected static final List<BindingTarget> fixedTargets;
    protected static final List<BindingTarget> defaultTargets;
    public static final String API_ONLY = "FOR_API_ONLY";
    public final String name, textureId;
    private int priority = 0;
    private float forwardU = 0, forwardV = 0, upwardU = 0, upwardV = 0, posU = 0, posV = 0, disablingDepth = 0.2f;
    private boolean bindX = false, bindY = true, bindZ = false, bindRotation = false;
    private double scale = 1, offsetX = 0, offsetY = 0, offsetZ = 0;
    private float pitch = 0, yaw = 0, roll = 0;
    private List<String> disabledTextureIds = List.of();

    static {
        fixedTargets = List.of(new BindingTarget(API_ONLY, ""));
        defaultTargets = List.of(BindingTarget.vanillaTarget("minecraft_head", 5, false).setOffsetX(-0.1),
                BindingTarget.vanillaTarget("skin_head", 5, false).setOffsetX(-0.1),
                BindingTarget.vanillaTarget("minecraft_head_2", 1, true).setOffsetX(-0.1),
                BindingTarget.vanillaTarget("skin_head_2", 1, true).setOffsetX(-0.1));
    }

    public BindingTarget() {
        this(null, null);
    }

    protected BindingTarget(String name, String textureId) {
        this.name = name;
        this.textureId = textureId;
    }

    protected static BindingTarget vanillaTarget(String name, int priority, boolean shouldBind) {
        return new BindingTarget(name, name.contains("skin") ? "minecraft:skins/" : "minecraft:textures/entity/player/").setPriority(priority)
                .setForwardU(0.1875f).setForwardV(0.2f)
                .setUpwardU(0.1875f).setUpwardV(0.075f)
                .setPosU(0.1875f).setPosV(0.2f)
                .setBindX(shouldBind).setBindZ(shouldBind).setBindRotation(shouldBind)
                .setDisabledTextureIds(List.of("minecraft:textures/entity/enderdragon/dragon.png"));
    }

    public static BindingTarget create(String name, String textureId) {
        return new BindingTarget(name, textureId);
    }

    public boolean isEmpty() {
        return name == null || name.isEmpty();
    }

    public boolean fixed() {
        return API_ONLY.equals(name);
    }

    public int getPriority() {
        return priority;
    }

    public BindingTarget setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public float getForwardU() {
        return forwardU;
    }

    public BindingTarget setForwardU(float forwardU) {
        this.forwardU = forwardU;
        return this;
    }

    public float getForwardV() {
        return forwardV;
    }

    public BindingTarget setForwardV(float forwardV) {
        this.forwardV = forwardV;
        return this;
    }

    public float getUpwardU() {
        return upwardU;
    }

    public BindingTarget setUpwardU(float upwardU) {
        this.upwardU = upwardU;
        return this;
    }

    public float getUpwardV() {
        return upwardV;
    }

    public BindingTarget setUpwardV(float upwardV) {
        this.upwardV = upwardV;
        return this;
    }

    public float getPosU() {
        return posU;
    }

    public BindingTarget setPosU(float posU) {
        this.posU = posU;
        return this;
    }

    public float getPosV() {
        return posV;
    }

    public BindingTarget setPosV(float posV) {
        this.posV = posV;
        return this;
    }

    public float getDisablingDepth() {
        return disablingDepth;
    }

    public BindingTarget setDisablingDepth(float disablingDepth) {
        this.disablingDepth = disablingDepth;
        return this;
    }

    public boolean isBindX() {
        return bindX;
    }

    public BindingTarget setBindX(boolean bindX) {
        this.bindX = bindX;
        return this;
    }

    public boolean isBindY() {
        return bindY;
    }

    public BindingTarget setBindY(boolean bindY) {
        this.bindY = bindY;
        return this;
    }

    public boolean isBindZ() {
        return bindZ;
    }

    public BindingTarget setBindZ(boolean bindZ) {
        this.bindZ = bindZ;
        return this;
    }

    public boolean isBindRotation() {
        return bindRotation;
    }

    public BindingTarget setBindRotation(boolean bindRotation) {
        this.bindRotation = bindRotation;
        return this;
    }

    public double getScale() {
        return scale;
    }

    public BindingTarget setScale(double scale) {
        this.scale = scale;
        return this;
    }

    public List<String> getDisabledTextureIds() {
        return disabledTextureIds;
    }

    public BindingTarget setDisabledTextureIds(List<String> disabledTextureIds) {
        this.disabledTextureIds = disabledTextureIds;
        return this;
    }

    public double getOffsetX() {
        return offsetX;
    }
    
    public BindingTarget setOffsetX(double offsetX) {
        this.offsetX = Mth.clamp(offsetX, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
        return this;
    }
    
    public double getOffsetY() {
        return offsetY;
    }

    public BindingTarget setOffsetY(double offsetY) {
        this.offsetY = Mth.clamp(offsetY, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
        return this;
    }
    
    public double getOffsetZ() {
        return offsetZ;
    }

    public BindingTarget setOffsetZ(double offsetZ) {
        this.offsetZ = Mth.clamp(offsetZ, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
        return this;
    }

    public float getPitch() {
        return pitch;
    }

    public BindingTarget setPitch(float pitch) {
        this.pitch = Mth.wrapDegrees(pitch);
        return this;
    }

    public float getYaw() {
        return yaw;
    }

    public BindingTarget setYaw(float yaw) {
        this.yaw = Mth.wrapDegrees(yaw);
        return this;
    }

    public float getRoll() {
        return roll;
    }

    public BindingTarget setRoll(float roll) {
        this.roll = Mth.wrapDegrees(roll);
        return this;
    }
}
