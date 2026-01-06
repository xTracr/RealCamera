package com.xtracr.realcamera.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.BindTarget.DisableConfig;
import com.xtracr.realcamera.config.BindTarget.UVRectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DisableMaskCache {
    private static final int MASK_SIZE = 2048;
    private static final int MASK_COLOR = 0xFFFFFFFF;
    private static final Map<CacheKey, ResourceLocation> CACHE = new HashMap<>();
    private static ResourceLocation defaultMask;

    private DisableMaskCache() {
    }

    public static ResourceLocation getMask(String textureId, DisableConfig[] configs) {
        if (configs.length == 0) return getDefaultMask();
        CacheKey key = new CacheKey(textureId, hashConfigs(configs));
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) return cached;
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "mask/" + Integer.toHexString(key.hashCode()));
        DynamicTexture texture = new DynamicTexture(buildMask(configs));
        Minecraft.getInstance().getTextureManager().register(location, texture);
        CACHE.put(key, location);
        return location;
    }

    private static ResourceLocation getDefaultMask() {
        if (defaultMask != null) return defaultMask;
        defaultMask = ResourceLocation.fromNamespaceAndPath(RealCamera.MODID, "mask/default");
        DynamicTexture texture = new DynamicTexture(new NativeImage(1, 1, false));
        Minecraft.getInstance().getTextureManager().register(defaultMask, texture);
        return defaultMask;
    }

    private static NativeImage buildMask(DisableConfig[] configs) {
        NativeImage image = new NativeImage(MASK_SIZE, MASK_SIZE, false);
        for (DisableConfig config : configs) {
            for (UVRectangle rect : config.rectangles()) {
                fillRect(image, rect);
            }
        }
        return image;
    }

    private static void fillRect(NativeImage image, UVRectangle rect) {
        float uMin = clamp01(Math.min(rect.uMin(), rect.uMax()));
        float uMax = clamp01(Math.max(rect.uMin(), rect.uMax()));
        float vMin = clamp01(Math.min(rect.vMin(), rect.vMax()));
        float vMax = clamp01(Math.max(rect.vMin(), rect.vMax()));
        int x0 = clamp((int) Math.floor(uMin * (MASK_SIZE - 1)), 0, MASK_SIZE - 1);
        int x1 = clamp((int) Math.ceil(uMax * (MASK_SIZE - 1)), 0, MASK_SIZE - 1);
        int y0 = clamp((int) Math.floor(vMin * (MASK_SIZE - 1)), 0, MASK_SIZE - 1);
        int y1 = clamp((int) Math.ceil(vMax * (MASK_SIZE - 1)), 0, MASK_SIZE - 1);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                image.setPixelRGBA(x, y, MASK_COLOR);
            }
        }
    }

    private static int hashConfigs(DisableConfig[] configs) {
        int result = 1;
        for (DisableConfig config : configs) {
            result = 31 * result + hashConfig(config);
        }
        return result;
    }

    private static int hashConfig(DisableConfig config) {
        int result = Objects.hash(config.name(), config.textureId(), config.disableAll());
        for (UVRectangle rect : config.rectangles()) {
            result = 31 * result + hashRect(rect);
        }
        return result;
    }

    private static int hashRect(UVRectangle rect) {
        int result = Float.hashCode(rect.uMin());
        result = 31 * result + Float.hashCode(rect.vMin());
        result = 31 * result + Float.hashCode(rect.uMax());
        result = 31 * result + Float.hashCode(rect.vMax());
        return result;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private record CacheKey(String textureId, int configHash) {
    }
}
