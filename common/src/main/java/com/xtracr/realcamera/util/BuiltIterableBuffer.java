package com.xtracr.realcamera.util;


import com.mojang.blaze3d.vertex.BufferBuilder;
import com.xtracr.realcamera.util.RenderTypeUtil.PrimitiveLayoutKey;
import com.xtracr.realcamera.util.VertexData.UV;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

public record BuiltIterableBuffer(RenderType renderType, String textureId, IterableVertexBuffer vertexBuffer,
                                  PrimitiveLayoutKey primitiveLayoutKey) {
    public static BuiltIterableBuffer buildFrom(RenderType renderType, BufferBuilder.RenderedBuffer meshData) {
        return buildFrom(renderType, meshData, 0);
    }

    public static BuiltIterableBuffer buildFrom(RenderType renderType, BufferBuilder.RenderedBuffer meshData, int meshOrdinal) {
        String textureId = RenderTypeUtil.getTextureId(renderType);
        IterableVertexBuffer vertexBuffer = new IterableVertexBuffer(meshData);
        PrimitiveLayoutKey primitiveLayoutKey = new PrimitiveLayoutKey(renderType, textureId, vertexBuffer.mode(),
                vertexBuffer.vertexSize, vertexBuffer.vertexCount, vertexBuffer.primitiveLength, vertexBuffer.primitiveCount,
                meshOrdinal, vertexBuffer.vertexLayoutHash());
        return new BuiltIterableBuffer(renderType, textureId, vertexBuffer, primitiveLayoutKey);
    }

    public boolean anyNotCached(UV[] uvs) {
        Object2IntMap<UV> cache = RenderTypeUtil.getPrimitiveCache(primitiveLayoutKey);
        if (cache == null) return true;
        for (UV uv : uvs) {
            if (uv == null) continue;
            if (cache.getInt(uv) == -1) return true;
        }
        return false;
    }

    public VertexData[] @Nullable [] findPrimitivesInCache(UV[] uvs) {
        Object2IntMap<UV> cache = RenderTypeUtil.getPrimitiveCache(primitiveLayoutKey);
        int uvsLength = uvs.length;
        VertexData[][] primitives = new VertexData[uvsLength][];
        if (cache == null) return primitives;
        for (int i = 0; i < uvsLength; i++) {
            if (uvs[i] == null) continue;
            int primitiveIndex = cache.getInt(uvs[i]);
            if (primitiveIndex == -1) continue;
            VertexData[] primitive;
            try {
                primitive = vertexBuffer.readPrimitiveAt(primitiveIndex);
            } catch (IndexOutOfBoundsException ignored) {
                return invalidatePrimitiveCache(uvsLength);
            }
            if (!VertexData.containsUV(primitive, uvs[i].u(), uvs[i].v())) return invalidatePrimitiveCache(uvsLength);
            primitives[i] = primitive;
        }
        return primitives;
    }

    public VertexData[] @Nullable [] resolvePrimitives(UV[] uvs) {
        VertexData[][] primitives = findPrimitivesInCache(uvs);
        if (!anyNotCached(uvs)) return primitives;
        UV[] missingUVs = uvs.clone();
        for (int i = 0; i < primitives.length; i++) {
            if (primitives[i] != null) missingUVs[i] = null;
        }
        VertexData[][] resolvedPrimitives = findPrimitives(missingUVs);
        for (int i = 0; i < primitives.length; i++) {
            if (resolvedPrimitives[i] != null) primitives[i] = resolvedPrimitives[i];
        }
        return primitives;
    }

    public VertexData[] @Nullable [] findPrimitives(UV[] uvs) {
        int uvsLength = uvs.length;
        VertexData[][] primitives = new VertexData[uvsLength][];
        int[] counter = new int[1];
        vertexBuffer.primitiveStream().anyMatch(primitive -> {
            int idx = counter[0]++;
            boolean allFound = true;
            for (int i = 0; i < uvsLength; i++) {
                if (primitives[i] != null) continue;
                UV uv = uvs[i];
                if (uv == null) continue;
                if (!VertexData.containsUV(primitive, uv.u(), uv.v())) {
                    allFound = false;
                    continue;
                }
                RenderTypeUtil.cachePrimitive(primitiveLayoutKey, uv, idx);
                primitives[i] = VertexData.asImmutable(primitive);
            }
            return allFound;
        });
        return primitives;
    }

    private VertexData[][] invalidatePrimitiveCache(int uvsLength) {
        RenderTypeUtil.invalidatePrimitiveCache(primitiveLayoutKey);
        return new VertexData[uvsLength][];
    }
}
