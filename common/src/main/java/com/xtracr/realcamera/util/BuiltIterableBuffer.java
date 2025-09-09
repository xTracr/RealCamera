package com.xtracr.realcamera.util;


import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record BuiltIterableBuffer(RenderType renderType, String textureId, IterableVertexBuffer vertexBuffer) {
    private static final Pattern textureIdPattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
    private static final Map<RenderType, Map<VertexData.UV, float[]>> FIND_PRIMITIVE_CACHE = new HashMap<>();

    public static BuiltIterableBuffer buildFrom(RenderType renderType, MeshData meshData) {
        String renderTypeName = renderType.toString();
        Matcher matcher = textureIdPattern.matcher(renderTypeName);
        String textureId = matcher.find() ? matcher.group(1) : renderTypeName;
        return new BuiltIterableBuffer(renderType, textureId, new IterableVertexBuffer(meshData));
    }

    public VertexData[] @Nullable [] findPrimitivesInCache(VertexData.UV[] uvs) {
        Map<VertexData.UV, float[]> cache = FIND_PRIMITIVE_CACHE.get(renderType);
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
        vertexBuffer.primitiveStream().filter(primitive -> {
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
        }).findAny();
        return primitives;
    }

    public VertexData[] @Nullable [] findPrimitives(VertexData.UV[] uvs) {
        final int resolution = 1000000;
        int length = renderType.mode().primitiveLength, uvsLength = uvs.length;
        int[] us = new int[length], vs = new int[length];
        VertexData[][] primitives = new VertexData[uvsLength][];
        vertexBuffer.primitiveStream().filter(primitive -> {
            for (int i = 0; i < length; i++) {
                us[i] = (int) (resolution * primitive[i].u());
                vs[i] = (int) (resolution * primitive[i].v());
            }
            Polygon polygon = new Polygon(us, vs, length);
            boolean allFound = true;
            for (int i = 0; i < uvsLength; i++){
                if (primitives[i] != null) continue;
                VertexData.UV uv = uvs[i];
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
                FIND_PRIMITIVE_CACHE.computeIfAbsent(renderType, k -> new HashMap<>()).put(uv, uvCache);
                primitives[i] = VertexData.asImmutable(primitive);
            }
            return allFound;
        }).findAny();
        return primitives;
    }
}