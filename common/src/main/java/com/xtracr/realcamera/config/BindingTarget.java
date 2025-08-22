package com.xtracr.realcamera.config;

import com.xtracr.realcamera.util.VertexData;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public record BindingTarget(
        String name, String textureId, int priority, float disablingDepth,
        TargetConfig targetConfig,
        BindConfig bindConfig,
        OffsetConfig offsets,
        DisableConfig[] disableConfigs) {
    public static final Set<String> fixedNames = new HashSet<>();
    public static final List<BindingTarget> defaultTargets;
    public static final BindingTarget EMPTY = blank(null, null);

    static {
        defaultTargets = List.of(
                BindingTarget.vanillaTarget("minecraft_head", 5, false),
                BindingTarget.vanillaTarget("skin_head", 5, false),
                BindingTarget.vanillaTarget("minecraft_head_2", 1, true),
                BindingTarget.vanillaTarget("skin_head_2", 1, true)
        );
    }

    public static BindingTarget blank(String name, String textureId) {
        TargetConfig targetConfig = new TargetConfig(0, 0, 0, 0, 0, 0);
        BindConfig bindConfig = new BindConfig(false, true, false, false);
        return new BindingTarget(name, textureId, 0, 0.2f, targetConfig, bindConfig, new OffsetConfig(), new DisableConfig[0]);
    }

    private static BindingTarget vanillaTarget(String name, int priority, boolean shouldBind) {
        String textureId = name.contains("skin") ? "minecraft:skins/" : "minecraft:textures/entity/player/";
        TargetConfig targetConfig = new TargetConfig(0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f);
        BindConfig bindConfig = new BindConfig(shouldBind, true, shouldBind, shouldBind);
        OffsetConfig offsets = new OffsetConfig().setX(-0.1);
        DisableConfig playerHead = new DisableConfig("player_head", textureId, false, new UVRectangle[]{new UVRectangle(0, 0, 1.0f, 0.25f)});
        DisableConfig dragonHead = new DisableConfig("dragon_head", "minecraft:textures/entity/enderdragon/dragon.png", true, new UVRectangle[0]);
        DisableConfig[] disableConfigs = new DisableConfig[]{playerHead, dragonHead};
        return new BindingTarget(name, textureId, priority, 0.1f, targetConfig, bindConfig, offsets, disableConfigs);
    }

    public boolean isEmpty() {
        return name == null || name.isEmpty();
    }

    public boolean fixed() {
        return fixedNames.contains(name);
    }

    public DisableConfig[] filteredDisableConfigs(Predicate<DisableConfig> filter) {
        return Arrays.stream(disableConfigs).filter(filter).toArray(DisableConfig[]::new);
    }

    public record TargetConfig(float forwardU, float forwardV, float upwardU, float upwardV, float posU, float posV) {}

    public record BindConfig(boolean bindX, boolean bindY, boolean bindZ, boolean bindRotation) {}

    public static class OffsetConfig {
        private double scale = 1, x = 0, y = 0, z = 0;
        private float pitch = 0, yaw = 0, roll = 0;

        public double getScale() {
            return scale;
        }

        public OffsetConfig setScale(double scale) {
            this.scale = scale;
            return this;
        }

        public double getX() {
            return x;
        }

        public OffsetConfig setX(double x) {
            this.x = Mth.clamp(x, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
            return this;
        }

        public double getY() {
            return y;
        }

        public OffsetConfig setY(double y) {
            this.y = Mth.clamp(y, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
            return this;
        }

        public double getZ() {
            return z;
        }

        public OffsetConfig setZ(double z) {
            this.z = Mth.clamp(z, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
            return this;
        }

        public float getPitch() {
            return pitch;
        }

        public OffsetConfig setPitch(float pitch) {
            this.pitch = Mth.wrapDegrees(pitch);
            return this;
        }

        public float getYaw() {
            return yaw;
        }

        public OffsetConfig setYaw(float yaw) {
            this.yaw = Mth.wrapDegrees(yaw);
            return this;
        }

        public float getRoll() {
            return roll;
        }

        public OffsetConfig setRoll(float roll) {
            this.roll = Mth.wrapDegrees(roll);
            return this;
        }
    }

    public record DisableConfig(String name, String textureId, boolean disableAll, UVRectangle[] rectangles) implements Predicate<VertexData> {
        @Override
        public boolean test(VertexData vertexData) {
            final float u = vertexData.u(), v = vertexData.v();
            for (UVRectangle rectangle : rectangles) {
                if (rectangle.contains(u, v)) return true;
            }
            return false;
        }
    }

    public record UVRectangle(float uMin, float vMin, float uMax, float vMax) {
        public boolean contains(float u, float v) {
            return u >= uMin && u <= uMax && v >= vMin && v <= vMax;
        }
    }
}
