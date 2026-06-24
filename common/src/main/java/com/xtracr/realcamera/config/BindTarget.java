package com.xtracr.realcamera.config;

import java.util.List;
import java.util.function.Predicate;

public record BindTarget(
        String name, String textureId, int priority, float disablingDepth,
        TargetConfig targetConfig,
        BindConfig bindConfig,
        OffsetConfig offsets,
        List<DisableConfig> disableConfigs) {
    public static final List<BindTarget> DEFAULT_TARGETS;
    public static final BindTarget EMPTY = blank(null, null);

    static {
        DEFAULT_TARGETS = List.of(
                BindTarget.vanillaTarget("minecraft_head", 5, false),
                BindTarget.vanillaTarget("skin_head", 5, false),
                BindTarget.vanillaTarget("minecraft_head_2", 1, true),
                BindTarget.vanillaTarget("skin_head_2", 1, true)
        );
    }

    public static BindTarget blank(String name, String textureId) {
        TargetConfig targetConfig = new TargetConfig(0, 0, 0, 0, 0, 0);
        BindConfig bindConfig = new BindConfig(false, true, false, false);
        return new BindTarget(name, textureId, 0, 0.2f, targetConfig, bindConfig, new OffsetConfig(), List.of());
    }

    private static BindTarget vanillaTarget(String name, int priority, boolean shouldBind) {
        String textureId = name.contains("skin") ? "minecraft:skins/" : "minecraft:textures/entity/player/";
        TargetConfig targetConfig = new TargetConfig(0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f);
        BindConfig bindConfig = new BindConfig(shouldBind, true, shouldBind, shouldBind);
        OffsetConfig offsets = new OffsetConfig();
        offsets.x = -0.1f;
        DisableConfig playerHead = new DisableConfig("player_head", textureId, false, List.of(new UVRectangle(0, 0, 1.0f, 0.25f)));
        DisableConfig dragonHead = new DisableConfig("dragon_head", "minecraft:textures/entity/enderdragon/dragon.png", true, List.of());
        List<DisableConfig> disableConfigs = List.of(playerHead, dragonHead);
        return new BindTarget(name, textureId, priority, 0.2f, targetConfig, bindConfig, offsets, disableConfigs);
    }

    public boolean isEmpty() {
        return name == null || textureId == null || targetConfig == null || bindConfig == null || offsets == null || disableConfigs == null;
    }

    public DisableConfig[] filteredDisableConfigs(Predicate<DisableConfig> filter) {
        return disableConfigs.stream().filter(filter).toArray(DisableConfig[]::new);
    }

    public record TargetConfig(float forwardU, float forwardV, float upwardU, float upwardV, float posU, float posV) {
    }

    public record BindConfig(boolean bindX, boolean bindY, boolean bindZ, boolean bindRotation) {
    }
}
