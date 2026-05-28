package com.xtracr.realcamera.config;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class OffsetConfig {
    public float scale = 1;
    public float x, y, z, pitch, yaw, roll;
    public float swimmingPitchAdjustment, crawlingPitchAdjustment;

    public OffsetConfig() {
    }

    public OffsetConfig(float scale, float x, float y, float z, float pitch, float yaw, float roll) {
        this(scale, x, y, z, pitch, yaw, roll, 0, 0);
    }

    public OffsetConfig(float scale, float x, float y, float z, float pitch, float yaw, float roll, float swimmingPitchAdjustment, float crawlingPitchAdjustment) {
        this.scale = scale;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
        this.swimmingPitchAdjustment = swimmingPitchAdjustment;
        this.crawlingPitchAdjustment = crawlingPitchAdjustment;
    }

    public void clamp() {
        x = Mth.clamp(x, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        y = Mth.clamp(y, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        z = Mth.clamp(z, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        pitch = Mth.wrapDegrees(pitch);
        yaw = Mth.wrapDegrees(yaw);
        roll = Mth.wrapDegrees(roll);
        swimmingPitchAdjustment = Mth.wrapDegrees(swimmingPitchAdjustment);
        crawlingPitchAdjustment = Mth.wrapDegrees(crawlingPitchAdjustment);
    }

    public float pitchAdjustment(Entity entity) {
        if (!(entity instanceof Player player)) return 0;
        if (player.isSwimming()) return swimmingPitchAdjustment;
        if (player.isVisuallyCrawling()) return crawlingPitchAdjustment;
        return 0;
    }

    public void adjustPitch(Entity entity, float value) {
        if (entity instanceof Player player) {
            if (player.isSwimming()) {
                swimmingPitchAdjustment += value;
                clamp();
                return;
            }
            if (player.isVisuallyCrawling()) {
                crawlingPitchAdjustment += value;
                clamp();
                return;
            }
        }
        pitch += value;
        clamp();
    }
}
