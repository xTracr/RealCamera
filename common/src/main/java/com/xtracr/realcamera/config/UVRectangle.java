package com.xtracr.realcamera.config;

import net.minecraft.network.FriendlyByteBuf;

public record UVRectangle(float uMin, float vMin, float uMax, float vMax) {
    public static UVRectangle read(FriendlyByteBuf byteBuf) {
        return new UVRectangle(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
    }

    public boolean contains(float u, float v) {
        return u >= uMin && u <= uMax && v >= vMin && v <= vMax;
    }

    public void write(FriendlyByteBuf byteBuf) {
        byteBuf.writeFloat(uMin);
        byteBuf.writeFloat(vMin);
        byteBuf.writeFloat(uMax);
        byteBuf.writeFloat(vMax);
    }
}
