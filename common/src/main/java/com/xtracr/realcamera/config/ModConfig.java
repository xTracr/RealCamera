package com.xtracr.realcamera.config;

import com.xtracr.realcamera.utils.Triple;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModConfig {
    protected static final double MIN_DOUBLE = -64.0D;
    protected static final double MAX_DOUBLE = 64.0D;

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

    public boolean doOffsetModel() {
        return binding.offsetModel;
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

    public boolean isPitchingBound() {
        return binding.bindPitching;
    }

    public boolean isYawingBound() {
        return binding.bindYawing;
    }

    public boolean isRollingBound() {
        return binding.bindRolling;
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

    public void adjustClassicX(boolean add) {
        int s = add ? 1 : -1;
        switch (classic.adjustMode) {
            case CENTER -> classic.centerX += s * getAdjustStep();
            case ROTATION -> classic.roll += s * 4 * (float) getAdjustStep();
            default -> classic.cameraX += s * getAdjustStep();
        }
        classic.clamp();
    }

    public void adjustClassicY(boolean add) {
        int s = add ? 1 : -1;
        switch (classic.adjustMode) {
            case CENTER -> classic.centerY += s * getAdjustStep();
            case ROTATION -> classic.yaw += s * 4 * (float) getAdjustStep();
            default -> classic.cameraY += s * getAdjustStep();
        }
        classic.clamp();
    }

    public void adjustClassicZ(boolean add) {
        int s = add ? 1 : -1;
        switch (classic.adjustMode) {
            case CENTER -> classic.centerZ += s * getAdjustStep();
            case ROTATION -> classic.pitch += s * 4 * (float) getAdjustStep();
            default -> classic.cameraZ += s * getAdjustStep();
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
                            triple.getMiddle().contains(Registry.ITEM.getId(stack.getItem()).toString())) &&
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
                (client.player.shouldLeaveSwimmingPose() && disable.crawling) ||
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
        public double scale = 1.0D;

        private void clamp() {
            adjustStep = MathHelper.clamp(adjustStep, 0.0D, MAX_DOUBLE);
            scale = MathHelper.clamp(scale, 0.0D, MAX_DOUBLE);
        }
    }

    public static class Binding {
        public VanillaModelPart vanillaModelPart = VanillaModelPart.head;
        public boolean adjustOffset = true;
        public boolean offsetModel = false;
        public double cameraX = 3.25D;
        public double cameraY = 2.0D;
        public double cameraZ = 0.0D;
        public boolean bindPitching = true;
        public boolean bindYawing = true;
        public boolean bindRolling = true;
        public float pitch = 0.0F;
        public float yaw = 0.0F;
        public float roll = 0.0F;

        private void clamp() {
            if (vanillaModelPart == null) vanillaModelPart = VanillaModelPart.head;
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
        public double cameraX = -60.0D;
        public double cameraY = 2.0D;
        public double cameraZ = -16.0D;
        public double centerX = 0.0D;
        public double centerY = -3.4D;
        public double centerZ = 0.0D;
        public float pitch = 0.0F;
        public float yaw = 18.0F;
        public float roll = 0.0F;

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
        protected static final List<String> defaultParts = Arrays.asList("head", "hat", "slot_head");
        protected static final Triple<String, List<String>, List<String>> defaultTriple = new Triple<>
                ("holding", List.of("new item id"), List.of("new action"));
        protected static final List<Triple<String, List<String>, List<String>>> defaultConditions = Arrays.asList(
                new Triple<>("using", List.of("minecraft:spyglass"), List.of("disable_rendering")),
                new Triple<>("holding", List.of("Example--minecraft:filled_map"), Arrays.asList(
                        "allow_rendering_hand", "leftArm", "rightArm", "leftSleeve", "rightSleeve", "heldItem")));
        protected static final String[] behaviors = {"holding", "attacking", "using"};

        public boolean onlyInBinding = true;
        public boolean renderModelPart = false;
        public List<String> disabledModelParts = defaultParts;
        public List<Triple<String, List<String>, List<String>>> customConditions = defaultConditions;
        public boolean fallFlying = true;
        public boolean swimming = false;
        public boolean crawling = false;
        public boolean sneaking = false;
        public boolean sleeping = false;
        public boolean screenOpened = false;

        private void clamp() {
            if (disabledModelParts == null) disabledModelParts = defaultParts;
            if (customConditions == null) customConditions = defaultConditions;
            customConditions.forEach(triple -> resetTripleIfNull(triple, defaultTriple));
        }
    }
}
