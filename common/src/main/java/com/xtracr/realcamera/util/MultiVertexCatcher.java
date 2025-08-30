package com.xtracr.realcamera.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public interface MultiVertexCatcher extends MultiBufferSource {
    void sendVertices(VertexRecorder recorder);

    static MultiVertexCatcher defaultImpl() {
        return MultiMeshCatcher.INSTANCE;
    }

    class MultiMeshCatcher extends MultiBufferSource.BufferSource implements MultiVertexCatcher {
        private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        protected final static MultiMeshCatcher INSTANCE = new MultiMeshCatcher();
        protected final SortedMap<VertexData[], RenderType> caughtData = new Object2ObjectLinkedOpenHashMap<>();

        protected MultiMeshCatcher() {
            super(new BufferBuilder(256), ImmutableMap.of());
        }

        protected void putVertexData(RenderType renderType, BufferBuilder.RenderedBuffer renderedBuffer) {
            ByteBuffer vertexBuffer = renderedBuffer.vertexBuffer();
            BufferBuilder.DrawState drawState = renderedBuffer.drawState();
            VertexFormat vertexFormat = drawState.format();
            boolean fullFormat = vertexFormat == DefaultVertexFormat.NEW_ENTITY;
            int vertexCount = drawState.vertexCount();
            int vertexSize = vertexFormat.getVertexSize();
            VertexData[] vertices = new VertexData[vertexCount];
            for (int i = 0; i < vertexCount; i++) {
                int vertexOffset = i * vertexSize;
                float x = vertexBuffer.getFloat(vertexOffset);
                float y = vertexBuffer.getFloat(vertexOffset + 4);
                float z = vertexBuffer.getFloat(vertexOffset + 8);
                int argb = vertexBuffer.getInt(vertexOffset + 12);
                argb = IS_LITTLE_ENDIAN ? argb : Integer.reverseBytes(argb);
                float u = vertexBuffer.getFloat(vertexOffset + 16);
                float v = vertexBuffer.getFloat(vertexOffset + 20);
                int overlay = vertexBuffer.getInt(vertexOffset + 24);
                int offset, light;
                if (fullFormat) {
                    offset = vertexOffset + 28;
                    light = vertexBuffer.getInt(offset);
                } else {
                    offset = vertexOffset + 24;
                    light = 0;
                }
                float normalX = ((int) vertexBuffer.get(offset + 4)) / 127.0f;
                float normalY = ((int) vertexBuffer.get(offset + 5)) / 127.0f;
                float normalZ = ((int) vertexBuffer.get(offset + 6)) / 127.0f;
                vertices[i] = new VertexData(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
            }
            caughtData.put(vertices, renderType);
            renderedBuffer.release();
        }

        @Override
        public void sendVertices(VertexRecorder recorder) {
            endLastBatch();
            for (Map.Entry<VertexData[], RenderType> entry : caughtData.entrySet()) {
                recorder.records().add(VertexRecorder.buildVertices(entry.getValue(), entry.getKey()));
            }
            caughtData.clear();
        }

        @Override
        public void endBatch(RenderType renderType) {
            BufferBuilder bufferBuilder = getBuilderRaw(renderType);
            boolean bl = Objects.equals(lastState, renderType.asOptional());
            if (bl || bufferBuilder != builder) {
                if (startedBuffers.remove(bufferBuilder)) {
                    if (bufferBuilder.building()) {
                        putVertexData(renderType, bufferBuilder.end());
                    }
                    if (bl) {
                        lastState = Optional.empty();
                    }
                }
            }
        }
    }
}
