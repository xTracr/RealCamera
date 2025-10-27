package com.xtracr.realcamera.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class ModConfig {
    public static final double MIN_OFFSET_D = -1.0, MAX_OFFSET_D = 1.0;
    public static final float MIN_OFFSET_F = -1.0f, MAX_OFFSET_F = 1.0f;
    public boolean enabled = false;
    public boolean isClassic = false;
    public boolean dynamicCrosshair = false;
    public boolean renderModel = true;
    public double adjustStep = 0.01;
    public Classic classic = new Classic();
    public Binding binding = new Binding();

    public void set(ModConfig modConfig) {
        enabled = modConfig.enabled;
        isClassic = modConfig.isClassic;
        dynamicCrosshair = modConfig.dynamicCrosshair;
        renderModel = modConfig.renderModel;
        adjustStep = modConfig.adjustStep;
        classic = modConfig.classic;
        binding = modConfig.binding;
    }

    public void clamp() {
        adjustStep = Mth.clamp(adjustStep, 0.0, MAX_OFFSET_D);
        classic.clamp();
        binding.clamp();
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public boolean isClassic() {
        return isClassic;
    }

    public void setClassic(boolean value) {
        isClassic = value;
    }

    public boolean dynamicCrosshair() {
        return dynamicCrosshair;
    }

    public boolean renderModel() {
        return renderModel;
    }

    public void cycleAdjustMode() {
        if (isClassic) classic.adjustMode = classic.adjustMode.cycle();
        else binding.adjustOffset = !binding.adjustOffset;
    }

    public void adjustOffsetX(int count) {
        if (isClassic) {
            switch (classic.adjustMode) {
                case CENTER -> classic.centerX += count * adjustStep;
                case ROTATION -> classic.roll += count * 100 * (float) adjustStep;
                default -> classic.cameraX += count * adjustStep;
            }
            classic.clamp();
        } else {
            BindTarget target = RealCameraCore.currentTarget();
            if (binding.adjustOffset) target.offsets().setX(target.offsets().getX() + count * (float) adjustStep);
            else target.offsets().setRoll(target.offsets().getRoll() + count * 100 * (float) adjustStep);
        }
    }

    public void adjustOffsetY(int count) {
        if (isClassic) {
            switch (classic.adjustMode) {
                case CENTER -> classic.centerY += count * adjustStep;
                case ROTATION -> classic.yaw += count * 100 * (float) adjustStep;
                default -> classic.cameraY += count * adjustStep;
            }
            classic.clamp();
        } else {
            BindTarget target = RealCameraCore.currentTarget();
            if (binding.adjustOffset) target.offsets().setY(target.offsets().getY() + count * (float) adjustStep);
            else target.offsets().setYaw(target.offsets().getYaw() + count * 100 * (float) adjustStep);
        }
    }

    public void adjustOffsetZ(int count) {
        if (isClassic) {
            switch (classic.adjustMode) {
                case CENTER -> classic.centerZ += count * adjustStep;
                case ROTATION -> classic.pitch += count * 100 * (float) adjustStep;
                default -> classic.cameraZ += count * adjustStep;
            }
            classic.clamp();
        } else {
            BindTarget target = RealCameraCore.currentTarget();
            if (binding.adjustOffset) target.offsets().setZ(target.offsets().getZ() + count * (float) adjustStep);
            else target.offsets().setPitch(target.offsets().getPitch() + count * 100 * (float) adjustStep);
        }
    }

    public void setActiveConfigIndex(int value) {
        binding.activeConfigIndex = value == 0 ? 0 : Mth.clamp(binding.activeConfigIndex += value, 0, Integer.MAX_VALUE);
    }

    // classic
    public boolean classicDisableWhenSneaking() {
        return classic.disableWhenSneaking;
    }

    public boolean classicDisableWhenSwimming() {
        return classic.disableWhenSwimming;
    }

    public int getClassicSwimOutTick() {
        return classic.swimOutTick;
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

    // binding
    public InputConstants.Key getScreenModifierKey() {
        return InputConstants.getKey(binding.screenModifierKey);
    }

    public boolean legacyBindingMode() {
        return binding.legacyBindingMode;
    }

    public boolean renderStuckObjects() {
        return binding.renderStuckObjects;
    }

    public boolean hideBindingFailureMessage() {
        return binding.hideFailureMessage;
    }

    public boolean bindingDisableWhenSneaking() {
        return binding.disableWhenSneaking;
    }

    public boolean bindingDisableWhenSwimming() {
        return binding.disableWhenSwimming;
    }

    public int getBindingSwimOutTick() {
        return binding.swimOutTick;
    }

    public int getBindResultRetentionFrames() {
        return binding.bindResultRetentionFrames;
    }

    public double getDisplacementSmoothFactor() {
        return binding.displacementSmoothFactor;
    }

    public double getRotationSmoothFactor() {
        return binding.rotationSmoothFactor;
    }

    public List<String> getDisableMainFeatureItems() {
        return binding.disableMainFeatureItems;
    }

    public List<String> getDisableRenderItems() {
        return binding.disableRenderItems;
    }

    public List<BindTarget> getFixedTargetList() {
        return binding.fixedTargetList;
    }

    public List<BindTarget> getBindTargetList() {
        binding.clamp();
        return binding.targetList;
    }

    public List<BindTarget> getTargetList(String textureId) {
        binding.clamp();
        int activeConfigIndex = binding.activeConfigIndex;
        List<BindTarget> matchedTargets = binding.targetList.stream().filter(target -> textureId.contains(target.textureId())).toList();
        if (activeConfigIndex == 0 || matchedTargets.isEmpty()) return matchedTargets;
        return List.of(matchedTargets.get(Math.min(activeConfigIndex - 1, matchedTargets.size() - 1)));
    }

    public void putBindTarget(BindTarget target) {
        if (target.isEmpty()) return;
        List<BindTarget> fixedList = binding.fixedTargetList;
        for (int i = 0, size = fixedList.size(); i < size; i++) {
            if (fixedList.get(i).name().equals(target.name())) {
                fixedList.set(i, target);
                return;
            }
        }
        List<BindTarget> list = binding.targetList;
        IntStream.range(0, list.size())
                .filter(i -> list.get(i).name().equals(target.name()))
                .findAny()
                .ifPresentOrElse(i -> list.set(i, target), () -> list.add(target));
        list.sort(Comparator.comparingInt(t -> -t.priority()));
    }

    public static class Classic {
        public AdjustMode adjustMode = AdjustMode.CAMERA;
        public boolean disableWhenSneaking = false;
        public boolean disableWhenSwimming = false;
        public int swimOutTick = 13;
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
            swimOutTick = Mth.clamp(swimOutTick, 0, 40);
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
        protected static final List<String> defaultDisableRenderItems = List.of("minecraft:filled_map");
        public String screenModifierKey = InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LALT).getName();
        public boolean legacyBindingMode = false;
        public boolean adjustOffset = true;
        public boolean hideFailureMessage = false;
        public boolean renderStuckObjects = true;
        public boolean disableWhenSneaking = false;
        public boolean disableWhenSwimming = false;
        public int swimOutTick = 13;
        public int bindResultRetentionFrames = 2;
        public double displacementSmoothFactor = 0.4;
        public double rotationSmoothFactor = 0.4;
        public int activeConfigIndex = 0;
        public List<String> disableMainFeatureItems = List.of();
        public List<String> disableRenderItems = defaultDisableRenderItems;
        public List<BindTarget> fixedTargetList = new ArrayList<>();
        public List<BindTarget> targetList = new ArrayList<>(BindTarget.defaultTargets);

        private void clamp() {
            try {
                InputConstants.getKey(screenModifierKey);
            } catch (Exception e) {
                screenModifierKey = InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LALT).getName();
            }
            swimOutTick = Mth.clamp(swimOutTick, 0, 40);
            bindResultRetentionFrames = Math.max(bindResultRetentionFrames, 0);
            displacementSmoothFactor = Mth.clamp(displacementSmoothFactor, 0.0, 1.0);
            rotationSmoothFactor = Mth.clamp(rotationSmoothFactor, 0.0, 1.0);
            if (disableMainFeatureItems == null) disableMainFeatureItems = List.of();
            if (disableRenderItems == null) disableRenderItems = List.of();
            if (fixedTargetList == null) fixedTargetList = new ArrayList<>();
            else fixedTargetList.removeIf(BindTarget::isEmpty);
            if (targetList == null) targetList = new ArrayList<>(BindTarget.defaultTargets);
            else {
                targetList.removeIf(BindTarget::isEmpty);
                if (targetList.isEmpty()) targetList = new ArrayList<>(BindTarget.defaultTargets);
            }
        }
    }
}