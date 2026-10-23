package com.xtracr.realcamera.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.config.binding.TargetConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class ModConfig {
    public static final double MIN_OFFSET_D = -1.0, MAX_OFFSET_D = 1.0;
    public static final float MIN_OFFSET_F = -1.0f, MAX_OFFSET_F = 1.0f;
    public final Classic classic = new Classic();
    public final Binding binding = new Binding();
    public boolean enabled = false;
    public boolean isClassic = false;
    public boolean dynamicCrosshair = false;
    public boolean renderModel = true;
    public double adjustStep = 0.01;

    public void clamp() {
        adjustStep = Mth.clamp(adjustStep, 0.0, MAX_OFFSET_D);
        classic.clamp();
        binding.clamp();
    }

    public void cycleAdjustMode() {
        if (isClassic) classic.adjustMode = classic.adjustMode.cycle();
        else binding.adjustOffset = !binding.adjustOffset;
    }

    public void adjustOffsetX(int count, @Nullable OffsetConfig offsets) {
        if (isClassic) {
            switch (classic.adjustMode) {
                case CENTER -> classic.centerX += count * adjustStep;
                case ROTATION -> classic.roll += count * 100 * (float) adjustStep;
                default -> classic.cameraX += count * adjustStep;
            }
            classic.clamp();
        } else if (offsets != null) {
            if (binding.adjustOffset) offsets.x += count * (float) adjustStep;
            else offsets.roll += count * 100 * (float) adjustStep;
            offsets.clamp();
        }
    }

    public void adjustOffsetY(int count, @Nullable OffsetConfig offsets) {
        if (isClassic) {
            switch (classic.adjustMode) {
                case CENTER -> classic.centerY += count * adjustStep;
                case ROTATION -> classic.yaw += count * 100 * (float) adjustStep;
                default -> classic.cameraY += count * adjustStep;
            }
            classic.clamp();
        } else if (offsets != null) {
            if (binding.adjustOffset) offsets.y += count * (float) adjustStep;
            else offsets.yaw += count * 100 * (float) adjustStep;
            offsets.clamp();
        }
    }

    public void adjustOffsetZ(int count, @Nullable OffsetConfig offsets) {
        if (isClassic) {
            switch (classic.adjustMode) {
                case CENTER -> classic.centerZ += count * adjustStep;
                case ROTATION -> classic.pitch += count * 100 * (float) adjustStep;
                default -> classic.cameraZ += count * adjustStep;
            }
            classic.clamp();
        } else if (offsets != null) {
            if (binding.adjustOffset) offsets.z += count * (float) adjustStep;
            else offsets.pitch += count * 100 * (float) adjustStep;
            offsets.clamp();
        }
    }

    public double getClassicX() {
        return classic.cameraX * classic.scale;
    }

    public double getClassicY() {
        return classic.cameraY * classic.scale;
    }

    public double getClassicZ() {
        return classic.cameraZ * classic.scale;
    }

    public double getCenterX() {
        return classic.centerX * classic.scale;
    }

    public double getCenterY() {
        return classic.centerY * classic.scale;
    }

    public double getCenterZ() {
        return classic.centerZ * classic.scale;
    }

    public float getClassicPitch() {
        return classic.pitch;
    }

    public float getClassicYaw() {
        return classic.yaw;
    }

    public float getClassicRoll() {
        return classic.roll;
    }

    public BindTarget getBindTarget(Identifier id) {
        return new BindTarget(id, binding.targetConfigs.get(id), binding.renderConfig(id), binding.offsets(id), binding.priority(id));
    }

    public List<BindTarget> getBindTargets() {
        Set<Identifier> ids = new LinkedHashSet<>(binding.targetConfigs.keySet());
        ids.addAll(binding.offsetConfigs.keySet());
        ids.addAll(binding.renderConfigs.keySet());
        return ids.stream().map(this::getBindTarget).sorted(Comparator.comparingInt(BindTarget::priority).reversed()).toList();
    }

    public void putBindTarget(BindTarget target) {
        if (target.targetConfig() == null) binding.targetConfigs.remove(target.id());
        else binding.targetConfigs.put(target.id(), target.targetConfig());
        binding.offsetConfigs.put(target.id(), target.offsets());
        binding.renderConfigs.put(target.id(), target.renderConfig());
        binding.priorities.put(target.id(), target.priority());
    }

    public void removeBindTarget(Identifier id) {
        binding.targetConfigs.remove(id);
        binding.offsetConfigs.remove(id);
        binding.renderConfigs.remove(id);
        binding.priorities.remove(id);
    }

    public static class Classic {
        public AdjustMode adjustMode = AdjustMode.CAMERA;
        public boolean disableWhenSneaking = false;
        public boolean disableWhenSwimming = false;
        public int exitTick = 13;
        public double scale = 8.0;
        public double cameraX = -0.5;
        public double cameraY = 0.04;
        public double cameraZ = -0.15;
        public double centerX = 0.0;
        public double centerY = 0.0;
        public double centerZ = 0.0;
        public float pitch = 0.0f;
        public float yaw = 18.0f;
        public float roll = 0.0f;

        private void clamp() {
            if (adjustMode == null) adjustMode = AdjustMode.CAMERA;
            exitTick = Mth.clamp(exitTick, 0, 40);
            scale = Mth.clamp(scale, 0.0, 64.0);
            cameraX = Mth.clamp(cameraX, MIN_OFFSET_D, MAX_OFFSET_D);
            cameraY = Mth.clamp(cameraY, MIN_OFFSET_D, MAX_OFFSET_D);
            cameraZ = Mth.clamp(cameraZ, MIN_OFFSET_D, MAX_OFFSET_D);
            centerX = Mth.clamp(centerX, MIN_OFFSET_D, MAX_OFFSET_D);
            centerY = Mth.clamp(centerY, MIN_OFFSET_D, MAX_OFFSET_D);
            centerZ = Mth.clamp(centerZ, MIN_OFFSET_D, MAX_OFFSET_D);
            pitch = Mth.wrapDegrees(pitch);
            yaw = Mth.wrapDegrees(yaw);
            roll = Mth.wrapDegrees(roll);
        }

        public enum AdjustMode {
            CAMERA, CENTER, ROTATION;

            private static final AdjustMode[] VALUES = values();

            public AdjustMode cycle() {
                return VALUES[(ordinal() + 1) % VALUES.length];
            }
        }
    }

    public static class Binding {
        protected static final List<String> DEFAULT_DISABLE_RENDER_ITEMS = List.of("minecraft:filled_map");
        public String screenModifierKey = InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LALT).getName();
        public boolean legacyMode = false;
        public boolean adjustOffset = true;
        public boolean hideFailureMessage = false;
        public boolean renderStuckObjects = true;
        public boolean disableWhenSneaking = false;
        public boolean disableWhenSwimming = false;
        public boolean disableWhenFlying = false;
        public int exitTick = 13;
        public int bindResultRetentionFrames = 2;
        public int activeConfigIndex = 0;
        public double displacementSmoothFactor = 0.4;
        public double rotationSmoothFactor = 0.4;
        public List<String> disableMainFeatureItems = List.of();
        public List<String> disableRenderItems = DEFAULT_DISABLE_RENDER_ITEMS;
        public Map<Identifier, TargetConfig> targetConfigs = new LinkedHashMap<>();
        public Map<Identifier, OffsetConfig> offsetConfigs = new LinkedHashMap<>();
        public Map<Identifier, RenderConfig> renderConfigs = new LinkedHashMap<>();
        @Deprecated
        public Map<Identifier, Integer> priorities = new LinkedHashMap<>();

        public Binding() {
            for (var target : List.of(
                    BindTarget.vanillaTarget("minecraft_head", false),
                    BindTarget.vanillaTarget("skin_head", false),
                    BindTarget.vanillaTarget("minecraft_head_2", true),
                    BindTarget.vanillaTarget("skin_head_2", true))) {
                targetConfigs.put(target.id(), target.targetConfig());
                offsetConfigs.put(target.id(), target.offsets());
                renderConfigs.put(target.id(), target.renderConfig());
            }
        }

        public OffsetConfig offsets(Identifier id) {
            return offsetConfigs.computeIfAbsent(id, _ -> new OffsetConfig());
        }

        public RenderConfig renderConfig(Identifier id) {
            return renderConfigs.computeIfAbsent(id, _ -> new RenderConfig());
        }

        @Deprecated
        public int priority(Identifier id) {
            return priorities.getOrDefault(id, BindTarget.defaultPriority(id));
        }

        private void clamp() {
            try {
                InputConstants.getKey(screenModifierKey);
            } catch (Exception e) {
                screenModifierKey = InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LALT).getName();
            }
            exitTick = Mth.clamp(exitTick, 0, 40);
            bindResultRetentionFrames = Math.max(bindResultRetentionFrames, 0);
            activeConfigIndex = Math.max(activeConfigIndex, 0);
            displacementSmoothFactor = Mth.clamp(displacementSmoothFactor, 0.0, 1.0);
            rotationSmoothFactor = Mth.clamp(rotationSmoothFactor, 0.0, 1.0);
            if (disableMainFeatureItems == null) disableMainFeatureItems = List.of();
            if (disableRenderItems == null) disableRenderItems = List.of();
            if (targetConfigs == null) targetConfigs = new LinkedHashMap<>();
            if (offsetConfigs == null) offsetConfigs = new LinkedHashMap<>();
            if (renderConfigs == null) renderConfigs = new LinkedHashMap<>();
            if (priorities == null) priorities = new LinkedHashMap<>();
            targetConfigs.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().textureId() == null);
            offsetConfigs.values().removeIf(Objects::isNull);
            renderConfigs.values().removeIf(Objects::isNull);
            priorities.values().removeIf(Objects::isNull);
            offsetConfigs.values().forEach(OffsetConfig::clamp);
        }
    }
}
