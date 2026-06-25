package com.xtracr.realcamera.config;

import net.minecraft.util.Mth;

public final class PoseRotation {
    public float pitch, yaw, roll;

    public PoseRotation() {
    }

    public PoseRotation(float pitch, float yaw, float roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;

    }

    public void clamp() {
        pitch = Mth.wrapDegrees(pitch);
        yaw = Mth.wrapDegrees(yaw);
        roll = Mth.wrapDegrees(roll);
    }
}