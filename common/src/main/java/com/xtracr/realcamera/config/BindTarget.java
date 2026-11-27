package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.config.binding.TargetConfig;
import com.xtracr.realcamera.config.binding.UVRectangle;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Combines a path's data for editing and sharing; runtime solving uses TargetConfig only.
 */
public record BindTarget(
        Identifier id,
        TargetConfig targetConfig,
        RenderConfig renderConfig,
        OffsetConfig offsets,
        @Deprecated int priority
) {
    public BindTarget(Identifier id, TargetConfig targetConfig, RenderConfig renderConfig, OffsetConfig offsets) {
        this(id, targetConfig, renderConfig, offsets, defaultPriority(id));
    }

    public static int defaultPriority(Identifier id) {
        if (!id.getNamespace().equals(RealCamera.MOD_ID)) return 0;
        return switch (id.getPath()) {
            case "minecraft_head", "skin_head" -> 5;
            case "minecraft_head_2", "skin_head_2" -> 1;
            default -> 0;
        };
    }

    public static BindTarget blank(Identifier id) {
        return new BindTarget(id, TargetConfig.EMPTY, new RenderConfig(), new OffsetConfig());
    }

    /**
     * The targets a fresh installation starts with; they are written as individual files on first launch.
     */
    public static List<BindTarget> defaults() {
        return List.of(
                vanillaTarget("minecraft_head", false),
                vanillaTarget("skin_head", false),
                vanillaTarget("minecraft_head_2", true),
                vanillaTarget("skin_head_2", true));
    }

    public static BindTarget vanillaTarget(String path, boolean shouldBind) {
        String textureId = path.contains("skin") ? "minecraft:skins/" : "minecraft:textures/entity/player/";
        TargetConfig target = new TargetConfig(textureId, 0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f);
        OffsetConfig offsets = new OffsetConfig();
        offsets.x = -0.1f;
        offsets.bindX = offsets.bindZ = offsets.bindRotation = shouldBind;
        RenderConfig render = new RenderConfig();
        render.setDisableConfigs(List.of(
                new RenderConfig.DisableConfig("player_head", textureId, false, List.of(new UVRectangle(0, 0, 1.0f, 0.25f))),
                new RenderConfig.DisableConfig("dragon_head", "minecraft:textures/entity/enderdragon/dragon.png", true, List.of())));
        return new BindTarget(Identifier.fromNamespaceAndPath(RealCamera.MOD_ID, path), target, render, offsets);
    }
}
