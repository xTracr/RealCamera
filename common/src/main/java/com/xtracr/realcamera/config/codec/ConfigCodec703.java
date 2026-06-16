package com.xtracr.realcamera.config.codec;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.BindConfig;
import com.xtracr.realcamera.config.BindTarget.TargetConfig;
import com.xtracr.realcamera.config.DisableConfig;
import com.xtracr.realcamera.config.OffsetConfig;
import com.xtracr.realcamera.config.UVRectangle;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

import java.util.List;

final class ConfigCodec703 implements ConfigCodec {
    static final StreamCodec<ByteBuf, TargetConfig> TARGET_CONFIG_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TargetConfig::forwardU,
            ByteBufCodecs.FLOAT, TargetConfig::forwardV,
            ByteBufCodecs.FLOAT, TargetConfig::upwardU,
            ByteBufCodecs.FLOAT, TargetConfig::upwardV,
            ByteBufCodecs.FLOAT, TargetConfig::posU,
            ByteBufCodecs.FLOAT, TargetConfig::posV,
            TargetConfig::new
    );
    static final StreamCodec<ByteBuf, BindConfig> BIND_CONFIG_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            bindConfig -> {
                byte bindFlags = 0;
                if (bindConfig.bindX()) bindFlags |= 0x01;
                if (bindConfig.bindY()) bindFlags |= 0x02;
                if (bindConfig.bindZ()) bindFlags |= 0x04;
                if (bindConfig.bindRotation()) bindFlags |= 0x08;
                return bindFlags;
            },
            bindFlags -> {
                boolean bindX = (bindFlags & 0x01) != 0;
                boolean bindY = (bindFlags & 0x02) != 0;
                boolean bindZ = (bindFlags & 0x04) != 0;
                boolean bindRotation = (bindFlags & 0x08) != 0;
                return new BindConfig(bindX, bindY, bindZ, bindRotation);
            }
    );
    static final StreamCodec<ByteBuf, OffsetConfig> OFFSET_CONFIG_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, offsets -> offsets.scale,
            ByteBufCodecs.FLOAT, offsets -> offsets.x,
            ByteBufCodecs.FLOAT, offsets -> offsets.y,
            ByteBufCodecs.FLOAT, offsets -> offsets.z,
            ByteBufCodecs.FLOAT, offsets -> offsets.pitch,
            ByteBufCodecs.FLOAT, offsets -> offsets.yaw,
            ByteBufCodecs.FLOAT, offsets -> offsets.roll,
            OffsetConfig::new
    );
    static final StreamCodec<ByteBuf, List<UVRectangle>> UV_RECTANGLES_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, UVRectangle::uMin,
            ByteBufCodecs.FLOAT, UVRectangle::vMin,
            ByteBufCodecs.FLOAT, UVRectangle::uMax,
            ByteBufCodecs.FLOAT, UVRectangle::vMax,
            UVRectangle::new
    ).apply(ByteBufCodecs.list());
    static final StreamCodec<ByteBuf, List<DisableConfig>> DISABLE_CONFIGS_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DisableConfig::name,
            ByteBufCodecs.STRING_UTF8, DisableConfig::textureId,
            ByteBufCodecs.BOOL, DisableConfig::disableAll,
            UV_RECTANGLES_CODEC, DisableConfig::rectangles,
            DisableConfig::new
    ).apply(ByteBufCodecs.list());
    static final StreamCodec<ByteBuf, BindTarget> BIND_TARGET_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BindTarget::name,
            ByteBufCodecs.STRING_UTF8, BindTarget::textureId,
            ByteBufCodecs.VAR_INT, BindTarget::priority,
            ByteBufCodecs.FLOAT, BindTarget::disablingDepth,
            TARGET_CONFIG_CODEC, BindTarget::targetConfig,
            BIND_CONFIG_CODEC, BindTarget::bindConfig,
            OFFSET_CONFIG_CODEC, BindTarget::offsets,
            DISABLE_CONFIGS_CODEC, BindTarget::disableConfigs,
            BindTarget::new
    );

    @Override
    public short version() {
        return 703; // 0.7.3
    }

    @Override
    public @NonNull BindTarget decode(@NonNull ByteBuf byteBuf) throws DecoderException {
        return BIND_TARGET_CODEC.decode(byteBuf);
    }

    @Override
    public void encode(@NonNull ByteBuf byteBuf, @NonNull BindTarget bindTarget) {
        BIND_TARGET_CODEC.encode(byteBuf, bindTarget);
    }
}
