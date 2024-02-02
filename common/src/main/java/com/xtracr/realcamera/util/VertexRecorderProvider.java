package com.xtracr.realcamera.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class VertexRecorderProvider implements VertexConsumerProvider {
    protected final List<Pair<RenderLayer, VertexRecorder>> recorders = new ArrayList<>();

    public VertexRecorder getUnion(VertexRecorder union) {
        recorders.forEach(pair -> union.vertices.addAll(pair.getRight().vertices));
        return union;
    }

    public void drawByAnother(MatrixStack matrixStack, VertexConsumerProvider anotherProvider, @Nullable Predicate<RenderLayer> layerPredicate, @Nullable VertexPredicate vertexPredicate) {
        recorders.forEach(pair -> {
            RenderLayer renderLayer = pair.getLeft();
            VertexRecorder recorder = pair.getRight();
            if (layerPredicate != null && !layerPredicate.test(renderLayer)) return;
            VertexConsumer buffer = anotherProvider.getBuffer(renderLayer);
            Consumer<VertexRecorder.Vertex> vertexConsumer = vertex -> {
                Vector4f pos = new Vector4f(vertex.pos().toVector3f(), 1.0f).mul(matrixStack.peek().getPositionMatrix());
                Vector3f normal = vertex.normal().toVector3f().mul(matrixStack.peek().getNormalMatrix());
                int argb = vertex.argb();
                buffer.vertex(pos.x(), pos.y(), pos.z(),
                        (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                        vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), normal.x(), normal.y(), normal.z());
            };
            int size = recorder.vertexCount();
            if (vertexPredicate == null) recorder.vertices.forEach(vertexConsumer);
            else for (int i = 0; i < size; i++) {
                VertexRecorder.Vertex vertex = recorder.vertices.get(i);
                if (vertexPredicate.test(renderLayer, vertex, i)) vertexConsumer.accept(vertex);
            }
        });
    }

    public void drawByAnother(VertexConsumerProvider anotherProvider, @Nullable Predicate<RenderLayer> layerPredicate, @Nullable VertexPredicate vertexPredicate) {
        recorders.forEach(pair -> {
            RenderLayer renderLayer = pair.getLeft();
            VertexRecorder recorder = pair.getRight();
            if (layerPredicate != null && !layerPredicate.test(renderLayer)) return;
            VertexConsumer buffer = anotherProvider.getBuffer(renderLayer);
            Consumer<VertexRecorder.Vertex> vertexConsumer = vertex -> {
                Vec3d pos = vertex.pos(), normal = vertex.normal();
                int argb = vertex.argb();
                buffer.vertex((float) pos.getX(), (float) pos.getY(), (float) pos.getZ(),
                        (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                        vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
            };
            int size = recorder.vertexCount();
            if (vertexPredicate == null) recorder.vertices.forEach(vertexConsumer);
            else for (int i = 0; i < size; i++) {
                VertexRecorder.Vertex vertex = recorder.vertices.get(i);
                if (vertexPredicate.test(renderLayer, vertex, i)) vertexConsumer.accept(vertex);
            }
        });
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        VertexRecorder recorder = new VertexRecorder();
        recorders.add(new Pair<>(layer, recorder));
        return recorder;
    }

    public interface VertexPredicate {
        boolean test(RenderLayer renderLayer, VertexRecorder.Vertex vertex, int index);
    }
}
