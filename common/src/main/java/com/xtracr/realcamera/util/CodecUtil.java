package com.xtracr.realcamera.util;

import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class CodecUtil {
    public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> streamCodec,
                                                                                 final Function<C, T1> function,
                                                                                 final StreamCodec<? super B, T2> streamCodec2,
                                                                                 final Function<C, T2> function2,
                                                                                 final StreamCodec<? super B, T3> streamCodec3,
                                                                                 final Function<C, T3> function3,
                                                                                 final StreamCodec<? super B, T4> streamCodec4,
                                                                                 final Function<C, T4> function4,
                                                                                 final StreamCodec<? super B, T5> streamCodec5,
                                                                                 final Function<C, T5> function5,
                                                                                 final StreamCodec<? super B, T6> streamCodec6,
                                                                                 final Function<C, T6> function6,
                                                                                 final StreamCodec<? super B, T7> streamCodec7,
                                                                                 final Function<C, T7> function7,
                                                                                 final Function7<T1, T2, T3, T4, T5, T6, T7, C> function72) {
        return new StreamCodec<>() {
            public @NotNull C decode(B object) {
                T1 object2 = streamCodec.decode(object);
                T2 object3 = streamCodec2.decode(object);
                T3 object4 = streamCodec3.decode(object);
                T4 object5 = streamCodec4.decode(object);
                T5 object6 = streamCodec5.decode(object);
                T6 object7 = streamCodec6.decode(object);
                T7 object8 = streamCodec7.decode(object);
                return function72.apply(object2, object3, object4, object5, object6, object7, object8);
            }

            public void encode(B object, C object2) {
                streamCodec.encode(object, function.apply(object2));
                streamCodec2.encode(object, function2.apply(object2));
                streamCodec3.encode(object, function3.apply(object2));
                streamCodec4.encode(object, function4.apply(object2));
                streamCodec5.encode(object, function5.apply(object2));
                streamCodec6.encode(object, function6.apply(object2));
                streamCodec7.encode(object, function7.apply(object2));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> streamCodec,
                                                                                     final Function<C, T1> function,
                                                                                     final StreamCodec<? super B, T2> streamCodec2,
                                                                                     final Function<C, T2> function2,
                                                                                     final StreamCodec<? super B, T3> streamCodec3,
                                                                                     final Function<C, T3> function3,
                                                                                     final StreamCodec<? super B, T4> streamCodec4,
                                                                                     final Function<C, T4> function4,
                                                                                     final StreamCodec<? super B, T5> streamCodec5,
                                                                                     final Function<C, T5> function5,
                                                                                     final StreamCodec<? super B, T6> streamCodec6,
                                                                                     final Function<C, T6> function6,
                                                                                     final StreamCodec<? super B, T7> streamCodec7,
                                                                                     final Function<C, T7> function7,
                                                                                     final StreamCodec<? super B, T8> streamCodec8,
                                                                                     final Function<C, T8> function8,
                                                                                     final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> function82) {
        return new StreamCodec<>() {
            public @NotNull C decode(B object) {
                T1 object2 = streamCodec.decode(object);
                T2 object3 = streamCodec2.decode(object);
                T3 object4 = streamCodec3.decode(object);
                T4 object5 = streamCodec4.decode(object);
                T5 object6 = streamCodec5.decode(object);
                T6 object7 = streamCodec6.decode(object);
                T7 object8 = streamCodec7.decode(object);
                T8 object9 = streamCodec8.decode(object);
                return function82.apply(object2, object3, object4, object5, object6, object7, object8, object9);
            }

            public void encode(B object, C object2) {
                streamCodec.encode(object, function.apply(object2));
                streamCodec2.encode(object, function2.apply(object2));
                streamCodec3.encode(object, function3.apply(object2));
                streamCodec4.encode(object, function4.apply(object2));
                streamCodec5.encode(object, function5.apply(object2));
                streamCodec6.encode(object, function6.apply(object2));
                streamCodec7.encode(object, function7.apply(object2));
                streamCodec8.encode(object, function8.apply(object2));
            }
        };
    }
}
