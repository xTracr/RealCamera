package com.xtracr.realcamera.config.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.DataResult;
import com.xtracr.realcamera.config.BindTarget;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.codec.StreamCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public interface ConfigCodec extends StreamCodec<ByteBuf, BindTarget> {
    ConfigCodec SERIALIZER_703 = new ConfigCodec703();
    ConfigCodec SERIALIZER_708 = new ConfigCodec708();
    Map<Short, ConfigCodec> SERIALIZERS = ImmutableMap.of(
            SERIALIZER_703.version(), SERIALIZER_703,
            SERIALIZER_708.version(), SERIALIZER_708
    );

    static BindTarget readWithVersion(ByteBuf byteBuf) throws DecoderException, IllegalArgumentException {
        short version = byteBuf.readShort();
        ConfigCodec serializer = SERIALIZERS.get(version);
        if (serializer  == null) throw new IllegalArgumentException("Incompatible version: " + toSemVer(version));
        return serializer.decode(byteBuf);
    }

    static void writeWithVersion(ByteBuf byteBuf, BindTarget bindTarget) throws EncoderException {
        byteBuf.writeShort(SERIALIZER_708.version());
        SERIALIZER_708.encode(byteBuf, bindTarget);
    }

    static DataResult<BindTarget> fromCompressedBase64(String base64) {
        ByteBuf byteBuf = null;
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
            byteBuf = Unpooled.wrappedBuffer(bytes);
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

    static DataResult<String> toCompressedBase64(BindTarget bindTarget) {
        ByteBuf byteBuf = Unpooled.buffer();
        try {
            writeWithVersion(byteBuf, bindTarget);
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

    private static String toSemVer(short version) {
        int major = version / 10000;
        int minor = version % 10000 / 100;
        int patch = version % 100;
        return major + "." + minor + "." + patch;
    }

    short version();
}
