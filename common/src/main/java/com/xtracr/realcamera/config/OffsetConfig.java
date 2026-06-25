package com.xtracr.realcamera.config;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;

public final class OffsetConfig {
    public float scale = 1;
    public float x, y, z;
    public PoseRotation standing = new PoseRotation();
    public PoseRotation crouching = new PoseRotation();
    public PoseRotation swimming = new PoseRotation();
    public PoseRotation flying = new PoseRotation();

    public OffsetConfig() {
    }

    @Deprecated
    public OffsetConfig(float scale, float x, float y, float z, float pitch, float yaw, float roll) {
        this(scale, x, y, z, new PoseRotation(pitch, yaw, roll), new PoseRotation(), new PoseRotation(), new PoseRotation());
    }

    public OffsetConfig(float scale, float x, float y, float z, PoseRotation standing, PoseRotation crouching, PoseRotation swimming, PoseRotation flying) {
        this.scale = scale;
        this.x = x;
        this.y = y;
        this.z = z;
        this.standing = standing;
        this.crouching = crouching;
        this.swimming = swimming;
        this.flying = flying;
    }


    public float pitch(Pose state) {
        return switch (state) {
            case CROUCHING -> crouching.pitch + standing.pitch;
            case SWIMMING -> swimming.pitch + standing.pitch;
            case FALL_FLYING -> flying.pitch + standing.pitch;
            default -> standing.pitch;
        };
    }

    public float yaw(Pose state) {
        return switch (state) {
            case CROUCHING -> crouching.yaw + standing.yaw;
            case SWIMMING -> swimming.yaw + standing.yaw;
            case FALL_FLYING -> flying.yaw + standing.yaw;
            default -> standing.yaw;
        };
    }

    public float roll(Pose state) {
        return switch (state) {
            case CROUCHING -> crouching.roll + standing.roll;
            case SWIMMING -> swimming.roll + standing.roll;
            case FALL_FLYING -> flying.roll + standing.roll;
            default -> standing.roll;
        };
    }

    public void adjustPitch(Pose state, float value) {
        switch (state) {
            case CROUCHING:
                crouching.pitch += value;
                break;
            case SWIMMING:
                swimming.pitch += value;
                break;
            case FALL_FLYING:
                flying.pitch += value;
                break;
            default:
                standing.pitch += value;
        }
    }

    public void adjustYaw(Pose state, float value) {
        switch (state) {
            case CROUCHING:
                crouching.yaw += value;
                break;
            case SWIMMING:
                swimming.yaw += value;
                break;
            case FALL_FLYING:
                flying.yaw += value;
                break;
            default:
                standing.yaw += value;
        }
    }

    public void adjustRoll(Pose state, float value) {
        switch (state) {
            case CROUCHING:
                crouching.roll += value;
                break;
            case SWIMMING:
                swimming.roll += value;
                break;
            case FALL_FLYING:
                flying.roll += value;
                break;
            default:
                standing.roll += value;
        }
    }

    public void clamp() {
        x = Mth.clamp(x, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        y = Mth.clamp(y, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        z = Mth.clamp(z, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        standing.clamp();
        crouching.clamp();
        swimming.clamp();
        flying.clamp();
    }
}