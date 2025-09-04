package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.*;
import com.xtracr.realcamera.mixin.accessor.BufferSourceAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;

public interface MultiVertexCatcher extends MultiBufferSource {
    static MultiVertexCatcher defaultImpl() {
        if (MeshCatcher.meshCatcher == null) {
            MeshCatcher.meshCatcher = new MeshCatcher(Minecraft.getInstance().renderBuffers().bufferSource());
        }
        return MeshCatcher.meshCatcher;
    }

    void sendVertices(VertexRecorder recorder);

    class MeshCatcher extends MultiBufferSource.BufferSource implements MultiVertexCatcher {
        private static MeshCatcher meshCatcher;
        protected final SortedMap<VertexData[], RenderType> caughtData = new Object2ObjectLinkedOpenHashMap<>();

        protected MeshCatcher(BufferSource bufferSource) {
            super(new BufferBuilder(256), Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
                for (RenderType renderType : ((BufferSourceAccessor) bufferSource).getFixedBuffers().keySet()) {
                    map.put(renderType, new BufferBuilder(renderType.bufferSize()));
                }
            }));
        }

        protected void putVertexData(RenderType renderType, BufferBuilder.RenderedBuffer renderedBuffer) {
            ByteBuffer vertexBuffer = renderedBuffer.vertexBuffer();
            BufferBuilder.DrawState drawState = renderedBuffer.drawState();
            VertexFormat format = drawState.format();
            int vertexCount = drawState.vertexCount();
            int vertexSize = format.getVertexSize();
            List<VertexFormatElement> elements = format.getElements();
            VertexData[] vertices = new VertexData[vertexCount];
            for (int i = 0; i < vertexCount; i++) {
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
            endBatch();
            caughtData.forEach((vertices, renderType) -> recorder.records().add(VertexRecorder.buildVertices(renderType, vertices)));
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
