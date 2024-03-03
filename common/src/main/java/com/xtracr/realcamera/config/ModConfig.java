package com.xtracr.realcamera.config;

import com.xtracr.realcamera.util.Triple;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.stream.IntStream;

public class ModConfig {
    protected static final double MIN_DOUBLE = -64.0d;
    protected static final double MAX_DOUBLE = 64.0d;

    public General general = new General();
    public Binding binding = new Binding();
    public Classic classic = new Classic();
    public Compats compats = new Compats();
    public Disable disable = new Disable();

    public static <L, M, R> void resetTripleIfNull(Triple<L, M, R> triple, Triple<L, M, R> source) {
        if (triple == null) triple = source;
        if (triple.getLeft() == null) triple.setLeft(source.getLeft());
        if (triple.getMiddle() == null) triple.setMiddle(source.getMiddle());
        if (triple.getRight() == null) triple.setRight(source.getRight());
    }

    public void set(ModConfig modConfig) {
        general = modConfig.general;
        binding = modConfig.binding;
        classic = modConfig.classic;
        compats = modConfig.compats;
        disable = modConfig.disable;
    }

    public void clamp() {
        general.clamp();
        binding.clamp();
        classic.clamp();
        disable.clamp();
    }

    public boolean isEnabled() {
        return general.enabled;
    }

    public void setEnabled(boolean value) {
        general.enabled = value;
    }

    public boolean isClassic() {
        return general.classic;
    }

    public void setClassic(boolean value) {
        general.classic = value;
    }

    public boolean isCrosshairDynamic() {
        return general.dynamicCrosshair;
    }

    public boolean isRendering() {
        return general.renderModel;
    }

    public double getAdjustStep() {
        return general.adjustStep;
    }

    public double getScale() {
        return general.scale * 0.0625D;
    }

    // binding
    public VanillaModelPart getVanillaModelPart() {
        return binding.vanillaModelPart;
    }

    public boolean isAdjustingOffset() {
        return binding.adjustOffset;
    }

    public BindingTarget getTarget() {
        return binding.targetList.stream().filter(t -> t.name().equals(binding.targetName)).findAny().orElse(null);
    }

    public boolean isXBound() {
        return binding.bindX;
    }

    public boolean isYBound() {
        return binding.bindY;
    }

    public boolean isZBound() {
        return binding.bindZ;
    }

    public double getBindingX() {
        return binding.cameraX;
    }

    public double getBindingY() {
        return binding.cameraY;
    }

    public double getBindingZ() {
        return binding.cameraZ;
    }

    public boolean isRotationBound() {
        return binding.bindRotation;
    }

    public float getBindingPitch() {
        return binding.pitch;
    }

    public float getBindingYaw() {
        return binding.yaw;
    }

    public float getBindingRoll() {
        return binding.roll;
    }

    public void setAdjustOffset(boolean value) {
        binding.adjustOffset = value;
    }

    public void putTarget(BindingTarget target) {
        if (target.isEmpty()) return;
        IntStream.range(0, binding.targetList.size())
                .filter(i -> binding.targetList.get(i).name().equals(target.name()))
                .findAny()
                .ifPresentOrElse(i -> binding.targetList.set(i, target), () -> binding.targetList.add(target));
    }

    public void adjustBindingX(boolean add) {
        int s = add ? 1 : -1;
        if (isAdjustingOffset()) binding.cameraX += s * getAdjustStep();
        else binding.roll += s * 4 * (float) getAdjustStep();
        binding.clamp();
    }

    public void adjustBindingY(boolean add) {
        int s = add ? 1 : -1;
        if (isAdjustingOffset()) binding.cameraY += s * getAdjustStep();
        else binding.yaw += s * 4 * (float) getAdjustStep();
        binding.clamp();
    }

    public void adjustBindingZ(boolean add) {
        int s = add ? 1 : -1;
        if (isAdjustingOffset()) binding.cameraZ += s * getAdjustStep();
        else binding.pitch += s * 4 * (float) getAdjustStep();
        binding.clamp();
    }

    // classic
    public double getClassicX() {
        return classic.cameraX;
    }

    public double getClassicY() {
        return classic.cameraY;
    }

    public double getClassicZ() {
        return classic.cameraZ;
    }

    public double getCenterX() {
        return classic.centerX;
    }

    public double getCenterY() {
        return classic.centerY;
    }

    public double getCenterZ() {
        return classic.centerZ;
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

    public void cycleClassicAdjustMode() {
        classic.adjustMode = classic.adjustMode.cycle();
    }

    public void adjustClassicX(int count) {
        switch (classic.adjustMode) {
            case CENTER -> classic.centerX += count * getAdjustStep();
            case ROTATION -> classic.roll += count * 4 * (float) getAdjustStep();
            default -> classic.cameraX += count * getAdjustStep();
        }
        classic.clamp();
    }

    public void adjustClassicY(int count) {
        switch (classic.adjustMode) {
            case CENTER -> classic.centerY += count * getAdjustStep();
            case ROTATION -> classic.yaw += count * 4 * (float) getAdjustStep();
            default -> classic.cameraY += count * getAdjustStep();
        }
        classic.clamp();
    }

    public void adjustClassicZ(int count) {
        switch (classic.adjustMode) {
            case CENTER -> classic.centerZ += count * getAdjustStep();
            case ROTATION -> classic.pitch += count * 4 * (float) getAdjustStep();
            default -> classic.cameraZ += count * getAdjustStep();
        }
        classic.clamp();
    }

    // compats
    public boolean isUsingModModel() {
        return compats.useModModel;
    }

    public String getModelModID() {
        return compats.modelModID;
    }

    public String getModModelPartName() {
        return compats.modModelPart;
    }

    public boolean compatPehkui() {
        return compats.pehkui;
    }

    public boolean compatPhysicsMod() {
        return compats.physicsMod;
    }

    // disable
    private boolean shouldDisable(MinecraftClient client, String action) {
        boolean b = false;
        for (Triple<String, List<String>, List<String>> triple : disable.customConditions) {
            if (!triple.getRight().contains(action)) continue;
            String behavior = triple.getLeft();
            b = b || (client.player.isHolding(stack ->
                    triple.getMiddle().contains(Registries.ITEM.getId(stack.getItem()).toString())) &&
                    (behavior.equals("holding") ||
                            (behavior.equals("attacking") && client.options.attackKey.isPressed()) ||
                            (behavior.equals("using") && client.options.useKey.isPressed())));
        }
        return b;
    }

    public boolean shouldDisableModelPart(String modelPartName) {
        if (disable.onlyInBinding && general.classic) return false;
        return (disable.renderModelPart && disable.disabledModelParts.contains(modelPartName)) ||
                shouldDisable(MinecraftClient.getInstance(), modelPartName);
    }

    public boolean allowRenderingHand(MinecraftClient client) {
        if (disable.onlyInBinding && general.classic) return false;
        return shouldDisable(client, "allow_rendering_hand");
    }

    public boolean shouldDisableMod(MinecraftClient client) {
        if (disable.onlyInBinding && general.classic) return false;
        return shouldDisable(client, "disable_mod") ||
                (client.player.isFallFlying() && disable.fallFlying) ||
                (client.player.isSwimming() && disable.swimming) ||
                (client.player.isCrawling() && disable.crawling) ||
                (client.player.isSneaking() && disable.sneaking) ||
                (client.player.isSleeping() && disable.sleeping) ||
                (client.currentScreen != null && disable.screenOpened);
    }

    public boolean shouldDisableRendering(MinecraftClient client) {
        if (disable.onlyInBinding && general.classic) return false;
        return shouldDisable(client, "disable_rendering");
    }

    public static class General {
        public boolean enabled = false;
        public boolean classic = false;
        public boolean dynamicCrosshair = false;
        public boolean renderModel = true;
        public double adjustStep = 0.25D;
        public double scale = 1.0d;

        private void clamp() {
            adjustStep = MathHelper.clamp(adjustStep, 0.0d, MAX_DOUBLE);
            scale = MathHelper.clamp(scale, 0.0d, MAX_DOUBLE);
        }
    }

    public static class Binding {
        public List<BindingTarget> targetList = new ArrayList<>(BindingTarget.defaultTargets);
        public VanillaModelPart vanillaModelPart = VanillaModelPart.head;
        public boolean experimental = false;
        public boolean adjustOffset = true;
        public boolean autoBind = true;
        public String targetName = "minecraft_head";
        public boolean bindX = true;
        public boolean bindY = true;
        public boolean bindZ = true;
        public double cameraX = 0.0d;
        public double cameraY = 0.0d;
        public double cameraZ = 0.0d;
        public boolean bindRotation = true;
        public float pitch = 0.0f;
        public float yaw = 0.0f;
        public float roll = 0.0f;

        private void clamp() {
            if (vanillaModelPart == null) vanillaModelPart = VanillaModelPart.head;
            if (targetList == null || targetList.isEmpty()) targetList = new ArrayList<>(BindingTarget.defaultTargets);
            cameraX = MathHelper.clamp(cameraX, MIN_DOUBLE, MAX_DOUBLE);
            cameraY = MathHelper.clamp(cameraY, MIN_DOUBLE, MAX_DOUBLE);
            cameraZ = MathHelper.clamp(cameraZ, MIN_DOUBLE, MAX_DOUBLE);
            pitch = MathHelper.wrapDegrees(pitch);
            yaw = MathHelper.wrapDegrees(yaw);
            roll = MathHelper.wrapDegrees(roll);
        }
    }

    public static class Classic {
        public AdjustMode adjustMode = AdjustMode.CAMERA;
        public double cameraX = -60.0d;
        public double cameraY = 2.0d;
        public double cameraZ = -16.0d;
        public double centerX = 0.0d;
        public double centerY = -3.4D;
        public double centerZ = 0.0d;
        public float pitch = 0.0f;
        public float yaw = 18.0f;
        public float roll = 0.0f;

        private void clamp() {
            if (adjustMode == null) adjustMode = AdjustMode.CAMERA;
            cameraX = MathHelper.clamp(cameraX, MIN_DOUBLE, MAX_DOUBLE);
            cameraY = MathHelper.clamp(cameraY, MIN_DOUBLE, MAX_DOUBLE);
            cameraZ = MathHelper.clamp(cameraZ, MIN_DOUBLE, MAX_DOUBLE);
            centerX = MathHelper.clamp(centerX, MIN_DOUBLE, MAX_DOUBLE);
            centerY = MathHelper.clamp(centerY, MIN_DOUBLE, MAX_DOUBLE);
            centerZ = MathHelper.clamp(centerZ, MIN_DOUBLE, MAX_DOUBLE);
            pitch = MathHelper.wrapDegrees(pitch);
            yaw = MathHelper.wrapDegrees(yaw);
            roll = MathHelper.wrapDegrees(roll);
        }

        public enum AdjustMode {
            CAMERA, CENTER, ROTATION;

            private static final AdjustMode[] VALUES = values();

            public AdjustMode cycle() {
                return VALUES[(ordinal() + 1) % VALUES.length];
            }
        }
    }

    public static class Compats {
        public boolean useModModel = false;
        public String modelModID = "minecraft";
        public String modModelPart = "head";
        public boolean pehkui = true;
        public boolean physicsMod = true;
    }

    public static class Disable {
        public static final Set<String> optionalParts = new HashSet<>(Set.of("head", "hat", "slot_head"));
        protected static final List<String> defaultParts = List.of("head", "hat", "slot_head");
        protected static final Triple<String, List<String>, List<String>> defaultTriple = new Triple<>
                ("holding", List.of("new item id"), List.of("new action"));
        protected static final List<Triple<String, List<String>, List<String>>> defaultConditions = List.of(
                new Triple<>("using", List.of("minecraft:spyglass"), List.of("disable_rendering")),
                new Triple<>("holding", List.of("Example--minecraft:filled_map"), Arrays.asList(
                        "allow_rendering_hand", "leftArm", "rightArm", "leftSleeve", "rightSleeve", "heldItem")));
        protected static final String[] behaviors = {"holding", "attacking", "using"};

        public boolean onlyInBinding = true;
        public boolean renderModelPart = false;
        public List<String> disabledModelParts = new ArrayList<>(defaultParts);
        public List<Triple<String, List<String>, List<String>>> customConditions = new ArrayList<>(defaultConditions);
        public boolean fallFlying = true;
        public boolean swimming = false;
        public boolean crawling = false;
        public boolean sneaking = false;
        public boolean sleeping = false;
        public boolean screenOpened = false;

        private void clamp() {
            if (disabledModelParts == null) disabledModelParts = new ArrayList<>(defaultParts);
            if (customConditions == null) customConditions = new ArrayList<>(defaultConditions);
            customConditions.forEach(triple -> resetTripleIfNull(triple, defaultTriple));
        }
    }
}
