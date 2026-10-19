package com.xtracr.realcamera.config.codec;

import com.mojang.serialization.DataResult;
import com.xtracr.realcamera.config.BindTarget;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceMap;
import net.minecraft.network.codec.StreamDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class CodecDispatcher {
    static final short CURRENT_VERSION = 709;
    static final Short2ReferenceMap<StreamDecoder<ByteBuf, BindTarget>> CODECS = Short2ReferenceMap.ofEntries(
            Short2ReferenceMap.entry((short) 703, ConfigDecoder703::decode),
            Short2ReferenceMap.entry(CURRENT_VERSION, ConfigCodec.CODEC)
    );

    public static BindTarget readWithVersion(ByteBuf buffer) {
        short version = buffer.readShort();
        StreamDecoder<ByteBuf, BindTarget> decoder = CODECS.get(version);
        if (decoder == null) throw new IllegalArgumentException("Incompatible version: " + version);
        return decoder.decode(buffer);
    }

    public static void writeWithVersion(ByteBuf buffer, BindTarget target) {
        buffer.writeShort(CURRENT_VERSION);
        ConfigCodec.CODEC.encode(buffer, target);
    }

    public static DataResult<BindTarget> fromCompressedBase64(String base64) {
        ByteBuf buffer = null;
        try {
            byte[] compressed = Base64.getDecoder().decode(base64);
            try (InflaterInputStream stream = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
                buffer = Unpooled.wrappedBuffer(stream.readAllBytes());
            }
            return DataResult.success(readWithVersion(buffer));
        } catch (Exception e) {
            return DataResult.error(() -> e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (buffer != null) buffer.release();
        }
    }

    public static DataResult<String> toCompressedBase64(BindTarget target) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            writeWithVersion(buffer, target);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), bytes);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            try (DeflaterOutputStream stream = new DeflaterOutputStream(output, deflater)) {
                stream.write(bytes);
            } finally {
                deflater.end();
            }
            return DataResult.success(Base64.getEncoder().encodeToString(output.toByteArray()));
        } catch (Exception e) {
            return DataResult.error(() -> e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            buffer.release();
        }
    }
}
