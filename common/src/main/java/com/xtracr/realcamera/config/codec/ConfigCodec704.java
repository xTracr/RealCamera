package com.xtracr.realcamera.config.codec;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.BindConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

final class ConfigCodec704 {
    static final StreamCodec<ByteBuf, BindConfig> BIND_CONFIG_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            bindConfig -> {
                byte bindFlags = 0;
                if (bindConfig.bindX()) bindFlags |= 0x01;
                if (bindConfig.bindY()) bindFlags |= 0x02;
                if (bindConfig.bindZ()) bindFlags |= 0x04;
                if (bindConfig.bindPitch()) bindFlags |= 0x08;
                if (bindConfig.bindYaw()) bindFlags |= 0x10;
                if (bindConfig.bindRoll()) bindFlags |= 0x20;
                return bindFlags;
            },
            bindFlags -> new BindConfig(
                    (bindFlags & 0x01) != 0,
                    (bindFlags & 0x02) != 0,
                    (bindFlags & 0x04) != 0,
                    (bindFlags & 0x08) != 0,
                    (bindFlags & 0x10) != 0,
                    (bindFlags & 0x20) != 0
            )
    );

    static final StreamCodec<ByteBuf, BindTarget> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BindTarget::name,
            ByteBufCodecs.STRING_UTF8, BindTarget::textureId,
            ByteBufCodecs.VAR_INT, BindTarget::priority,
            ByteBufCodecs.FLOAT, BindTarget::disablingDepth,
            ConfigCodec703.TARGET_CONFIG_CODEC, BindTarget::targetConfig,
            BIND_CONFIG_CODEC, BindTarget::bindConfig,
            ConfigCodec703.OFFSET_CONFIG_CODEC, BindTarget::offsets,
            ConfigCodec703.DISABLE_CONFIGS_CODEC, BindTarget::disableConfigs,
            BindTarget::new
    );
}
