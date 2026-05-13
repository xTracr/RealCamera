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
    public static final List<BindTarget> defaultTargets;
    public static final BindTarget EMPTY = blank(null, null);
    private static final short serialVersion = 703; // 0.7.3

    static {
        defaultTargets = List.of(
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

    public static BindTarget read(FriendlyByteBuf byteBuf) throws IllegalArgumentException {
        short version = byteBuf.readShort();
        if (version != serialVersion)
            throw new IllegalArgumentException("Invalid version: " + version + ", expected " + serialVersion);
        String name = byteBuf.readUtf();
        String textureId = byteBuf.readUtf();
        int priority = byteBuf.readVarInt();
        float disablingDepth = byteBuf.readFloat();
        TargetConfig targetConfig = TargetConfig.read(byteBuf);
        BindConfig bindConfig = BindConfig.read(byteBuf);
        OffsetConfig offsets = OffsetConfig.read(byteBuf);
        DisableConfig[] disableConfigs = new DisableConfig[byteBuf.readVarInt()];
        for (int i = 0; i < disableConfigs.length; i++) {
            disableConfigs[i] = DisableConfig.read(byteBuf);
        }
        return new BindTarget(name, textureId, priority, disablingDepth, targetConfig, bindConfig, offsets, disableConfigs);
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
        byteBuf.writeShort(serialVersion);
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
        public static TargetConfig read(FriendlyByteBuf byteBuf) {
            return new TargetConfig(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
        }

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
        public static BindConfig read(FriendlyByteBuf byteBuf) {
            byte bindFlags = byteBuf.readByte();
            boolean bindX = (bindFlags & 0x01) != 0;
            boolean bindY = (bindFlags & 0x02) != 0;
            boolean bindZ = (bindFlags & 0x04) != 0;
            boolean bindRotation = (bindFlags & 0x08) != 0;
            return new BindConfig(bindX, bindY, bindZ, bindRotation);
        }

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
