package com.xtracr.realcamera.config.codec;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.OffsetConfig;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

final class ConfigSerializer708 {
    static final StreamCodec<ByteBuf, OffsetConfig> OFFSET_CONFIG_CODEC = new StreamCodec<>() {
        @Override
        public @NonNull OffsetConfig decode(@NonNull ByteBuf byteBuf) {
            return new OffsetConfig(
                    byteBuf.readFloat(),
                    byteBuf.readFloat(),
                    byteBuf.readFloat(),
                    byteBuf.readFloat(),
                    byteBuf.readFloat(),
                    byteBuf.readFloat(),
                    byteBuf.readFloat(),
                    byteBuf.readFloat(),
                    byteBuf.readFloat());
        }

        @Override
        public void encode(@NonNull ByteBuf byteBuf, @NonNull OffsetConfig offsets) {
            byteBuf.writeFloat(offsets.scale);
            byteBuf.writeFloat(offsets.x);
            byteBuf.writeFloat(offsets.y);
            byteBuf.writeFloat(offsets.z);
            byteBuf.writeFloat(offsets.pitch);
            byteBuf.writeFloat(offsets.yaw);
            byteBuf.writeFloat(offsets.roll);
            byteBuf.writeFloat(offsets.swimmingPitchAdjustment);
            byteBuf.writeFloat(offsets.crawlingPitchAdjustment);
        }
    };
    static final StreamCodec<ByteBuf, BindTarget> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BindTarget::name,
            ByteBufCodecs.STRING_UTF8, BindTarget::textureId,
            ByteBufCodecs.VAR_INT, BindTarget::priority,
            ByteBufCodecs.FLOAT, BindTarget::disablingDepth,
            ConfigCodec703.TARGET_CONFIG_CODEC, BindTarget::targetConfig,
            ConfigCodec703.BIND_CONFIG_CODEC, BindTarget::bindConfig,
            OFFSET_CONFIG_CODEC, BindTarget::offsets,
            ConfigCodec703.DISABLE_CONFIGS_CODEC, BindTarget::disableConfigs,
            BindTarget::new
    );
}
