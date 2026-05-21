package com.xtracr.realcamera.config;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public record BindTarget(
        String name, String textureId, int priority, float disablingDepth,
        TargetConfig targetConfig,
        BindConfig bindConfig,
        OffsetConfig offsets,
        DisableConfig[] disableConfigs) {
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
        return new BindTarget(name, textureId, 0, 0.2f, targetConfig, bindConfig, new OffsetConfig(), new DisableConfig[0]);
    }

    private static BindTarget vanillaTarget(String name, int priority, boolean shouldBind) {
        String textureId = name.contains("skin") ? "minecraft:skins/" : "minecraft:textures/entity/player/";
        TargetConfig targetConfig = new TargetConfig(0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f);
        BindConfig bindConfig = new BindConfig(shouldBind, true, shouldBind, shouldBind);
        OffsetConfig offsets = new OffsetConfig();
        offsets.x = -0.1f;
        DisableConfig playerHead = new DisableConfig("player_head", textureId, false, new UVRectangle[]{new UVRectangle(0, 0, 1.0f, 0.25f)});
        DisableConfig dragonHead = new DisableConfig("dragon_head", "minecraft:textures/entity/enderdragon/dragon.png", true, new UVRectangle[0]);
        DisableConfig[] disableConfigs = new DisableConfig[]{playerHead, dragonHead};
        return new BindTarget(name, textureId, priority, 0.2f, targetConfig, bindConfig, offsets, disableConfigs);
    }

    public boolean isEmpty() {
        return name == null || textureId == null || targetConfig == null || bindConfig == null || offsets == null || disableConfigs == null;
    }

    public DisableConfig[] filteredDisableConfigs(Predicate<DisableConfig> filter) {
        return Arrays.stream(disableConfigs).filter(filter).toArray(DisableConfig[]::new);
    }

    public void write(FriendlyByteBuf byteBuf) {
        byteBuf.writeUtf(name);
        byteBuf.writeUtf(textureId);
        byteBuf.writeVarInt(priority);
        byteBuf.writeFloat(disablingDepth);
        targetConfig.write(byteBuf);
        bindConfig.write(byteBuf);
        offsets.write(byteBuf);
        byteBuf.writeVarInt(disableConfigs.length);
        for (DisableConfig disableConfig : disableConfigs) {
            disableConfig.write(byteBuf);
        }
    }

    public record TargetConfig(float forwardU, float forwardV, float upwardU, float upwardV, float posU, float posV) {
        public void write(FriendlyByteBuf byteBuf) {
            byteBuf.writeFloat(forwardU);
            byteBuf.writeFloat(forwardV);
            byteBuf.writeFloat(upwardU);
            byteBuf.writeFloat(upwardV);
            byteBuf.writeFloat(posU);
            byteBuf.writeFloat(posV);
        }
    }

    public record BindConfig(boolean bindX, boolean bindY, boolean bindZ, boolean bindRotation) {
        public void write(FriendlyByteBuf byteBuf) {
            byte bindFlags = 0;
            if (bindX) bindFlags |= 0x01;
            if (bindY) bindFlags |= 0x02;
            if (bindZ) bindFlags |= 0x04;
            if (bindRotation) bindFlags |= 0x08;
            byteBuf.writeByte(bindFlags);
        }
    }
}
