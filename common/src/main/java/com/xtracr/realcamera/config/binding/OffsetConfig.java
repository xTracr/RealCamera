package com.xtracr.realcamera.config.binding;

import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.ReferenceFrame;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;

public final class OffsetConfig {
    public boolean bindX = false, bindY = true, bindZ = false, bindRotation = false;
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

    /**
     * Compensate local offsets for a mirrored preview; reflection stays outside the frame.
     */
    public ReferenceFrame applyTo(ReferenceFrame frame, boolean mirrored) {
        int orientation = mirrored ? -1 : 1;
        Vec3 offset = new Vec3(orientation * z, y, x).scale(scale);
        Quaterniond rotation = new Quaterniond()
                .rotateY(orientation * Math.toRadians(yaw))
                .rotateX(Math.toRadians(pitch))
                .rotateZ(orientation * Math.toRadians(roll));
        return frame.translatedLocal(offset).rotatedLocal(rotation);
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
