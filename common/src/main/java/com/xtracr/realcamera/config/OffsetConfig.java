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
}
