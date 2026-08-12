package com.xtracr.realcamera.config.codec.utli;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A polyfill for {@code net.minecraft.network.codec.StreamCodec} in Minecraft 1.21.1.
 */
public interface StreamCodec<B, V> {

    V decode(B buf);

    void encode(B buf, V value);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.accept(buf, value);
            }
        };
    }

    static <B, T, F> StreamCodec<B, T> of(StreamCodec<B, F> fieldCodec, Function<T, F> toField, Function<F, T> fromField) {
        return new StreamCodec<>() {
            @Override
            public T decode(B buf) {
                F fieldValue = fieldCodec.decode(buf);
                return fromField.apply(fieldValue);
            }

            @Override
            public void encode(B buf, T value) {
                F fieldValue = toField.apply(value);
                fieldCodec.encode(buf, fieldValue);
            }
        };
    }

    static <B, V> StreamCodec<B, List<V>> list(StreamCodec<B, V> elementCodec) {
        return new StreamCodec<>() {
            @Override
            public List<V> decode(B buf) {
                if (!(buf instanceof ByteBuf byteBuf))
                    throw new UnsupportedOperationException("List codec only supports ByteBuf");
                int size = ByteBufCodecs.readVarInt(byteBuf);
                List<V> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(elementCodec.decode(buf));
                }
                return list;
            }

            @Override
            public void encode(B buf, List<V> value) {
                if (!(buf instanceof ByteBuf byteBuf))
                    throw new UnsupportedOperationException("List codec only supports ByteBuf");
                ByteBufCodecs.writeVarInt(byteBuf, value.size());
                for (V element : value) {
                    elementCodec.encode(buf, element);
                }
            }
        };
    }
}