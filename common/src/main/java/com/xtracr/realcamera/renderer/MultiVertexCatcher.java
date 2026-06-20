package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class MultiVertexCatcher {
    private final SubmitNodeStorage storage = new SubmitNodeStorage();
    private final ByteBufferBuilder stagingBuffer = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private final List<CaughtMesh> caughtMeshes = new ArrayList<>();
    private final Consumer<SubmitNode>[] handlers;
    @Nullable
    private BufferBuilder currentBuilder;
    @Nullable
    private RenderType currentType;

    @SuppressWarnings("unchecked")
    private MultiVertexCatcher() {
        int[] featureIds = {ModelFeatureRenderer.TYPE.id(), CustomFeatureRenderer.TYPE.id()};
        handlers = new Consumer[Arrays.stream(featureIds).max().orElseThrow() + 1];
        handlers[ModelFeatureRenderer.TYPE.id()] = this::captureModel;
        handlers[CustomFeatureRenderer.TYPE.id()] = this::captureCustom;
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
        discard();
        storage.getSubmitsPerOrder().clear();
        return storage;
    }

    public void forEachBuffer(Consumer<BuiltIterableBuffer> consumer) {
        if (!storage.getSubmitsPerOrder().isEmpty()) {
            storage.drainPhases(phase -> phase.sortInto((submitNode, _) -> renderSubmit(submitNode)));
            finishCurrentBuilder();
        }
        for (CaughtMesh entry : caughtMeshes) {
            consumer.accept(BuiltIterableBuffer.buildFrom(entry.renderType, entry.meshData));
        }
    }

    private void renderSubmit(SubmitNode submitNode) {
        int id = submitNode.featureType().id();
        if (id < handlers.length) {
            Consumer<SubmitNode> handler = handlers[id];
            if (handler != null) handler.accept(submitNode);
        }
    }

    private void captureModel(SubmitNode submitNode) {
        ModelFeatureRenderer.Submit<?> submit = (ModelFeatureRenderer.Submit<?>) submitNode;
        VertexConsumer buffer = prepareBuffer(submit.renderType());
        renderModelSubmit(submit, buffer);
    }

    private void captureCustom(SubmitNode submitNode) {
        CustomFeatureRenderer.Submit submit = (CustomFeatureRenderer.Submit) submitNode;
        VertexConsumer buffer = prepareBuffer(submit.renderType());
        submit.customGeometryRenderer().render(submit.pose(), buffer);
    }

    private VertexConsumer prepareBuffer(RenderType renderType) {
        if (currentBuilder != null && currentType == renderType && renderType.canConsolidateConsecutiveGeometry()) {
            return currentBuilder;
        }
        finishCurrentBuilder();
        currentType = renderType;
        currentBuilder = new BufferBuilder(stagingBuffer, renderType.primitiveTopology(), renderType.format());
        return currentBuilder;
    }

    private void finishCurrentBuilder() {
        if (currentBuilder != null) {
            MeshData mesh = currentBuilder.build();
            if (mesh != null) caughtMeshes.add(new CaughtMesh(currentType, mesh));
            currentBuilder = null;
            currentType = null;
        }
    }

    private void discard() {
        finishCurrentBuilder();
        for (CaughtMesh entry : caughtMeshes) entry.meshData.close();
        caughtMeshes.clear();
        stagingBuffer.discard();
    }

    private record CaughtMesh(RenderType renderType, MeshData meshData) {
    }
}
