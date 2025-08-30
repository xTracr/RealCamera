package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;


public interface MultiVertexCatcher extends MultiBufferSource {
    void sendVertices(VertexRecorder recorder);

    static MultiVertexCatcher meshCatcher() {
        return MultiRecordBuilder.INSTANCE;
    }

    static MultiVertexCatcher simpleCatcher() {
        return SimpleMultiVertexCatcher.INSTANCE;
    }

    class SimpleMultiVertexCatcher implements MultiVertexCatcher {
        protected static final SimpleMultiVertexCatcher INSTANCE = new SimpleMultiVertexCatcher();
        protected final Stack<SimpleCatcher> catchers = new Stack<>();

        @Override
        public void sendVertices(VertexRecorder recorder) {
            for (SimpleCatcher catcher : catchers) {
                recorder.records().add(VertexRecorder.buildVertices(catcher.renderType(), catcher.collectVertices()));
            }
            catchers.clear();
        }

        @Override
        public @NotNull VertexConsumer getBuffer(RenderType renderType) {
            if (catchers.isEmpty() || !Objects.equals(catchers.peek().renderType(), renderType) || !renderType.canConsolidateConsecutiveGeometry()) {
                return catchers.push(new SimpleCatcher(renderType));
            }
            return catchers.peek();
        }

        protected static class SimpleCatcher implements VertexConsumer {
            protected final List<VertexData> vertexList = new ArrayList<>();
            protected final RenderType renderType;
            private float x, y, z, u, v, normalX, normalY, normalZ;
            private int argb, overlay, light;
            private boolean active;

            public SimpleCatcher(RenderType renderType) {
                this.renderType = renderType;
            }

            protected void endVertex() {
                if (!active) return;
                addVertexInternal(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
                x = y = z = normalX = normalY = normalZ = 0;
                u = v = overlay = light = argb = 0;
                active = false;
            }

            protected void addVertexInternal(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
                vertexList.add(new VertexData(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ));
            }

            public RenderType renderType() {
                return renderType;
            }

            public VertexData[] collectVertices() {
                endVertex();
                return this.vertexList.toArray(VertexData[]::new);
            }

            @Override
            public @NotNull VertexConsumer addVertex(float x, float y, float z) {
                endVertex();
                active = true;
                this.x = x;
                this.y = y;
                this.z = z;
                return this;
            }

            @Override
            public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
                argb = alpha << 24 | red << 16 | green << 8 | blue;
                return this;
            }

            @Override
            public @NotNull VertexConsumer setUv(float u, float v) {
                this.u = u;
                this.v = v;
                return this;
            }

            @Override
            public @NotNull VertexConsumer setUv1(int u, int v) {
                overlay = (short) u | (short) v << 16;
                return this;
            }

            @Override
            public @NotNull VertexConsumer setUv2(int u, int v) {
                light = (short) u | (short) v << 16;
                return this;
            }

            @Override
            public @NotNull VertexConsumer setNormal(float x, float y, float z) {
                normalX = x;
                normalY = y;
                normalZ = z;
                return this;
            }

            @Override
            public void addVertex(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
                addVertexInternal(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
            }

            @Override
            public @NotNull VertexConsumer setColor(int argb) {
                this.argb = argb;
                return this;
            }

            @Override
            public @NotNull VertexConsumer setOverlay(int overlay) {
                this.overlay = overlay;
                return this;
            }

            @Override
            public @NotNull VertexConsumer setLight(int light) {
                this.light = light;
                return this;
            }
        }
    }

    class MultiRecordBuilder extends MultiBufferSource.BufferSource implements MultiVertexCatcher {
        protected static final MultiRecordBuilder INSTANCE = new MultiRecordBuilder();
        protected final Stack<RecordBuilder> catchers = new Stack<>();

        protected MultiRecordBuilder() {
            super(new ByteBufferBuilder(786432), Object2ObjectSortedMaps.emptyMap());
        }

        @Override
        public void sendVertices(VertexRecorder recorder) {
            for (RecordBuilder catcher : catchers) {
                recorder.records().add(VertexRecorder.buildVertices(catcher.renderType(), catcher.collectVertices()));
            }
            catchers.clear();
        }

        @Override
        public @NotNull VertexConsumer getBuffer(RenderType renderType) {
            BufferBuilder bufferBuilder = startedBuilders.get(renderType);
            if (bufferBuilder != null && !renderType.canConsolidateConsecutiveGeometry()) {
                endBatch(renderType, bufferBuilder);
                bufferBuilder = null;
            }
            if (bufferBuilder == null) {
                ByteBufferBuilder byteBufferBuilder = fixedBuffers.get(renderType);
                if (byteBufferBuilder != null) {
                    bufferBuilder = new RecordBuilder(renderType, byteBufferBuilder, renderType.mode(), renderType.format());
                } else {
                    if (lastSharedType != null) {
                        endBatch(lastSharedType);
                    }
                    bufferBuilder = new RecordBuilder(renderType, sharedBuffer, renderType.mode(), renderType.format());
                    lastSharedType = renderType;
                }
                startedBuilders.put(renderType, bufferBuilder);
                if (bufferBuilder instanceof RecordBuilder recordBuilder) catchers.push(recordBuilder);
            }
            return bufferBuilder;
        }

        @Override
        protected void endBatch(RenderType renderType, BufferBuilder bufferBuilder) {
            if (renderType.equals(lastSharedType)) {
                lastSharedType = null;
            }
        }

        protected static class RecordBuilder extends BufferBuilder {
            protected final List<VertexData> vertexList = new ArrayList<>();
            protected final RenderType renderType;
            private float x, y, z, u, v, normalX, normalY, normalZ;
            private int argb, overlay, light;
            private boolean active;

            public RecordBuilder(RenderType renderType, ByteBufferBuilder byteBufferBuilder, VertexFormat.Mode mode, VertexFormat vertexFormat) {
                super(byteBufferBuilder, mode, vertexFormat);
                this.renderType = renderType;
            }

            protected void endVertex() {
                if (!active) return;
                addVertexInternal(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
                x = y = z = normalX = normalY = normalZ = 0;
                u = v = overlay = light = argb = 0;
                active = false;
            }

            protected void addVertexInternal(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
                vertexList.add(new VertexData(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ));
            }

            public RenderType renderType() {
                return renderType;
            }

            public VertexData[] collectVertices() {
                endVertex();
                return vertexList.toArray(VertexData[]::new);
            }

            @Override
            public @NotNull VertexConsumer addVertex(float x, float y, float z) {
                endVertex();
                active = true;
                this.x = x;
                this.y = y;
                this.z = z;
                return super.addVertex(x, y, z);
            }

            @Override
            public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
                argb = alpha << 24 | red << 16 | green << 8 | blue;
                return super.setColor(red, green, blue, alpha);
            }

            @Override
            public @NotNull VertexConsumer setColor(int argb) {
                this.argb = argb;
                return super.setColor(argb);
            }

            @Override
            public @NotNull VertexConsumer setUv(float u, float v) {
                this.u = u;
                this.v = v;
                return super.setUv(u, v);
            }

            @Override
            public @NotNull VertexConsumer setUv1(int u, int v) {
                overlay = (short) u | (short) v << 16;
                return super.setUv1(u, v);
            }

            @Override
            public @NotNull VertexConsumer setOverlay(int overlay) {
                this.overlay = overlay;
                return super.setOverlay(overlay);
            }

            @Override
            public @NotNull VertexConsumer setUv2(int u, int v) {
                light = (short) u | (short) v << 16;
                return super.setUv2(u, v);
            }

            @Override
            public @NotNull VertexConsumer setLight(int light) {
                this.light = light;
                return super.setLight(light);
            }

            @Override
            public @NotNull VertexConsumer setNormal(float x, float y, float z) {
                normalX = x;
                normalY = y;
                normalZ = z;
                return super.setNormal(x, y, z);
            }

            @Override
            public void addVertex(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
                addVertexInternal(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
                super.addVertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
            }
        }
    }
}
