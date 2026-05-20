package com.xtracr.realcamera.config;

import com.mojang.serialization.DataResult;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public record BindTarget(
        String name, String textureId, int priority, float disablingDepth,
        TargetConfig targetConfig,
        BindConfig bindConfig,
        OffsetConfig offsets,
        DisableConfig[] disableConfigs) {
    public static final List<BindTarget> DEFAULT_TARGETS;
    public static final BindTarget EMPTY = blank(null, null);
    private static final short SERIAL_VERSION = 703; // 0.7.3

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

    public static BindTarget read(FriendlyByteBuf byteBuf) throws IllegalArgumentException {
        short version = byteBuf.readShort();
        if (version != SERIAL_VERSION)
            throw new IllegalArgumentException("Invalid version: " + toSemVer(version) + ", expected " + toSemVer(SERIAL_VERSION));
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

    public static DataResult<BindTarget> fromCompressedBase64(String base64) {
        FriendlyByteBuf byteBuf = null;
        try {
            byte[] compressed = Base64.getDecoder().decode(base64);
            InflaterInputStream inflaterStream = new InflaterInputStream(new ByteArrayInputStream(compressed));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inflaterStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            byte[] bytes = outputStream.toByteArray();
            byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            BindTarget target = BindTarget.read(byteBuf);
            if (target.isEmpty()) return DataResult.error(() -> "Invalid config format");
            return DataResult.success(target);
        } catch (Exception e) {
            return DataResult.error(() -> {
                String message = e.getClass().getSimpleName();
                if (e instanceof IllegalArgumentException) message += ": " + e.getMessage();
                return message;
            });
        } finally {
            if (byteBuf != null) byteBuf.release();
        }
    }

    private static String toSemVer(short version) {
        int major = version / 10000;
        int minor = version % 10000 / 100;
        int patch = version % 100;
        return major + "." + minor + "." + patch;
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
        byteBuf.writeShort(SERIAL_VERSION);
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

    public DataResult<String> toCompressedBase64() {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            write(byteBuf);
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), bytes);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(outputStream, new Deflater(Deflater.BEST_COMPRESSION))) {
                deflaterStream.write(bytes);
            }
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return DataResult.success(base64);
        } catch (Exception e) {
            return DataResult.error(() -> e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            byteBuf.release();
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
