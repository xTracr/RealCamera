package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.MeshData;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.renderer.state.VertexData.UV;
import com.xtracr.realcamera.util.RenderTypeUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public record BuiltIterableBuffer(RenderType renderType, String textureId, IterableVertexBuffer vertexBuffer) {
    public static BuiltIterableBuffer buildFrom(RenderType renderType, MeshData meshData) {
        return new BuiltIterableBuffer(renderType, RenderTypeUtil.getTextureId(renderType), new IterableVertexBuffer(meshData));
    }

    public boolean anyNotCached(UV[] uvs) {
        Map<UV, Integer> cache = RenderTypeUtil.getPrimitiveCache(renderType);
        if (cache == null) return true;
        for (UV uv : uvs) {
            if (!cache.containsKey(uv)) return true;
        }
        return false;
    }

    public VertexData[] @Nullable [] findPrimitivesInCache(UV[] uvs) {
        Map<UV, Integer> cache = RenderTypeUtil.getPrimitiveCache(renderType);
        int uvsLength = uvs.length;
        VertexData[][] primitives = new VertexData[uvsLength][];
        if (cache == null) return primitives;
        for (int i = 0; i < uvsLength; i++) {
            if (uvs[i] == null) continue;
            Integer primitiveIndex = cache.get(uvs[i]);
            if (primitiveIndex == null) continue;
            VertexData[] primitive = vertexBuffer.readPrimitiveAt(primitiveIndex);
            if (!VertexData.containsUV(primitive, uvs[i].u(), uvs[i].v())) continue;
            primitives[i] = primitive;
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
                RenderTypeUtil.cachePrimitive(renderType, uv, idx);
                primitives[i] = VertexData.asImmutable(primitive);
            }
            return allFound;
        });
        return primitives;
    }
}
