package com.xtracr.realcamera.config.serialization;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.DisableConfig;
import com.xtracr.realcamera.config.OffsetConfig;
import com.xtracr.realcamera.config.UVRectangle;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

final class ConfigSerializer703 implements ConfigSerializer {
    private static BindTarget.TargetConfig readTargetConfig(FriendlyByteBuf byteBuf) {
        float forwardU = byteBuf.readFloat();
        float forwardV = byteBuf.readFloat();
        float upwardU = byteBuf.readFloat();
        float upwardV = byteBuf.readFloat();
        float posU = byteBuf.readFloat();
        float posV = byteBuf.readFloat();
        return new BindTarget.TargetConfig(forwardU,  forwardV, upwardU, upwardV, posU, posV);
    }

    private static BindTarget.BindConfig readBindConfig(FriendlyByteBuf byteBuf) {
        byte bindFlags = byteBuf.readByte();
        boolean bindX = (bindFlags & 0x01) != 0;
        boolean bindY = (bindFlags & 0x02) != 0;
        boolean bindZ = (bindFlags & 0x04) != 0;
        boolean bindRotation = (bindFlags & 0x08) != 0;
        return new BindTarget.BindConfig(bindX, bindY, bindZ, bindRotation);
    }

    private static OffsetConfig readOffsetConfig(FriendlyByteBuf byteBuf) {
        float scale = byteBuf.readFloat();
        float x = byteBuf.readFloat();
        float y = byteBuf.readFloat();
        float z = byteBuf.readFloat();
        float pitch = byteBuf.readFloat();
        float yaw = byteBuf.readFloat();
        float roll = byteBuf.readFloat();
        return new OffsetConfig(scale, x, y, z, pitch, yaw, roll);
    }

    private static DisableConfig readDisableConfig(FriendlyByteBuf byteBuf) {
        String name = byteBuf.readUtf();
        String textureId = byteBuf.readUtf();
        boolean disableAll = byteBuf.readBoolean();
        UVRectangle[] rectangles = new UVRectangle[byteBuf.readVarInt()];
        for (int i = 0; i < rectangles.length; i++) rectangles[i] = readUVRectangle(byteBuf);
        return new DisableConfig(name, textureId, disableAll, rectangles);
    }

    private static UVRectangle readUVRectangle(FriendlyByteBuf byteBuf) {
        float uMin = byteBuf.readFloat();
        float vMin = byteBuf.readFloat();
        float uMax = byteBuf.readFloat();
        float vMax = byteBuf.readFloat();
        return new UVRectangle(uMin, vMin, uMax, vMax);
    }

    @Override
    public short version() {
        return 703; // 0.7.3
    }

    @Override
    public BindTarget readBindTarget(FriendlyByteBuf byteBuf) throws DecoderException {
        String name = byteBuf.readUtf();
        String textureId = byteBuf.readUtf();
        int priority = byteBuf.readVarInt();
        float disablingDepth = byteBuf.readFloat();
        BindTarget.TargetConfig targetConfig = readTargetConfig(byteBuf);
        BindTarget.BindConfig bindConfig = readBindConfig(byteBuf);
        OffsetConfig offsets = readOffsetConfig(byteBuf);
        DisableConfig[] disableConfigs = new DisableConfig[byteBuf.readVarInt()];
        for (int i = 0; i < disableConfigs.length; i++) {
            disableConfigs[i] = readDisableConfig(byteBuf);
        }
        return new BindTarget(name, textureId, priority, disablingDepth, targetConfig, bindConfig, offsets, disableConfigs);
    }
}
