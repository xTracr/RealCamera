package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.MeshData;
import com.xtracr.realcamera.util.VertexData.UV;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record BuiltIterableBuffer(RenderType renderType, String textureId, IterableVertexBuffer vertexBuffer) {
    private static final Pattern TEXTURE_ID_PATTERN = Pattern.compile("texture\\[Optional\\[(.*?)]");
    private static final Map<RenderType, String> TEXTURE_ID_CACHE = new HashMap<>();
    private static final Map<RenderType, Map<UV, PrimitiveCache>> FIND_PRIMITIVE_CACHE = new HashMap<>();

    private record PrimitiveCache(int index, float[] uvCache) { }

    public static BuiltIterableBuffer buildFrom(RenderType renderType, MeshData meshData) {
        String textureId = TEXTURE_ID_CACHE.computeIfAbsent(renderType, rt -> {
            String renderTypeName = rt.toString();
            Matcher matcher = TEXTURE_ID_PATTERN.matcher(renderTypeName);
            return matcher.find() ? matcher.group(1) : renderTypeName;
        });
        return new BuiltIterableBuffer(renderType, textureId, new IterableVertexBuffer(meshData));
    }

    public boolean anyNotCached(UV[] uvs) {
        Map<UV, PrimitiveCache> cache = FIND_PRIMITIVE_CACHE.get(renderType);
        if (cache == null) return true;
        for (UV uv : uvs) {
            if (uv != null && !cache.containsKey(uv)) return true;
        }
        return false;
    }

    public VertexData[] @Nullable [] findPrimitivesInCache(UV[] uvs) {
        VertexData[][] primitives = new VertexData[uvs.length][];
        findPrimitivesInCache(uvs, primitives);
        return primitives;
    }

    public int findPrimitivesInCache(UV[] uvs, VertexData[] @Nullable [] primitives) {
        Map<UV, PrimitiveCache> cache = FIND_PRIMITIVE_CACHE.get(renderType);
        int unresolved = 0;
        if (cache == null) {
            for (UV uv : uvs) {
                if (uv != null) unresolved++;
            }
            return unresolved;
        }
        int primitiveCount = vertexBuffer.primitiveCount();
        int cachedIndex0 = -1, cachedIndex1 = -1, cachedIndex2 = -1;
        VertexData[] primitive0 = null, primitive1 = null, primitive2 = null;
        for (int i = 0; i < uvs.length; i++) {
            UV uv = uvs[i];
            if (uv == null) continue;
            PrimitiveCache cachedPrimitive = cache.get(uv);
            if (cachedPrimitive == null) {
                unresolved++;
                continue;
            }
            int primitiveIndex = cachedPrimitive.index();
            if (primitiveIndex >= primitiveCount) {
                cache.remove(uv);
                unresolved++;
                continue;
            }
            if (!vertexBuffer.primitiveMatchesUVs(primitiveIndex, cachedPrimitive.uvCache())) {
                cache.remove(uv);
                unresolved++;
                continue;
            }
            VertexData[] primitive;
            if (primitiveIndex == cachedIndex0) primitive = primitive0;
            else if (primitiveIndex == cachedIndex1) primitive = primitive1;
            else if (primitiveIndex == cachedIndex2) primitive = primitive2;
            else {
                primitive = vertexBuffer.readPrimitiveAt(primitiveIndex);
                if (cachedIndex0 == -1) {
                    cachedIndex0 = primitiveIndex;
                    primitive0 = primitive;
                } else if (cachedIndex1 == -1) {
                    cachedIndex1 = primitiveIndex;
                    primitive1 = primitive;
                } else {
                    cachedIndex2 = primitiveIndex;
                    primitive2 = primitive;
                }
            }
            primitives[i] = primitive;
        }
        return unresolved;
    }

    public VertexData[] @Nullable [] findPrimitives(UV[] uvs) {
        UV[] unresolvedUvs = uvs.clone();
        VertexData[][] primitives = new VertexData[unresolvedUvs.length][];
        int unresolved = 0;
        for (UV uv : unresolvedUvs) {
            if (uv != null) unresolved++;
        }
        findPrimitives(unresolvedUvs, primitives, unresolved);
        return primitives;
    }

    public int findPrimitives(UV[] uvs, VertexData[] @Nullable [] primitives, int unresolved) {
        int length = renderType.mode().primitiveLength;
        float[] primitiveUvs = new float[length * 2];
        Map<UV, PrimitiveCache> cache = FIND_PRIMITIVE_CACHE.computeIfAbsent(renderType, k -> new HashMap<>());
        for (int primitiveIndex = 0, primitiveCount = vertexBuffer.primitiveCount(); primitiveIndex < primitiveCount && unresolved > 0; primitiveIndex++) {
            vertexBuffer.readPrimitiveUVs(primitiveIndex, primitiveUvs);
            float minU = primitiveUvs[0], maxU = primitiveUvs[0], minV = primitiveUvs[1], maxV = primitiveUvs[1];
            for (int i = 1; i < length; i++) {
                float primitiveU = primitiveUvs[i * 2], primitiveV = primitiveUvs[i * 2 + 1];
                minU = Math.min(minU, primitiveU);
                maxU = Math.max(maxU, primitiveU);
                minV = Math.min(minV, primitiveV);
                maxV = Math.max(maxV, primitiveV);
            }
            VertexData[] primitive = null;
            float[] uvCache = null;
            for (int i = 0; i < uvs.length; i++) {
                UV uv = uvs[i];
                if (uv == null) continue;
                double u = uv.u(), v = uv.v();
                if (u < minU || u > maxU || v < minV || v > maxV || !primitiveContains(primitiveUvs, length, u, v)) {
                    continue;
                }
                if (primitive == null) {
                    primitive = vertexBuffer.readPrimitiveAt(primitiveIndex);
                }
                if (uvCache == null) {
                    uvCache = primitiveUvs.clone();
                }
                cache.put(uv, new PrimitiveCache(primitiveIndex, uvCache));
                primitives[i] = primitive;
                uvs[i] = null;
                unresolved--;
            }
        }
        return unresolved;
    }

    private static boolean primitiveContains(float[] primitiveUvs, int length, double u, double v) {
        if (length == 0) return false;
        if (length == 1) {
            return primitiveUvs[0] == u && primitiveUvs[1] == v;
        }
        if (length == 2) {
            return pointOnSegment(primitiveUvs[0], primitiveUvs[1], primitiveUvs[2], primitiveUvs[3], u, v);
        }
        boolean inside = false;
        for (int i = 0, j = length - 1; i < length; j = i++) {
            int current = i * 2;
            int previous = j * 2;
            double currentU = primitiveUvs[current], currentV = primitiveUvs[current + 1];
            double previousU = primitiveUvs[previous], previousV = primitiveUvs[previous + 1];
            if (pointOnSegment(currentU, currentV, previousU, previousV, u, v)) {
                return true;
            }
            boolean intersects = ((currentV > v) != (previousV > v)) &&
                    (u < (previousU - currentU) * (v - currentV) / (previousV - currentV) + currentU);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean pointOnSegment(double u1, double v1, double u2, double v2, double u, double v) {
        double cross = (u2 - u1) * (v - v1) - (v2 - v1) * (u - u1);
        if (Math.abs(cross) > 1.0E-7D) return false;
        return u >= Math.min(u1, u2) && u <= Math.max(u1, u2) && v >= Math.min(v1, v2) && v <= Math.max(v1, v2);
    }
}