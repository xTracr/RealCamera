package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface MultiVertexCatcher extends MultiBufferSource {
    static MultiVertexCatcher defaultImpl(Consumer<BuiltIterableBuffer> consumer) {
        return MeshCatcher.meshCatcher.setConsumer(consumer);
    }

    void endCatching();

    class MeshCatcher extends MultiBufferSource.BufferSource implements MultiVertexCatcher {
        private static final MeshCatcher meshCatcher = new MeshCatcher();
        private Consumer<BuiltIterableBuffer> consumer;

        protected MeshCatcher() {
            super(new ByteBufferBuilder(1536), new Object2ObjectLinkedOpenHashMap<>());
        }

        public MultiVertexCatcher setConsumer(Consumer<BuiltIterableBuffer> consumer) {
            this.consumer = consumer;
            return this;
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
        public void endCatching() {
            endBatch();
        }

        @Override
        protected void endBatch(RenderType renderType, BufferBuilder bufferBuilder) {
            MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
                if (consumer != null) {
                    consumer.accept(BuiltIterableBuffer.buildFrom(renderType, meshData));
                }
                meshData.close();
            }
            if (renderType.equals(lastSharedType)) {
                lastSharedType = null;
            }
        }
    }
}
