package com.xtracr.realcamera.config.codec.utli;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * A polyfill for {@code net.minecraft.network.codec.ByteBufCodecs} in Minecraft 1.21.1.
 */
public final class ByteBufCodecs {
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((buf, val) -> buf.writeByte(val), ByteBuf::readByte);
    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(ByteBufCodecs::writeVarInt, ByteBufCodecs::readVarInt);
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(ByteBufCodecs::writeString, ByteBufCodecs::readString);

    public static int readVarInt(ByteBuf buf) {
        int i = 0;
        int j = 0;
        byte b;
        do {
            b = buf.readByte();
            i |= (b & 0x7F) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((b & 0x80) == 0x80);
        return i;
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        int i = value;
        while ((i & -128) != 0) {
            buf.writeByte(i & 127 | 128);
            i >>>= 7;
        }
        buf.writeByte(i);
    }

    public static String readString(ByteBuf buf) {
        int length = readVarInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }
}