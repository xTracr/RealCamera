package com.xtracr.realcamera.config.codec.utli;

import java.util.function.Function;

/**
 * A polyfill for composite constructors in {@code net.minecraft.network.codec.StreamCodec} for Minecraft 1.21.1.
 */
@SuppressWarnings({"unchecked", "unused"})
public final class CodecUtil {
    private CodecUtil() {}

    @FunctionalInterface public interface Constructor4<A, B, C, D, R> { R apply(A a, B b, C c, D d); }
    @FunctionalInterface public interface Constructor5<A, B, C, D, E, R> { R apply(A a, B b, C c, D d, E e); }
    @FunctionalInterface public interface Constructor6<A, B, C, D, E, F, R> { R apply(A a, B b, C c, D d, E e, F f); }
    @FunctionalInterface public interface Constructor7<A, B, C, D, E, F, G, R> { R apply(A a, B b, C c, D d, E e, F f, G g); }
    @FunctionalInterface public interface Constructor8<A, B, C, D, E, F, G, H, R> { R apply(A a, B b, C c, D d, E e, F f, G g, H h); }
    @FunctionalInterface public interface Constructor9<A, B, C, D, E, F, G, H, I, R> { R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i); }

    private static <B, T> StreamCodec<B, T> composite(final StreamCodec<B, ?>[] codecs, final Function<T, ?>[] getters, final Function<Object[], T> constructor) {
        return new StreamCodec<>() {
            @Override
            public T decode(B buf) {
                Object[] values = new Object[codecs.length];
                for (int i = 0; i < codecs.length; i++) {
                    values[i] = codecs[i].decode(buf);
                }
                return constructor.apply(values);
            }

            @Override
            public void encode(B buf, T value) {
                for (int i = 0; i < codecs.length; i++) {
                    ((StreamCodec<B, Object>) codecs[i]).encode(buf, ((Function<T, Object>) getters[i]).apply(value));
                }
            }
        };
    }

    public static <B, T, F1, F2, F3, F4> StreamCodec<B, T> composite(
            StreamCodec<B, F1> c1, Function<T, F1> g1, StreamCodec<B, F2> c2, Function<T, F2> g2,
            StreamCodec<B, F3> c3, Function<T, F3> g3, StreamCodec<B, F4> c4, Function<T, F4> g4,
            Constructor4<F1, F2, F3, F4, T> constructor) {
        return composite(new StreamCodec[]{c1, c2, c3, c4}, new Function[]{g1, g2, g3, g4},
                (vals) -> constructor.apply((F1) vals[0], (F2) vals[1], (F3) vals[2], (F4) vals[3]));
    }

    public static <B, T, F1, F2, F3, F4, F5> StreamCodec<B, T> composite(
            StreamCodec<B, F1> c1, Function<T, F1> g1, StreamCodec<B, F2> c2, Function<T, F2> g2,
            StreamCodec<B, F3> c3, Function<T, F3> g3, StreamCodec<B, F4> c4, Function<T, F4> g4,
            StreamCodec<B, F5> c5, Function<T, F5> g5, Constructor5<F1, F2, F3, F4, F5, T> constructor) {
        return composite(new StreamCodec[]{c1, c2, c3, c4, c5}, new Function[]{g1, g2, g3, g4, g5},
                (vals) -> constructor.apply((F1) vals[0], (F2) vals[1], (F3) vals[2], (F4) vals[3], (F5) vals[4]));
    }

    public static <B, T, F1, F2, F3, F4, F5, F6> StreamCodec<B, T> composite(
            StreamCodec<B, F1> c1, Function<T, F1> g1, StreamCodec<B, F2> c2, Function<T, F2> g2,
            StreamCodec<B, F3> c3, Function<T, F3> g3, StreamCodec<B, F4> c4, Function<T, F4> g4,
            StreamCodec<B, F5> c5, Function<T, F5> g5, StreamCodec<B, F6> c6, Function<T, F6> g6,
            Constructor6<F1, F2, F3, F4, F5, F6, T> constructor) {
        return composite(new StreamCodec[]{c1, c2, c3, c4, c5, c6}, new Function[]{g1, g2, g3, g4, g5, g6},
                (vals) -> constructor.apply((F1) vals[0], (F2) vals[1], (F3) vals[2], (F4) vals[3], (F5) vals[4], (F6) vals[5]));
    }

    public static <B, T, F1, F2, F3, F4, F5, F6, F7> StreamCodec<B, T> composite(
            StreamCodec<B, F1> c1, Function<T, F1> g1, StreamCodec<B, F2> c2, Function<T, F2> g2,
            StreamCodec<B, F3> c3, Function<T, F3> g3, StreamCodec<B, F4> c4, Function<T, F4> g4,
            StreamCodec<B, F5> c5, Function<T, F5> g5, StreamCodec<B, F6> c6, Function<T, F6> g6,
            StreamCodec<B, F7> c7, Function<T, F7> g7,
            Constructor7<F1, F2, F3, F4, F5, F6, F7, T> constructor) {
        return composite(new StreamCodec[]{c1, c2, c3, c4, c5, c6, c7}, new Function[]{g1, g2, g3, g4, g5, g6, g7},
                (vals) -> constructor.apply((F1) vals[0], (F2) vals[1], (F3) vals[2], (F4) vals[3], (F5) vals[4], (F6) vals[5], (F7) vals[6]));
    }

    public static <B, T, F1, F2, F3, F4, F5, F6, F7, F8> StreamCodec<B, T> composite(
            StreamCodec<B, F1> c1, Function<T, F1> g1, StreamCodec<B, F2> c2, Function<T, F2> g2,
            StreamCodec<B, F3> c3, Function<T, F3> g3, StreamCodec<B, F4> c4, Function<T, F4> g4,
            StreamCodec<B, F5> c5, Function<T, F5> g5, StreamCodec<B, F6> c6, Function<T, F6> g6,
            StreamCodec<B, F7> c7, Function<T, F7> g7, StreamCodec<B, F8> c8, Function<T, F8> g8,
            Constructor8<F1, F2, F3, F4, F5, F6, F7, F8, T> constructor) {
        return composite(new StreamCodec[]{c1, c2, c3, c4, c5, c6, c7, c8}, new Function[]{g1, g2, g3, g4, g5, g6, g7, g8},
                (vals) -> constructor.apply((F1) vals[0], (F2) vals[1], (F3) vals[2], (F4) vals[3], (F5) vals[4], (F6) vals[5], (F7) vals[6], (F8) vals[7]));
    }

    public static <B, T, F1, F2, F3, F4, F5, F6, F7, F8, F9> StreamCodec<B, T> composite(
            StreamCodec<B, F1> c1, Function<T, F1> g1, StreamCodec<B, F2> c2, Function<T, F2> g2,
            StreamCodec<B, F3> c3, Function<T, F3> g3, StreamCodec<B, F4> c4, Function<T, F4> g4,
            StreamCodec<B, F5> c5, Function<T, F5> g5, StreamCodec<B, F6> c6, Function<T, F6> g6,
            StreamCodec<B, F7> c7, Function<T, F7> g7, StreamCodec<B, F8> c8, Function<T, F8> g8,
            StreamCodec<B, F9> c9, Function<T, F9> g9, Constructor9<F1, F2, F3, F4, F5, F6, F7, F8, F9, T> constructor) {
        return composite(new StreamCodec[]{c1, c2, c3, c4, c5, c6, c7, c8, c9}, new Function[]{g1, g2, g3, g4, g5, g6, g7, g8, g9},
                (vals) -> constructor.apply((F1) vals[0], (F2) vals[1], (F3) vals[2], (F4) vals[3], (F5) vals[4], (F6) vals[5], (F7) vals[6], (F8) vals[7], (F9) vals[8]));
    }
}