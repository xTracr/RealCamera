package com.xtracr.realcamera.config.codec;

import com.mojang.serialization.DataResult;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.DisableConfig;
import com.xtracr.realcamera.config.OffsetConfig;
import com.xtracr.realcamera.config.UVRectangle;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class ConfigCodec {
    private static final short CURRENT_VERSION = 703;

    private ConfigCodec() {
    }

    public static BindTarget readWithVersion(FriendlyByteBuf byteBuf) throws IllegalArgumentException {
        short version = byteBuf.readShort();
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Invalid version: " + version + ", expected " + CURRENT_VERSION);
        return readTarget(byteBuf);
    }

    public static void writeWithVersion(FriendlyByteBuf byteBuf, BindTarget bindTarget) {
        byteBuf.writeShort(CURRENT_VERSION);
        writeTarget(byteBuf, bindTarget);
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
            byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(outputStream.toByteArray()));
            BindTarget target = readWithVersion(byteBuf);
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

    public static DataResult<String> toCompressedBase64(BindTarget bindTarget) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeWithVersion(byteBuf, bindTarget);
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), bytes);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(outputStream, new Deflater(Deflater.BEST_COMPRESSION))) {
                deflaterStream.write(bytes);
            }
            return DataResult.success(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
        } catch (Exception e) {
            return DataResult.error(() -> e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            byteBuf.release();
        }
    }

    private static BindTarget readTarget(FriendlyByteBuf byteBuf) {
        String name = byteBuf.readUtf();
        String textureId = byteBuf.readUtf();
        int priority = byteBuf.readVarInt();
        float disablingDepth = byteBuf.readFloat();
        BindTarget.TargetConfig targetConfig = readTargetConfig(byteBuf);
        BindTarget.BindConfig bindConfig = readBindConfig(byteBuf);
        OffsetConfig offsets = readOffsetConfig(byteBuf);
        int size = byteBuf.readVarInt();
        List<DisableConfig> disableConfigs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) disableConfigs.add(readDisableConfig(byteBuf));
        return new BindTarget(name, textureId, priority, disablingDepth, targetConfig, bindConfig, offsets, disableConfigs);
    }

    private static void writeTarget(FriendlyByteBuf byteBuf, BindTarget bindTarget) {
        byteBuf.writeUtf(bindTarget.name());
        byteBuf.writeUtf(bindTarget.textureId());
        byteBuf.writeVarInt(bindTarget.priority());
        byteBuf.writeFloat(bindTarget.disablingDepth());
        writeTargetConfig(byteBuf, bindTarget.targetConfig());
        writeBindConfig(byteBuf, bindTarget.bindConfig());
        writeOffsetConfig(byteBuf, bindTarget.offsets());
        byteBuf.writeVarInt(bindTarget.disableConfigs().size());
        for (DisableConfig disableConfig : bindTarget.disableConfigs()) writeDisableConfig(byteBuf, disableConfig);
    }

    private static BindTarget.TargetConfig readTargetConfig(FriendlyByteBuf byteBuf) {
        return new BindTarget.TargetConfig(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
    }

    private static void writeTargetConfig(FriendlyByteBuf byteBuf, BindTarget.TargetConfig config) {
        byteBuf.writeFloat(config.forwardU());
        byteBuf.writeFloat(config.forwardV());
        byteBuf.writeFloat(config.upwardU());
        byteBuf.writeFloat(config.upwardV());
        byteBuf.writeFloat(config.posU());
        byteBuf.writeFloat(config.posV());
    }

    private static BindTarget.BindConfig readBindConfig(FriendlyByteBuf byteBuf) {
        byte bindFlags = byteBuf.readByte();
        boolean bindX = (bindFlags & 0x01) != 0;
        boolean bindY = (bindFlags & 0x02) != 0;
        boolean bindZ = (bindFlags & 0x04) != 0;
        boolean bindRotation = (bindFlags & 0x08) != 0;
        return new BindTarget.BindConfig(bindX, bindY, bindZ, bindRotation);
    }

    private static void writeBindConfig(FriendlyByteBuf byteBuf, BindTarget.BindConfig bindConfig) {
        byte bindFlags = 0;
        if (bindConfig.bindX()) bindFlags |= 0x01;
        if (bindConfig.bindY()) bindFlags |= 0x02;
        if (bindConfig.bindZ()) bindFlags |= 0x04;
        if (bindConfig.bindRotation()) bindFlags |= 0x08;
        byteBuf.writeByte(bindFlags);
    }

    private static OffsetConfig readOffsetConfig(FriendlyByteBuf byteBuf) {
        return new OffsetConfig(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
    }

    private static void writeOffsetConfig(FriendlyByteBuf byteBuf, OffsetConfig offsets) {
        byteBuf.writeFloat(offsets.scale);
        byteBuf.writeFloat(offsets.x);
        byteBuf.writeFloat(offsets.y);
        byteBuf.writeFloat(offsets.z);
        byteBuf.writeFloat(offsets.pitch);
        byteBuf.writeFloat(offsets.yaw);
        byteBuf.writeFloat(offsets.roll);
    }

    private static DisableConfig readDisableConfig(FriendlyByteBuf byteBuf) {
        String name = byteBuf.readUtf();
        String textureId = byteBuf.readUtf();
        boolean disableAll = byteBuf.readBoolean();
        int size = byteBuf.readVarInt();
        List<UVRectangle> rectangles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) rectangles.add(readUVRectangle(byteBuf));
        return new DisableConfig(name, textureId, disableAll, rectangles);
    }

    private static void writeDisableConfig(FriendlyByteBuf byteBuf, DisableConfig disableConfig) {
        byteBuf.writeUtf(disableConfig.name());
        byteBuf.writeUtf(disableConfig.textureId());
        byteBuf.writeBoolean(disableConfig.disableAll());
        byteBuf.writeVarInt(disableConfig.rectangles().size());
        for (UVRectangle rect : disableConfig.rectangles()) writeUVRectangle(byteBuf, rect);
    }

    private static UVRectangle readUVRectangle(FriendlyByteBuf byteBuf) {
        return new UVRectangle(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
    }

    private static void writeUVRectangle(FriendlyByteBuf byteBuf, UVRectangle rect) {
        byteBuf.writeFloat(rect.uMin());
        byteBuf.writeFloat(rect.vMin());
        byteBuf.writeFloat(rect.uMax());
        byteBuf.writeFloat(rect.vMax());
    }
}
