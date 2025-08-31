package com.xtracr.realcamera.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.nio.ByteBuffer;
import java.util.*;

public interface MultiVertexCatcher extends MultiBufferSource {
    void sendVertices(VertexRecorder recorder);

    static MultiVertexCatcher defaultImpl() {
        return MultiMeshCatcher.INSTANCE;
    }

    class MultiMeshCatcher extends MultiBufferSource.BufferSource implements MultiVertexCatcher {
        protected final static MultiMeshCatcher INSTANCE = new MultiMeshCatcher();
        protected final SortedMap<VertexData[], RenderType> caughtData = new Object2ObjectLinkedOpenHashMap<>();

        protected MultiMeshCatcher() {
            super(new BufferBuilder(256), ImmutableMap.of());
        }

        protected void putVertexData(RenderType renderType, BufferBuilder.RenderedBuffer renderedBuffer) {
            ByteBuffer vertexBuffer = renderedBuffer.vertexBuffer();
            BufferBuilder.DrawState drawState = renderedBuffer.drawState();
            VertexFormat format = drawState.format();
            int vertexCount = drawState.vertexCount();
            int vertexSize = format.getVertexSize();
            List<VertexFormatElement> elements = format.getElements();
            VertexData[] vertices = new VertexData[vertexCount];
            for (int i = 0; i < vertexCount; i ++) {
                float x = 0, y = 0, z = 0, u = 0, v = 0, normalX = 0, normalY = 0, normalZ = 0;
                int argb = 0, overlay = 0, light = 0;
                int vertexOffset = i * vertexSize, offset = vertexOffset;
                for (VertexFormatElement element : elements) {
                    switch (element.getUsage()) {
                        case POSITION:
                            x = vertexBuffer.getFloat(offset);
                            y = vertexBuffer.getFloat(offset + 4);
                            z = vertexBuffer.getFloat(offset + 8);
                            break;
                        case COLOR:
                            argb = vertexBuffer.getInt(offset);
                            break;
                        case UV:
                            switch (element.getIndex()) {
                                case 0:
                                    u = vertexBuffer.getFloat(offset);
                                    v = vertexBuffer.getFloat(offset + 4);
                                    break;
                                case 1:
                                    overlay = vertexBuffer.getInt(offset);
                                    break;
                                case 2:
                                    light = vertexBuffer.getInt(offset);
                                    break;
                            }
                            break;
                        case NORMAL:
                            normalX = ((int) vertexBuffer.get(offset)) / 127.0f;
                            normalY = ((int) vertexBuffer.get(offset + 1)) / 127.0f;
                            normalZ = ((int) vertexBuffer.get(offset + 2)) / 127.0f;
                            break;
                    }
                    offset += element.getByteSize();
                }
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
