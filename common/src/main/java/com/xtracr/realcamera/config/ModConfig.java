package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class ModConfig {
    public static final double MIN_DOUBLE = -1.0;
    public static final double MAX_DOUBLE = 1.0;
    public boolean enabled = false;
    public boolean isClassic = false;
    public boolean dynamicCrosshair = false;
    public boolean renderModel = true;
    public double adjustStep = 0.01d;
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
        adjustStep = Mth.clamp(adjustStep, 0.0d, MAX_DOUBLE);
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
            BindingTarget target = RealCameraCore.currentTarget();
            if (target.isEmpty()) return;
            if (binding.adjustOffset) target.setOffsetX(target.getOffsetX() + count * adjustStep);
            else target.setRoll(target.getRoll() + count * 100 * (float) adjustStep);
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
            BindingTarget target = RealCameraCore.currentTarget();
            if (target.isEmpty()) return;
            if (binding.adjustOffset) target.setOffsetY(target.getOffsetY() + count * adjustStep);
            else target.setYaw(target.getYaw() + count * 100 * (float) adjustStep);
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
            BindingTarget target = RealCameraCore.currentTarget();
            if (target.isEmpty()) return;
            if (binding.adjustOffset) target.setOffsetZ(target.getOffsetZ() + count * adjustStep);
            else target.setPitch(target.getPitch() + count * 100 * (float) adjustStep);
        }
    }

    // classic
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
    public boolean renderStuckObjects() {
        return binding.renderStuckObjects;
    }

    public boolean rerenderModel() {
        return binding.rerenderModel;
    }

    public List<String> getDisableRenderItems() {
        return binding.disableRenderItems;
    }

    public List<BindingTarget> getFixedTargetList() {
        binding.clamp();
        return binding.fixedTargetList;
    }

    public BindingTarget findFixedTarget(String name) {
        return binding.fixedTargetList.stream().filter(target -> target.name.equals(name)).findFirst().orElse(binding.fixedTargetList.getFirst());
    }

    public List<BindingTarget> getTargetList() {
        binding.clamp();
        return binding.targetList;
    }

    public static class Classic {
        public AdjustMode adjustMode = AdjustMode.CAMERA;
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
            scale = Mth.clamp(scale, 0.0, 64.0);
            cameraX = Mth.clamp(cameraX, MIN_DOUBLE, MAX_DOUBLE);
            cameraY = Mth.clamp(cameraY, MIN_DOUBLE, MAX_DOUBLE);
            cameraZ = Mth.clamp(cameraZ, MIN_DOUBLE, MAX_DOUBLE);
            centerX = Mth.clamp(centerX, MIN_DOUBLE, MAX_DOUBLE);
            centerY = Mth.clamp(centerY, MIN_DOUBLE, MAX_DOUBLE);
            centerZ = Mth.clamp(centerZ, MIN_DOUBLE, MAX_DOUBLE);
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
        public boolean adjustOffset = true;
        public boolean renderStuckObjects = true;
        public boolean rerenderModel = false;
        public List<String> disableRenderItems = defaultDisableRenderItems;
        public List<BindingTarget> fixedTargetList = new ArrayList<>(BindingTarget.fixedTargets);
        public List<BindingTarget> targetList = new ArrayList<>(BindingTarget.defaultTargets);

        private static void putTarget(BindingTarget target, List<BindingTarget> list) {
            IntStream.range(0, list.size())
                    .filter(i -> list.get(i).name.equals(target.name))
                    .findAny()
                    .ifPresentOrElse(i -> list.set(i, target), () -> list.add(target));
            list.sort(Comparator.comparingInt(t -> -t.getPriority()));
        }

        private void clamp() {
            if (disableRenderItems == null) disableRenderItems = List.of();
            if (fixedTargetList == null || fixedTargetList.isEmpty()) fixedTargetList = new ArrayList<>(BindingTarget.fixedTargets);
            if (targetList == null || targetList.isEmpty()) targetList = new ArrayList<>(BindingTarget.defaultTargets);
            fixedTargetList.removeIf(target -> !target.fixed());
        }

        public void putTarget(BindingTarget target) {
            if (target.isEmpty()) return;
            if (target.fixed()) putTarget(target, fixedTargetList);
            else putTarget(target, targetList);
        }
    }
}
