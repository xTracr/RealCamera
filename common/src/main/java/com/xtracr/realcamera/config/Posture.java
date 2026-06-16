package com.xtracr.realcamera.config;

import net.minecraft.util.Mth;

public final class Posture {
    public float pitch, yaw, roll;

    public Posture() {
    }

    public Posture(float pitch, float yaw, float roll) {
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