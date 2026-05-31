package com.xtracr.realcamera.config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class OffsetConfig {
    public float scale = 1;
    public float x, y, z;
    public Posture standing = new Posture();
    public Posture sneaking = new Posture();
    public Posture swimming = new Posture();
    public Posture crawling = new Posture();

    @Deprecated
    public OffsetConfig(float scale, float x, float y, float z, float pitch, float yaw, float roll) {
        this(scale, x, y, z, new Posture().read(pitch, yaw, roll), new Posture(), new Posture(), new Posture());
    }

    public OffsetConfig() {
    }

    public OffsetConfig(float scale, float x, float y, float z, Posture standing, Posture sneaking, Posture swimming, Posture crawling) {
        this.scale = scale;
        this.x = x;
        this.y = y;
        this.z = z;
        this.standing = standing;
        this.sneaking = sneaking;
        this.swimming = swimming;
        this.crawling = crawling;
    }

    public static String from(Entity entity) {
        if (!(entity instanceof Player player)) return "NONE";
        if (player.isCrouching()) return "SNEAK";
        if (player.isSwimming()) return "SWIM";
        if (player.isVisuallyCrawling()) return "CRAWL";
        return "STAND";
    }

    public float pitch(String state) {
        return switch (state) {
            case "SNEAK" -> sneaking.pitch + standing.pitch;
            case "SWIM" -> swimming.pitch + standing.pitch;
            case "CRAWL" -> crawling.pitch + standing.pitch;
            default -> standing.pitch;
        };
    }

    public float yaw(String state) {
        return switch (state) {
            case "SNEAK" -> sneaking.yaw + standing.yaw;
            case "SWIM" -> swimming.yaw + standing.yaw;
            case "CRAWL" -> crawling.yaw + standing.yaw;
            default -> standing.yaw;
        };
    }

    public float roll(String state) {
        return switch (state) {
            case "SNEAK" -> sneaking.roll + standing.roll;
            case "SWIM" -> swimming.roll + standing.roll;
            case "CRAWL" -> crawling.roll + standing.roll;
            default -> standing.roll;
        };
    }

    public void adjustPitch(Entity entity, float value) {
        switch (from(entity)) {
            case "SNEAK":
                sneaking.pitch += value;
                break;
            case "SWIM":
                swimming.pitch += value;
                break;
            case "CRAWL":
                crawling.pitch += value;
                break;
            default:
                standing.pitch += value;
        }
    }

    public void adjustYaw(Entity entity, float value) {
        switch (from(entity)) {
            case "SNEAK":
                sneaking.yaw += value;
                break;
            case "SWIM":
                swimming.yaw += value;
                break;
            case "CRAWL":
                crawling.yaw += value;
                break;
            default:
                standing.yaw += value;
        }
    }

    public void adjustRoll(Entity entity, float value) {
        switch (from(entity)) {
            case "SNEAK":
                sneaking.roll += value;
                break;
            case "SWIM":
                swimming.roll += value;
                break;
            case "CRAWL":
                crawling.roll += value;
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
        sneaking.clamp();
        swimming.clamp();
        crawling.clamp();
    }

    public void write(FriendlyByteBuf byteBuf) {
        byteBuf.writeFloat(scale);
        byteBuf.writeFloat(x);
        byteBuf.writeFloat(y);
        byteBuf.writeFloat(z);
        byteBuf.writeFloat(standing.pitch);
        byteBuf.writeFloat(standing.yaw);
        byteBuf.writeFloat(standing.roll);
        byteBuf.writeFloat(sneaking.pitch);
        byteBuf.writeFloat(sneaking.yaw);
        byteBuf.writeFloat(sneaking.roll);
        byteBuf.writeFloat(swimming.pitch);
        byteBuf.writeFloat(swimming.yaw);
        byteBuf.writeFloat(swimming.roll);
        byteBuf.writeFloat(crawling.pitch);
        byteBuf.writeFloat(crawling.yaw);
        byteBuf.writeFloat(crawling.roll);
    }

    public static class Posture {
        public float pitch, yaw, roll;

        public Posture read(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
            return this;
        }

        public Posture read(FriendlyByteBuf buf) {
            pitch = buf.readFloat();
            yaw = buf.readFloat();
            roll = buf.readFloat();
            return this;
        }

        public void clamp() {
            pitch = Mth.wrapDegrees(pitch);
            yaw = Mth.wrapDegrees(yaw);
            roll = Mth.wrapDegrees(roll);
        }
    }
}