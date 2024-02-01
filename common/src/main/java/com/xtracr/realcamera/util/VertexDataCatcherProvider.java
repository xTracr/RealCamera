package com.xtracr.realcamera.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class VertexDataCatcherProvider implements VertexConsumerProvider {
    protected final Map<RenderLayer, List<VertexDataCatcher>> catchers = new LinkedHashMap<>();

    public VertexDataCatcher getUnion(VertexDataCatcher union) {
        catchers.values().forEach(catcherList -> catcherList.forEach(catcher -> union.vertices.addAll(catcher.vertices)));
        return union;
    }

    public void drawByAnother(MatrixStack matrixStack, VertexConsumerProvider anotherProvider, @Nullable Predicate<RenderLayer> layerPredicate, @Nullable VertexPredicate vertexPredicate) {
        catchers.forEach(((renderLayer, catcherList) -> catcherList.forEach(catcher -> {
            if (layerPredicate != null && !layerPredicate.test(renderLayer)) return;
            VertexConsumer buffer = anotherProvider.getBuffer(renderLayer);
            Consumer<VertexDataCatcher.Vertex> vertexConsumer = vertex -> {
                Vector4f pos = new Vector4f(vertex.pos().toVector3f(), 1.0f).mul(matrixStack.peek().getPositionMatrix());
                Vector3f normal = vertex.normal().toVector3f().mul(matrixStack.peek().getNormalMatrix());
                int argb = vertex.argb();
                buffer.vertex(pos.x(), pos.y(), pos.z(),
                        (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                        vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), normal.x(), normal.y(), normal.z());
            };
            int size = catcher.vertexCount();
            if (vertexPredicate == null) catcher.vertices.forEach(vertexConsumer);
            else for (int i = 0; i < size; i++) {
                VertexDataCatcher.Vertex vertex = catcher.vertices.get(i);
                if (vertexPredicate.test(renderLayer, vertex, i)) vertexConsumer.accept(vertex);
            }
        })));
    }

    public void drawByAnother(VertexConsumerProvider anotherProvider, @Nullable Predicate<RenderLayer> layerPredicate, @Nullable VertexPredicate vertexPredicate) {
        catchers.forEach(((renderLayer, catcherList) -> catcherList.forEach(catcher -> {
            if (layerPredicate != null && !layerPredicate.test(renderLayer)) return;
            VertexConsumer buffer = anotherProvider.getBuffer(renderLayer);
            Consumer<VertexDataCatcher.Vertex> vertexConsumer = vertex -> {
                Vec3d pos = vertex.pos(), normal = vertex.normal();
                int argb = vertex.argb();
                buffer.vertex((float) pos.getX(), (float) pos.getY(), (float) pos.getZ(),
                        (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                        vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
            };
            int size = catcher.vertexCount();
            if (vertexPredicate == null) catcher.vertices.forEach(vertexConsumer);
            else for (int i = 0; i < size; i++) {
                VertexDataCatcher.Vertex vertex = catcher.vertices.get(i);
                if (vertexPredicate.test(renderLayer, vertex, i)) vertexConsumer.accept(vertex);
            }
        })));
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        VertexDataCatcher catcher = new VertexDataCatcher();
        List<VertexDataCatcher> catcherList = catchers.getOrDefault(layer, new ArrayList<>());
        catcherList.add(catcher);
        catchers.putIfAbsent(layer, catcherList);
        return catcher;
    }

    public interface VertexPredicate {
        boolean test(RenderLayer renderLayer, VertexDataCatcher.Vertex vertex, int index);
    }
}
