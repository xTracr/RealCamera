package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.config.binding.TargetConfig;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Combines a path's data for editing and sharing; runtime solving uses TargetConfig only. */
public record BindTarget(Identifier id, @Nullable TargetConfig targetConfig, RenderConfig renderConfig, OffsetConfig offsets) {
    public static BindTarget blank(Identifier id) {
        TargetConfig target = id.getNamespace().equals(RealCamera.MOD_ID) ? new TargetConfig("", 0, 0, 0, 0, 0, 0) : null;
        return new BindTarget(id, target, new RenderConfig(), new OffsetConfig());
    }

    public static BindTarget vanillaTarget(String path, boolean shouldBind) {
        String textureId = path.contains("skin") ? "minecraft:skins/" : "minecraft:textures/entity/player/";
        TargetConfig target = new TargetConfig(textureId, 0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f);
        OffsetConfig offsets = new OffsetConfig();
        offsets.x = -0.1f;
        offsets.bindX = offsets.bindZ = offsets.bindRotation = shouldBind;
        RenderConfig render = new RenderConfig();
        render.disableConfigs.add(new RenderConfig.DisableConfig("player_head", textureId, false,
                java.util.List.of(new UVRectangle(0, 0, 1.0f, 0.25f))));
        render.disableConfigs.add(new RenderConfig.DisableConfig("dragon_head", "minecraft:textures/entity/enderdragon/dragon.png", true, java.util.List.of()));
        return new BindTarget(Identifier.fromNamespaceAndPath(RealCamera.MOD_ID, path), target, render, offsets);
    }
}
