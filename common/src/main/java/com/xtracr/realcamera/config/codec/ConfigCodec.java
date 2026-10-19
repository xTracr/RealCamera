package com.xtracr.realcamera.config.codec;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.binding.UVRectangle;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.config.binding.TargetConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public final class ConfigCodec {
    static final StreamCodec<ByteBuf, TargetConfig> TARGET_CONFIG_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TargetConfig::textureId,
            ByteBufCodecs.FLOAT, TargetConfig::forwardU,
            ByteBufCodecs.FLOAT, TargetConfig::forwardV,
            ByteBufCodecs.FLOAT, TargetConfig::upwardU,
            ByteBufCodecs.FLOAT, TargetConfig::upwardV,
            ByteBufCodecs.FLOAT, TargetConfig::posU,
            ByteBufCodecs.FLOAT, TargetConfig::posV,
            TargetConfig::new);
    static final StreamCodec<ByteBuf, OffsetConfig> OFFSET_CONFIG_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, offsets -> {
                byte bindFlags = 0;
                if (offsets.bindX) bindFlags |= 0x01;
                if (offsets.bindY) bindFlags |= 0x02;
                if (offsets.bindZ) bindFlags |= 0x04;
                if (offsets.bindRotation) bindFlags |= 0x08;
                return bindFlags;
            },
            ByteBufCodecs.FLOAT, offsets -> offsets.scale,
            ByteBufCodecs.FLOAT, offsets -> offsets.x,
            ByteBufCodecs.FLOAT, offsets -> offsets.y,
            ByteBufCodecs.FLOAT, offsets -> offsets.z,
            ByteBufCodecs.FLOAT, offsets -> offsets.pitch,
            ByteBufCodecs.FLOAT, offsets -> offsets.yaw,
            ByteBufCodecs.FLOAT, offsets -> offsets.roll,
            (flags, scale, x, y, z, pitch, yaw, roll) -> {
                OffsetConfig offsets = new OffsetConfig(scale, x, y, z, pitch, yaw, roll);
                offsets.bindX = (flags & 0x01) != 0;
                offsets.bindY = (flags & 0x02) != 0;
                offsets.bindZ = (flags & 0x04) != 0;
                offsets.bindRotation = (flags & 0x08) != 0;
                return offsets;
            });
    static final StreamCodec<ByteBuf, List<RenderConfig.DisableConfig>> DISABLE_CONFIGS = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RenderConfig.DisableConfig::name,
            ByteBufCodecs.STRING_UTF8, RenderConfig.DisableConfig::textureId,
            ByteBufCodecs.BOOL, RenderConfig.DisableConfig::disableAll,
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, UVRectangle::uMin,
                    ByteBufCodecs.FLOAT, UVRectangle::vMin,
                    ByteBufCodecs.FLOAT, UVRectangle::uMax,
                    ByteBufCodecs.FLOAT, UVRectangle::vMax,
                    UVRectangle::new).apply(ByteBufCodecs.list()), RenderConfig.DisableConfig::rectangles,
            RenderConfig.DisableConfig::new).apply(ByteBufCodecs.list());
    static final StreamCodec<ByteBuf, RenderConfig> RENDER_CONFIG_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, config -> config.disablingDepth,
            DISABLE_CONFIGS, RenderConfig::disableConfigs,
            RenderConfig::new);
    static final StreamCodec<ByteBuf, BindTarget> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(Identifier::parse, Identifier::toString), BindTarget::id,
            ByteBufCodecs.optional(TARGET_CONFIG_CODEC), target -> Optional.ofNullable(target.targetConfig()),
            OFFSET_CONFIG_CODEC, BindTarget::offsets,
            RENDER_CONFIG_CODEC, BindTarget::renderConfig,
            (id, target, offsets, render) -> new BindTarget(id, target.orElse(null), render, offsets));
}
