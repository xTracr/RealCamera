package com.xtracr.realcamera.config;

import net.minecraft.util.Mth;

public final class OffsetConfig {
    public float scale = 1;
    public float x, y, z, pitch, yaw, roll;

    public OffsetConfig() {
    }

    public OffsetConfig(float scale, float x, float y, float z, float pitch, float yaw, float roll) {
        this.scale = scale;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public void clamp() {
        x = Mth.clamp(x, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        y = Mth.clamp(y, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        z = Mth.clamp(z, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        pitch = Mth.wrapDegrees(pitch);
        yaw = Mth.wrapDegrees(yaw);
        roll = Mth.wrapDegrees(roll);
    }

    public float getScale() {
        return scale;
    }

    public OffsetConfig setScale(float scale) {
        this.scale = scale;
        return this;
    }

    public float getX() {
        return x;
    }

    public OffsetConfig setX(float x) {
        this.x = x;
        clamp();
        return this;
    }

    public float getY() {
        return y;
    }

    public OffsetConfig setY(float y) {
        this.y = y;
        clamp();
        return this;
    }

    public float getZ() {
        return z;
    }

    public OffsetConfig setZ(float z) {
        this.z = z;
        clamp();
        return this;
    }

    public float getPitch() {
        return pitch;
    }

    public OffsetConfig setPitch(float pitch) {
        this.pitch = pitch;
        clamp();
        return this;
    }

    public float getYaw() {
        return yaw;
    }

    public OffsetConfig setYaw(float yaw) {
        this.yaw = yaw;
        clamp();
        return this;
    }

    public float getRoll() {
        return roll;
    }

    public OffsetConfig setRoll(float roll) {
        this.roll = roll;
        clamp();
        return this;
    }
}
