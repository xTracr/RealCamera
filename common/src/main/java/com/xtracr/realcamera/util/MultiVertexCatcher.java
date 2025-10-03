package com.xtracr.realcamera.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.SortedMap;
import java.util.function.Consumer;

public interface MultiVertexCatcher extends MultiBufferSource {
    static MultiVertexCatcher defaultImpl() {
        return MeshCatcher.INSTANCE;
    }

    void endCatching(Consumer<BuiltIterableBuffer> consumer);

    class MeshCatcher extends MultiBufferSource.BufferSource implements MultiVertexCatcher {
        private static final MeshCatcher INSTANCE = new MeshCatcher();
        protected final SortedMap<RenderType, BufferBuilder> startedBuilders = new Object2ObjectLinkedOpenHashMap<>();
        private final SortedMap<RenderType, BufferBuilderPool> bufferPools = new Object2ObjectLinkedOpenHashMap<>();
        private final SortedMap<RenderedBuffer, RenderType> caughtMeshes = new Object2ObjectLinkedOpenHashMap<>();

        protected MeshCatcher() {
            super(new BufferBuilder(0), ImmutableMap.of());
        }

        @Override
        public @NotNull VertexConsumer getBuffer(RenderType renderType) {
            BufferBuilder bufferBuilder = startedBuilders.get(renderType);
            if (bufferBuilder != null) {
                endBatch(renderType, bufferBuilder);
            }
            bufferBuilder = getBuilderRaw(renderType);
            startedBuilders.put(renderType, bufferBuilder);
            bufferBuilder.begin(renderType.mode(), renderType.format());
            return bufferBuilder;
        }

        @Override
        protected @NotNull BufferBuilder getBuilderRaw(RenderType renderType) {
            return bufferPools.computeIfAbsent(renderType, type -> new BufferBuilderPool(type.bufferSize())).getBuffer();
        }

        @Override
        public void endCatching(Consumer<BuiltIterableBuffer> consumer) {
            endBatch();
            caughtMeshes.forEach((meshData, renderType) -> {
                consumer.accept(BuiltIterableBuffer.buildFrom(renderType, meshData));
                meshData.release();
            });
            caughtMeshes.clear();
            bufferPools.values().forEach(BufferBuilderPool::release);
        }

        @Override
        public void endBatch() {
            for(RenderType renderType : bufferPools.keySet()) {
                endBatch(renderType);
            }
        }

        @Override
        public void endBatch(RenderType renderType) {
            BufferBuilder bufferBuilder = startedBuilders.remove(renderType);
            if (bufferBuilder != null) {
                endBatch(renderType, bufferBuilder);
            }
        }

        protected void endBatch(RenderType renderType, BufferBuilder bufferBuilder) {
            RenderedBuffer meshData = bufferBuilder.end();
            caughtMeshes.put(meshData, renderType);
        }

        private static class BufferBuilderPool {
            private final int bufferSize;
            private BufferBuilder[] pool = new BufferBuilder[0];
            private int next = 0;

            public BufferBuilderPool(int bufferSize) {
                this.bufferSize = bufferSize;
            }

            public BufferBuilder getBuffer() {
                BufferBuilder bufferBuilder;
                if (next < pool.length) {
                    bufferBuilder = pool[next++];
                } else {
                    pool = Arrays.copyOf(pool, pool.length + 1);
                    pool[next++] = bufferBuilder = new BufferBuilder(bufferSize);
                }
                return bufferBuilder;
            }

            public void release() {
                next = 0;
            }
        }
    }
}
