package com.xtracr.realcamera.config.codec;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.CameraPosture;
import com.xtracr.realcamera.config.OffsetConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigCodec703Test {
    @Test
    void legacyRotationIsCopiedToEveryPosture() {
        ByteBuf byteBuf = Unpooled.buffer();
        writeLegacyBindTarget(byteBuf, 12.0f, -34.0f, 56.0f);

        BindTarget target = new ConfigCodec703().decode(byteBuf);

        for (CameraPosture posture : CameraPosture.values()) {
            OffsetConfig offsets = target.offsets();
            assertEquals(12.0f, offsets.pitch(posture), 0.0001f);
            assertEquals(-34.0f, offsets.yaw(posture), 0.0001f);
            assertEquals(56.0f, offsets.roll(posture), 0.0001f);
        }
    }

    private static void writeLegacyBindTarget(ByteBuf byteBuf, float pitch, float yaw, float roll) {
        ByteBufCodecs.STRING_UTF8.encode(byteBuf, "legacy");
        ByteBufCodecs.STRING_UTF8.encode(byteBuf, "minecraft:textures/entity/player/");
        ByteBufCodecs.VAR_INT.encode(byteBuf, 0);
        byteBuf.writeFloat(0.2f);
        for (int i = 0; i < 6; i++) byteBuf.writeFloat(0.0f);
        byteBuf.writeByte(0);
        byteBuf.writeFloat(1.0f);
        byteBuf.writeFloat(0.1f);
        byteBuf.writeFloat(0.2f);
        byteBuf.writeFloat(0.3f);
        byteBuf.writeFloat(pitch);
        byteBuf.writeFloat(yaw);
        byteBuf.writeFloat(roll);
        ByteBufCodecs.VAR_INT.encode(byteBuf, 0);
    }
}
