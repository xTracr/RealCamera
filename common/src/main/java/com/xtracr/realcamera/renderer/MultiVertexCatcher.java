package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Arrays;
import java.util.SequencedMap;
import java.util.function.Consumer;

public final class MultiVertexCatcher {
    private final SubmitNodeStorage storage = new SubmitNodeStorage();
    private final MeshCatcher meshCatcher = new MeshCatcher();

    private MultiVertexCatcher() {
    }

    public static MultiVertexCatcher create() {
        return new MultiVertexCatcher();
    }

    @SuppressWarnings("resource")
    private static <S> void renderModelSubmit(ModelFeatureRenderer.Submit<S> submit, VertexConsumer buffer) {
        PoseStack poseStack = new PoseStack();
        poseStack.last().set(submit.pose());
        VertexConsumer wrappedBuffer;
        if (submit.sheetedDecalPose() != null) {
            wrappedBuffer = new SheetedDecalTextureGenerator(buffer, submit.sheetedDecalPose(), 1.0F);
        } else if (submit.sprite() != null) {
            wrappedBuffer = submit.sprite().wrap(buffer);
        } else {
            wrappedBuffer = buffer;
        }
        Model<? super S> model = submit.model();
        model.setupAnim(submit.state());
        model.renderToBuffer(poseStack, wrappedBuffer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
    }

    public SubmitNodeCollector initCollector() {
        meshCatcher.clear();
        storage.getSubmitsPerOrder().clear();
        return storage;
    }

    public void forEachBuffer(Consumer<BuiltIterableBuffer> consumer) {
        if (!storage.getSubmitsPerOrder().isEmpty()) {
            storage.drainPhases(phase -> phase.sortInto((submitNode, _) -> renderSubmit(submitNode)));
            meshCatcher.endBatch();
        }
        meshCatcher.forEachBuffer(consumer);
    }

    private void renderSubmit(SubmitNode submitNode) {
        if (submitNode.featureType() == ModelFeatureRenderer.TYPE) {
            ModelFeatureRenderer.Submit<?> submit = (ModelFeatureRenderer.Submit<?>) submitNode;
            VertexConsumer buffer = meshCatcher.getBuffer(submit.renderType());
            renderModelSubmit(submit, buffer);
        } else if (submitNode.featureType() == CustomFeatureRenderer.TYPE) {
            CustomFeatureRenderer.Submit submit = (CustomFeatureRenderer.Submit) submitNode;
            VertexConsumer buffer = meshCatcher.getBuffer(submit.renderType());
            submit.customGeometryRenderer().render(submit.pose(), buffer);
        }
    }

    static final class MeshCatcher {
        private final SequencedMap<RenderType, ByteBufferBuilderPool> bufferPools = new Object2ObjectLinkedOpenHashMap<>();
        private final SequencedMap<RenderType, BufferBuilder> activeBuilders = new Object2ObjectLinkedOpenHashMap<>();
        private final SequencedMap<MeshData, RenderType> caughtMeshes = new Object2ObjectLinkedOpenHashMap<>();

        public VertexConsumer getBuffer(RenderType renderType) {
            BufferBuilder existing = activeBuilders.get(renderType);
            if (existing != null) {
                endBatch(renderType, existing);
            }
            ByteBufferBuilderPool bufferPool = bufferPools.computeIfAbsent(renderType, _ -> new ByteBufferBuilderPool(RenderType.SMALL_BUFFER_SIZE));
            BufferBuilder bufferBuilder = new BufferBuilder(bufferPool.getBuffer(), renderType.primitiveTopology(), renderType.format());
            activeBuilders.put(renderType, bufferBuilder);
            return bufferBuilder;
        }

        public void clear() {
            for (MeshData meshData : caughtMeshes.keySet()) {
                meshData.close();
            }
            caughtMeshes.clear();
            activeBuilders.clear();
            bufferPools.values().forEach(ByteBufferBuilderPool::release);
        }

        public void forEachBuffer(Consumer<BuiltIterableBuffer> consumer) {
            for (var entry : caughtMeshes.entrySet()) {
                consumer.accept(BuiltIterableBuffer.buildFrom(entry.getValue(), entry.getKey()));
            }
        }

        public void endBatch() {
            for (RenderType renderType : activeBuilders.keySet()) {
                BufferBuilder builder = activeBuilders.get(renderType);
                endBatch(renderType, builder);
            }
            activeBuilders.clear();
        }

        private void endBatch(RenderType renderType, BufferBuilder bufferBuilder) {
            MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
                caughtMeshes.put(meshData, renderType);
            }
        }

        private static final class ByteBufferBuilderPool {
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
