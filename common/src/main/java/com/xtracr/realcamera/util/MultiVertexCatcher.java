package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.SortedMap;

public interface MultiVertexCatcher extends MultiBufferSource {
    static MultiVertexCatcher defaultImpl() {
        return MeshCatcher.meshCatcher;
    }

    void sendVertices(VertexRecorder recorder);

    class MeshCatcher extends MultiBufferSource.BufferSource implements MultiVertexCatcher {
        private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        private static final MeshCatcher meshCatcher = new MeshCatcher();
        protected final SortedMap<VertexData[], RenderType> caughtData = new Object2ObjectLinkedOpenHashMap<>();

        protected MeshCatcher() {
            super(new ByteBufferBuilder(1536), new Object2ObjectLinkedOpenHashMap<>());
        }

        protected void putVertexData(RenderType renderType, MeshData meshData) {
            ByteBuffer vertexBuffer = meshData.vertexBuffer();
            MeshData.DrawState drawState = meshData.drawState();
            VertexFormat format = drawState.format();
            int vertexCount = drawState.vertexCount();
            int vertexSize = format.getVertexSize();
            int positionOffset = format.getOffset(VertexFormatElement.POSITION);
            int colorOffset = format.getOffset(VertexFormatElement.COLOR);
            int uvOffset = format.getOffset(VertexFormatElement.UV0);
            int overlayOffset = format.getOffset(VertexFormatElement.UV1);
            int lightOffset = format.getOffset(VertexFormatElement.UV2);
            int normalOffset = format.getOffset(VertexFormatElement.NORMAL);
            VertexData[] vertices = new VertexData[vertexCount];
            float x = 0, y = 0, z = 0, u = 0, v = 0, normalX = 0, normalY = 0, normalZ = 0;
            int argb = 0, overlay = 0, light = 0, vertexOffset = 0, offset;
            for (int i = 0; i < vertexCount; i++, vertexOffset += vertexSize) {
                offset = vertexOffset + positionOffset;
                if (positionOffset != -1) {
                    x = vertexBuffer.getFloat(offset);
                    y = vertexBuffer.getFloat(offset + 4);
                    z = vertexBuffer.getFloat(offset + 8);
                }
                if (colorOffset != -1) {
                    argb = vertexBuffer.getInt(vertexOffset + colorOffset);
                    argb = IS_LITTLE_ENDIAN ? argb : Integer.reverseBytes(argb);
                }
                if (uvOffset != -1) {
                    offset = vertexOffset + uvOffset;
                    u = vertexBuffer.getFloat(offset);
                    v = vertexBuffer.getFloat(offset + 4);
                }
                if (overlayOffset != -1) {
                    overlay = vertexBuffer.getInt(vertexOffset + overlayOffset);
                }
                if (lightOffset != -1) {
                    light = vertexBuffer.getInt(vertexOffset + lightOffset);
                }
                if (normalOffset != -1) {
                    offset = vertexOffset + normalOffset;
                    normalX = ((int) vertexBuffer.get(offset)) / 127.0f;
                    normalY = ((int) vertexBuffer.get(offset + 1)) / 127.0f;
                    normalZ = ((int) vertexBuffer.get(offset + 2)) / 127.0f;
                }
                vertices[i] = VertexData.object(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
            }
            caughtData.put(vertices, renderType);
            meshData.close();
        }

        @Override
        public void sendVertices(VertexRecorder recorder) {
            endBatch();
            caughtData.forEach((vertices, renderType) -> recorder.records().add(VertexRecorder.buildVertices(renderType, vertices)));
            caughtData.clear();
        }

        @Override
        public @NotNull VertexConsumer getBuffer(RenderType renderType) {
            BufferBuilder bufferBuilder = startedBuilders.get(renderType);
            if (bufferBuilder != null && !renderType.canConsolidateConsecutiveGeometry()) {
                endBatch(renderType, bufferBuilder);
                bufferBuilder = null;
            }
            if (bufferBuilder == null) {
                ByteBufferBuilder byteBufferBuilder = fixedBuffers.computeIfAbsent(renderType, type -> new ByteBufferBuilder(type.bufferSize()));
                bufferBuilder = new BufferBuilder(byteBufferBuilder, renderType.mode(), renderType.format());
                startedBuilders.put(renderType, bufferBuilder);
            }
            return bufferBuilder;
        }

        @Override
        protected void endBatch(RenderType renderType, BufferBuilder bufferBuilder) {
            MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
                putVertexData(renderType, meshData);
            }
            if (renderType.equals(lastSharedType)) {
                lastSharedType = null;
            }
        }
    }
}
