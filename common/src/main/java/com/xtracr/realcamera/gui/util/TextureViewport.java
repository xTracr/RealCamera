package com.xtracr.realcamera.gui.util;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.phys.Vec2;
import org.jspecify.annotations.NonNull;

public record TextureViewport(@NonNull ScreenRectangle area, int textureScale, float textureX, float textureY) {
    private static final int DEFAULT_SCALE = 80;

    public Vec2 uvToXY(float u, float v) {
        int left = area.left(), width = area.width(), top = area.top(), height = area.height();
        float scale = (textureScale * width) / (float) DEFAULT_SCALE;
        float x = (u + textureX - 0.5f) * scale + left + width / 2.0f;
        float y = (v + textureY - 0.5f) * scale + top + height / 2.0f;
        return new Vec2(x, y);
    }

    public Vec2 xyToUV(float x, float y) {
        int left = area.left(), width = area.width(), top = area.top(), height = area.height();
        float invScale = (float) DEFAULT_SCALE / (textureScale * width);
        float u = (x - left - width / 2.0f) * invScale - textureX + 0.5f;
        float v = (y - top - height / 2.0f) * invScale - textureY + 0.5f;
        return new Vec2(u, v);
    }
}
