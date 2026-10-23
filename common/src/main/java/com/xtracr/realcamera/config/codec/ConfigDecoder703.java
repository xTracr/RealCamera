package com.xtracr.realcamera.config.codec;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.config.binding.TargetConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class ConfigDecoder703 {
    static BindTarget decode(ByteBuf buffer) {
        String name = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String textureId = ByteBufCodecs.STRING_UTF8.decode(buffer);
        int priority = ByteBufCodecs.VAR_INT.decode(buffer);
        float depth = buffer.readFloat();
        TargetConfig target = new TargetConfig(textureId, buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        OffsetConfig offsets = ConfigCodec.OFFSET_CONFIG_CODEC.decode(buffer);
        RenderConfig render = new RenderConfig(depth, ConfigCodec.DISABLE_CONFIGS.decode(buffer));
        var id = legacyId(name);
        return new BindTarget(id, id.getNamespace().equals("legacy") ? null : target, render, offsets, priority);
    }

    public static Identifier legacyId(String name) {
        if ("LEGACY_MODE".equals(name)) return Identifier.fromNamespaceAndPath("legacy", "head");
        Identifier id = Identifier.tryParse(RealCamera.MOD_ID + ":" + name);
        // Reserve the encoded prefix so a literal old name cannot collide with an encoded one.
        return id != null && !name.startsWith("imported_") ? id : Identifier.fromNamespaceAndPath(RealCamera.MOD_ID,
                "imported_" + HexFormat.of().formatHex(name.getBytes(StandardCharsets.UTF_8)));
    }
}
