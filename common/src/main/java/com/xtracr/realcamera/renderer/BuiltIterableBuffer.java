package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.MeshData;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.renderer.state.VertexData.UV;
import com.xtracr.realcamera.util.RenderTypeUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.Map;

public record BuiltIterableBuffer(RenderType renderType, String textureId, IterableVertexBuffer vertexBuffer) {
    public static BuiltIterableBuffer buildFrom(RenderType renderType, MeshData meshData) {
        return new BuiltIterableBuffer(renderType, RenderTypeUtil.getTextureId(renderType), new IterableVertexBuffer(meshData));
    }

    public boolean anyNotCached(UV[] uvs) {
        Map<UV, float[]> cache = RenderTypeUtil.getPrimitiveCache(renderType);
        if (cache == null) return true;
        for (UV uv : uvs) {
            if (!cache.containsKey(uv)) return true;
        }
        return false;
    }

    public VertexData[] @Nullable [] findPrimitivesInCache(UV[] uvs) {
        Map<UV, float[]> cache = RenderTypeUtil.getPrimitiveCache(renderType);
        int length = renderType.mode().primitiveLength, uvsLength = uvs.length;
        VertexData[][] primitives = new VertexData[uvsLength][];
        if (cache == null) return primitives;
        float[][] uvCacheArray = new float[uvsLength][];
        boolean allNull = true;
        for (int i = 0; i < uvsLength; i++) {
            uvCacheArray[i] = cache.get(uvs[i]);
            if (uvCacheArray[i] != null) allNull = false;
        }
        if (allNull) return primitives;
        vertexBuffer.primitiveStream().anyMatch(primitive -> {
            float[] uvCache;
            boolean allFound = true;
            cacheFor:
            for (int i = 0; i < uvsLength; i++) {
                if (primitives[i] != null) continue;
                uvCache = uvCacheArray[i];
                if (uvCache == null) continue;
                for (int j = 0; j < length; j++) {
                    if (uvCache[j * 2] != primitive[j].u() || uvCache[j * 2 + 1] != primitive[j].v()) {
                        allFound = false;
                        continue cacheFor;
                    }
                }
                primitives[i] = VertexData.asImmutable(primitive);
            }
            return allFound;
        });
        return primitives;
    }

    public VertexData[] @Nullable [] findPrimitives(UV[] uvs) {
        final int resolution = 1000000;
        int length = renderType.mode().primitiveLength, uvsLength = uvs.length;
        int[] us = new int[length], vs = new int[length];
        VertexData[][] primitives = new VertexData[uvsLength][];
        vertexBuffer.primitiveStream().anyMatch(primitive -> {
            for (int i = 0; i < length; i++) {
                us[i] = (int) (resolution * primitive[i].u());
                vs[i] = (int) (resolution * primitive[i].v());
            }
            Polygon polygon = new Polygon(us, vs, length);
            boolean allFound = true;
            for (int i = 0; i < uvsLength; i++) {
                if (primitives[i] != null) continue;
                UV uv = uvs[i];
                if (uv == null) continue;
                if (!polygon.contains(resolution * uv.u(), resolution * uv.v())) {
                    allFound = false;
                    continue;
                }
                float[] uvCache = new float[length * 2];
                for (int j = 0; j < length; j++) {
                    uvCache[j * 2] = primitive[j].u();
                    uvCache[j * 2 + 1] = primitive[j].v();
                }
                RenderTypeUtil.cachePrimitive(renderType, uv, uvCache);
                primitives[i] = VertexData.asImmutable(primitive);
            }
            return allFound;
        });
        return primitives;
    }
}
