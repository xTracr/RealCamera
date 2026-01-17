package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.SequencedMap;
import java.util.function.Consumer;

public abstract class MultiVertexCatcher extends MultiBufferSource.BufferSource {
    protected MultiVertexCatcher(ByteBufferBuilder sharedBuffer, SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers) {
        super(sharedBuffer, fixedBuffers);
    }

    public static MultiVertexCatcher defaultImpl() {
        return MeshCatcher.INSTANCE;
    }

    public abstract void endCatching(Consumer<BuiltIterableBuffer> consumer);

    static class MeshCatcher extends MultiVertexCatcher {
        private static final MeshCatcher INSTANCE = new MeshCatcher();
        private final SequencedMap<RenderType, ByteBufferBuilderPool> bufferPools = new Object2ObjectLinkedOpenHashMap<>();
        private final SequencedMap<MeshData, RenderType> caughtMeshes = new Object2ObjectLinkedOpenHashMap<>();

        protected MeshCatcher() {
            super(new ByteBufferBuilder(0), Object2ObjectSortedMaps.emptyMap());
        }

        @Override
        public @NotNull VertexConsumer getBuffer(@NotNull RenderType renderType) {
            BufferBuilder bufferBuilder = startedBuilders.get(renderType);
            if (bufferBuilder != null) {
                endBatch(renderType, bufferBuilder);
            }
            ByteBufferBuilderPool bufferPool = bufferPools.computeIfAbsent(renderType, type -> new ByteBufferBuilderPool(type.bufferSize()));
            bufferBuilder = new BufferBuilder(bufferPool.getBuffer(), renderType.mode(), renderType.format());
            startedBuilders.put(renderType, bufferBuilder);
            return bufferBuilder;
        }

        @Override
        public void endCatching(Consumer<BuiltIterableBuffer> consumer) {
            endBatch();
            caughtMeshes.forEach((meshData, renderType) -> {
                consumer.accept(BuiltIterableBuffer.buildFrom(renderType, meshData));
                meshData.close();
            });
            caughtMeshes.clear();
            bufferPools.values().forEach(ByteBufferBuilderPool::release);
        }

        @Override
        public void endBatch() {
            for(RenderType renderType : bufferPools.keySet()) {
                endBatch(renderType);
            }
        }

        @Override
        protected void endBatch(@NotNull RenderType renderType, BufferBuilder bufferBuilder) {
            MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
                caughtMeshes.put(meshData, renderType);
            }
        }

        private static class ByteBufferBuilderPool {
            private final int bufferSize;
            private ByteBufferBuilder[] pool = new ByteBufferBuilder[0];
            private int next = 0;

            public ByteBufferBuilderPool(int bufferSize) {
                this.bufferSize = bufferSize;
            }

            public ByteBufferBuilder getBuffer() {
                ByteBufferBuilder byteBufferBuilder;
                if (next < pool.length) {
                    byteBufferBuilder = pool[next++];
                } else {
                    pool = Arrays.copyOf(pool, pool.length + 1);
                    pool[next++] = byteBufferBuilder = new ByteBufferBuilder(bufferSize);
                }
                return byteBufferBuilder;
            }

            public void release() {
                next = 0;
            }
        }
    }
}
