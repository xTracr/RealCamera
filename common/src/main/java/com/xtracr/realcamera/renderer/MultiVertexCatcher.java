package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.*;
import com.xtracr.realcamera.mixin.accessor.CustomFeatureRenderer$StorageAccessor;
import com.xtracr.realcamera.mixin.accessor.ModelFeatureRenderer$StorageAccessor;
import com.xtracr.realcamera.mixin.accessor.ModelPartFeatureRenderer$StorageAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Consumer;

public class MultiVertexCatcher {
    private final SubmitNodeStorage storage = new SubmitNodeStorage();
    private final MeshCatcher meshCatcher = new MeshCatcher();

    private MultiVertexCatcher() { }

    public static MultiVertexCatcher create() {
        return new MultiVertexCatcher();
    }

    private static <S> void renderModel(PoseStack poseStack, SubmitNodeStorage.ModelSubmit<S> submit, VertexConsumer buffer) {
        poseStack.pushPose();
        poseStack.last().set(submit.pose());
        Model<? super S> model = submit.model();
        //noinspection resource
        VertexConsumer wrappedBuffer = submit.sprite() == null ? buffer : submit.sprite().wrap(buffer);
        model.setupAnim(submit.state());
        model.renderToBuffer(poseStack, wrappedBuffer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
        poseStack.popPose();
    }

    private static void renderModelParts(PoseStack poseStack, Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> modelPartSubmitsMap, MultiBufferSource.BufferSource bufferSource) {
        poseStack.pushPose();
        for (Map.Entry<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> entry : modelPartSubmitsMap.entrySet()) {
            RenderType renderType = entry.getKey();
            List<SubmitNodeStorage.ModelPartSubmit> modelPartSubmits = entry.getValue();
            VertexConsumer buffer = bufferSource.getBuffer(renderType);

            for (SubmitNodeStorage.ModelPartSubmit submit : modelPartSubmits) {
                //noinspection resource
                VertexConsumer wrappedBuffer = submit.sprite() == null ? buffer : submit.sprite().wrap(buffer);
                poseStack.last().set(submit.pose());
                submit.modelPart().render(poseStack, wrappedBuffer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
            }
        }
        poseStack.popPose();
    }

    private void renderTranslucentFeatures() {
        for (SubmitNodeCollection collection : storage.getSubmitsPerOrder().values()) {
            PoseStack poseStack = new PoseStack();
            for (SubmitNodeStorage.TranslucentModelSubmit<?> submit : ((ModelFeatureRenderer$StorageAccessor) collection.getModelSubmits()).getTranslucentModelSubmits()) {
                renderModel(poseStack, submit.modelSubmit(), meshCatcher.getBuffer(submit.renderType()));
            }
            renderModelParts(poseStack, ((ModelPartFeatureRenderer$StorageAccessor) collection.getModelPartSubmits()).getTranslucentModelPartSubmits(), meshCatcher);

            for (Map.Entry<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> entry : ((CustomFeatureRenderer$StorageAccessor) collection.getCustomGeometrySubmits()).getTranslucentCustomGeometrySubmits().entrySet()) {
                VertexConsumer buffer = meshCatcher.getBuffer(entry.getKey());
                for (SubmitNodeStorage.CustomGeometrySubmit customGeometrySubmit : entry.getValue()) {
                    customGeometrySubmit.customGeometryRenderer().render(customGeometrySubmit.pose(), buffer);
                }
            }
        }
    }

    public SubmitNodeStorage initialize() {
        meshCatcher.initialize();
        return storage;
    }

    public void endCatching(Consumer<BuiltIterableBuffer> consumer) {
        if (storage.getSubmitsPerOrder().isEmpty()) return;
        renderTranslucentFeatures();
        storage.clear();
        meshCatcher.endCatching(consumer);
    }

    static class MeshCatcher extends MultiBufferSource.BufferSource {
        private final SequencedMap<RenderType, ByteBufferBuilderPool> bufferPools = new Object2ObjectLinkedOpenHashMap<>();
        private final SequencedMap<MeshData, RenderType> caughtMeshes = new Object2ObjectLinkedOpenHashMap<>();

        protected MeshCatcher() {
            super(new ByteBufferBuilder(0), Object2ObjectSortedMaps.emptyMap());
        }

        @Override
        public @NonNull VertexConsumer getBuffer(@NonNull RenderType renderType) {
            BufferBuilder bufferBuilder = startedBuilders.get(renderType);
            if (bufferBuilder != null) {
                endBatch(renderType, bufferBuilder);
            }
            ByteBufferBuilderPool bufferPool = bufferPools.computeIfAbsent(renderType, type -> new ByteBufferBuilderPool(type.bufferSize()));
            bufferBuilder = new BufferBuilder(bufferPool.getBuffer(), renderType.mode(), renderType.format());
            startedBuilders.put(renderType, bufferBuilder);
            return bufferBuilder;
        }

        public void initialize() {
            for (MeshData meshData : caughtMeshes.keySet()) {
                meshData.close();
            }
            caughtMeshes.clear();
            bufferPools.values().forEach(ByteBufferBuilderPool::release);
        }

        public void endCatching(Consumer<BuiltIterableBuffer> consumer) {
            endBatch();
            for (Map.Entry<MeshData, RenderType> entry : caughtMeshes.entrySet()) {
                MeshData meshData = entry.getKey();
                consumer.accept(BuiltIterableBuffer.buildFrom(entry.getValue(), meshData));
            }
        }

        @Override
        public void endBatch() {
            for (RenderType renderType : bufferPools.keySet()) {
                endBatch(renderType);
            }
        }

        @Override
        protected void endBatch(@NonNull RenderType renderType, BufferBuilder bufferBuilder) {
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
