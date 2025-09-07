package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface VertexRecorder {
    Pattern textureIdPattern = Pattern.compile("texture\\[Optional\\[(.*?)]");

    static BuiltRecord buildVertices(RenderType renderType, VertexData[] vertices) {
        String renderTypeName = renderType.toString();
        Matcher matcher = textureIdPattern.matcher(renderTypeName);
        String textureId = matcher.find() ? matcher.group(1) : renderTypeName;
        VertexFormat.Mode drawMode = renderType.mode();
        final int primitiveLength = drawMode.primitiveLength, primitiveStride = drawMode.primitiveStride;
        final int primitiveCount = (vertices.length - primitiveLength) / primitiveStride + 1;
        final boolean startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
        VertexData[][] primitives = new VertexData[primitiveCount][primitiveLength];
        for (int i = 0, k = 0; i < primitiveCount; i++, k += primitiveStride) {
            primitives[i][0] = vertices[startWithFirst ? 0 : k];
            System.arraycopy(vertices, k + 1, primitives[i], 1, primitiveLength - 1);
        }
        return new BuiltRecord(renderType, textureId, vertices, primitives);
    }

    List<BuiltRecord> records();

    void setCatcher(MultiVertexCatcher catcher);

    void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack);

    BindResult computeBindResult();

    record BuiltRecord(RenderType renderType, String textureId, VertexData[] vertices, VertexData[][] primitives) {
        private static final Map<RenderType, Map<UV, float[]>> FIND_PRIMITIVE_CACHE = new HashMap<>();

        public @Nullable VertexData[] findPrimitiveInCache(float u, float v) {
            Map<UV, float[]> cache = FIND_PRIMITIVE_CACHE.get(renderType);
            if (cache == null) return null;
            float[] cachedUVs = cache.get(new UV(u, v));
            if (cachedUVs == null) return null;
            int length = cachedUVs.length / 2;
            primitiveFor:
            for (VertexData[] primitive : primitives) {
                if (primitive.length != length) continue;
                for (int i = 0; i < length; i++) {
                    if (cachedUVs[i * 2] != primitive[i].u() || cachedUVs[i * 2 + 1] != primitive[i].v()) continue primitiveFor;
                }
                return primitive;
            }
            return null;
        }

        public @Nullable VertexData[] findPrimitive(float u, float v) {
            final int resolution = 1000000;
            int length = 0;
            int[] us = new int[0], vs = new int[0];
            for (VertexData[] primitive : primitives) {
                if (length != primitive.length) {
                    length = primitive.length;
                    us = new int[length];
                    vs = new int[length];
                }
                for (int i = 0; i < length; i++) {
                    us[i] = (int) (resolution * primitive[i].u());
                    vs[i] = (int) (resolution * primitive[i].v());
                }
                if (!new Polygon(us, vs, length).contains(resolution * u, resolution * v)) continue;
                float[] uvs = new float[length * 2];
                for (int i = 0; i < length; i++) {
                    uvs[i * 2] = primitive[i].u();
                    uvs[i * 2 + 1] = primitive[i].v();
                }
                FIND_PRIMITIVE_CACHE.computeIfAbsent(renderType, k -> new HashMap<>()).put(new UV(u, v), uvs);
                return primitive;
            }
            return null;
        }

        protected record UV(float u, float v) {
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if ((!(o instanceof UV(float u1, float v1)))) return false;
                return Float.compare(u, u1) == 0 && Float.compare(v, v1) == 0;
            }

            @Override
            public int hashCode() {
                return Float.hashCode(u) * 31 + Float.hashCode(v);
            }
        }
    }
}
