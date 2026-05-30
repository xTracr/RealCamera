package com.xtracr.realcamera.config;

import net.minecraft.util.Mth;

public final class OffsetConfig {
    public float scale = 1;
    public float x, y, z;
    public float pitch, yaw, roll;
    public float sneakingPitch, sneakingYaw, sneakingRoll;
    public float swimmingPitch, swimmingYaw, swimmingRoll;
    public float crawlingPitch, crawlingYaw, crawlingRoll;

    public OffsetConfig() {
    }

    public OffsetConfig(float scale, float x, float y, float z, float pitch, float yaw, float roll) {
        this(scale, x, y, z,
                pitch, yaw, roll,
                pitch, yaw, roll,
                pitch, yaw, roll,
                pitch, yaw, roll);
    }

    public OffsetConfig(
            float scale, float x, float y, float z,
            float standPitch, float standYaw, float standRoll,
            float sneakingPitch, float sneakingYaw, float sneakingRoll,
            float swimmingPitch, float swimmingYaw, float swimmingRoll,
            float crawlingPitch, float crawlingYaw, float crawlingRoll) {
        this.scale = scale;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = standPitch;
        this.yaw = standYaw;
        this.roll = standRoll;
        this.sneakingPitch = sneakingPitch;
        this.sneakingYaw = sneakingYaw;
        this.sneakingRoll = sneakingRoll;
        this.swimmingPitch = swimmingPitch;
        this.swimmingYaw = swimmingYaw;
        this.swimmingRoll = swimmingRoll;
        this.crawlingPitch = crawlingPitch;
        this.crawlingYaw = crawlingYaw;
        this.crawlingRoll = crawlingRoll;
    }

    public void clamp() {
        x = Mth.clamp(x, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        y = Mth.clamp(y, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        z = Mth.clamp(z, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        pitch = Mth.wrapDegrees(pitch);
        yaw = Mth.wrapDegrees(yaw);
        roll = Mth.wrapDegrees(roll);
        sneakingPitch = Mth.wrapDegrees(sneakingPitch);
        sneakingYaw = Mth.wrapDegrees(sneakingYaw);
        sneakingRoll = Mth.wrapDegrees(sneakingRoll);
        swimmingPitch = Mth.wrapDegrees(swimmingPitch);
        swimmingYaw = Mth.wrapDegrees(swimmingYaw);
        swimmingRoll = Mth.wrapDegrees(swimmingRoll);
        crawlingPitch = Mth.wrapDegrees(crawlingPitch);
        crawlingYaw = Mth.wrapDegrees(crawlingYaw);
        crawlingRoll = Mth.wrapDegrees(crawlingRoll);
    }

    public float pitch(CameraPosture posture) {
        return switch (normalize(posture)) {
            case SNEAKING -> sneakingPitch;
            case SWIMMING -> swimmingPitch;
            case CRAWLING -> crawlingPitch;
            case STAND -> pitch;
        };
    }

    public float yaw(CameraPosture posture) {
        return switch (normalize(posture)) {
            case SNEAKING -> sneakingYaw;
            case SWIMMING -> swimmingYaw;
            case CRAWLING -> crawlingYaw;
            case STAND -> yaw;
        };
    }

    public float roll(CameraPosture posture) {
        return switch (normalize(posture)) {
            case SNEAKING -> sneakingRoll;
            case SWIMMING -> swimmingRoll;
            case CRAWLING -> crawlingRoll;
            case STAND -> roll;
        };
    }

    public void setRotation(CameraPosture posture, float pitch, float yaw, float roll) {
        switch (normalize(posture)) {
            case SNEAKING -> {
                sneakingPitch = pitch;
                sneakingYaw = yaw;
                sneakingRoll = roll;
            }
            case SWIMMING -> {
                swimmingPitch = pitch;
                swimmingYaw = yaw;
                swimmingRoll = roll;
            }
            case CRAWLING -> {
                crawlingPitch = pitch;
                crawlingYaw = yaw;
                crawlingRoll = roll;
            }
            case STAND -> {
                this.pitch = pitch;
                this.yaw = yaw;
                this.roll = roll;
            }
        }
        clamp();
    }

    public void adjustPitch(CameraPosture posture, float value) {
        setRotation(posture, pitch(posture) + value, yaw(posture), roll(posture));
    }

    public void adjustYaw(CameraPosture posture, float value) {
        setRotation(posture, pitch(posture), yaw(posture) + value, roll(posture));
    }

    public void adjustRoll(CameraPosture posture, float value) {
        setRotation(posture, pitch(posture), yaw(posture), roll(posture) + value);
    }

    private static CameraPosture normalize(CameraPosture posture) {
        return posture == null ? CameraPosture.STAND : posture;
    }
}
