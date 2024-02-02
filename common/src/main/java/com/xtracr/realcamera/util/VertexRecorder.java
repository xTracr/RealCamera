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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class VertexRecorder implements VertexConsumerProvider {
    protected final List<BuiltRecord> records = new ArrayList<>();
    protected BuiltRecord currentRecord;
    private VertexRecord lastRecord;

    public int quadCount() {
        if (currentRecord == null) return 0;
        return currentRecord.quadCount;
    }

    public String currentRenderLayerName() {
        if (currentRecord == null) return null;
        return currentRecord.renderLayer.toString();
    }

    public Vec3d getCenter(int index) {
        if (currentRecord == null) return null;
        Vec3d center = Vec3d.ZERO;
        for (Vertex vertex : currentRecord.vertices[index]) center = center.add(vertex.pos);
        return center.multiply(1 / (double) currentRecord.additionalVertexCount);
    }

    public Vec3d getNormal(int index) {
        if (currentRecord == null) return null;
        return currentRecord.vertices[index][0].normal;
    }

    public void buildLastRecord() {
        if (lastRecord == null) return;
        records.add(lastRecord.build());
        lastRecord = null;
    }

    public void setCurrent(Predicate<RenderLayer> predicate, int index) {
        int i = 0;
        for (BuiltRecord record : records) {
            if (!predicate.test(record.renderLayer)) continue;
            if (i < index) {
                i++;
                continue;
            }
            currentRecord = record;
            return;
        }
    }

    public void drawByAnother(MatrixStack matrixStack, VertexConsumerProvider anotherProvider, @Nullable Predicate<RenderLayer> layerPredicate, @Nullable VertexPredicate vertexPredicate) {
        records.forEach(record -> {
            RenderLayer renderLayer = record.renderLayer;
            Vertex[][] vertices = record.vertices;
            if (layerPredicate != null && !layerPredicate.test(renderLayer)) return;
            VertexConsumer buffer = anotherProvider.getBuffer(renderLayer);
            Consumer<Vertex> vertexConsumer = vertex -> {
                Vector4f pos = new Vector4f(vertex.pos().toVector3f(), 1.0f).mul(matrixStack.peek().getPositionMatrix());
                Vector3f normal = vertex.normal().toVector3f().mul(matrixStack.peek().getNormalMatrix());
                int argb = vertex.argb();
                buffer.vertex(pos.x(), pos.y(), pos.z(),
                        (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                        vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), normal.x(), normal.y(), normal.z());
            };
            int size = record.quadCount;
            if (vertexPredicate == null) for (Vertex[] vertexList: vertices) {
                for (Vertex vertex : vertexList) vertexConsumer.accept(vertex);
            } else for (int i = 0; i < size; i++) {
                Vertex[] vertexList = vertices[i];
                if (vertexPredicate.test(renderLayer, vertexList, i)) for (Vertex vertex : vertexList) {
                    vertexConsumer.accept(vertex);
                }
            }
        });
    }

    public void drawByAnother(VertexConsumerProvider anotherProvider, @Nullable Predicate<RenderLayer> layerPredicate, @Nullable VertexPredicate vertexPredicate) {
        records.forEach(record -> {
            RenderLayer renderLayer = record.renderLayer;
            Vertex[][] vertices = record.vertices;
            if (layerPredicate != null && !layerPredicate.test(renderLayer)) return;
            VertexConsumer buffer = anotherProvider.getBuffer(renderLayer);
            Consumer<Vertex> vertexConsumer = vertex -> {
                Vec3d pos = vertex.pos(), normal = vertex.normal();
                int argb = vertex.argb();
                buffer.vertex((float) pos.getX(), (float) pos.getY(), (float) pos.getZ(),
                        (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                        vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
            };
            int size = record.quadCount;
            if (vertexPredicate == null) for (Vertex[] vertexList: vertices) {
                for (Vertex vertex : vertexList) vertexConsumer.accept(vertex);
            } else for (int i = 0; i < size; i++) {
                Vertex[] vertexList = vertices[i];
                if (vertexPredicate.test(renderLayer, vertexList, i)) for (Vertex vertex : vertexList) {
                    vertexConsumer.accept(vertex);
                }
            }
        });
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        buildLastRecord();
        return lastRecord = new VertexRecord(layer);
    }

    private static class VertexRecord implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();
        private final RenderLayer renderLayer;
        private final int additionalVertexCount;
        private Vec3d pos = Vec3d.ZERO, normal = Vec3d.ZERO;
        private int argb, overlay, light;
        private float u, v;

        VertexRecord(RenderLayer renderLayer) {
            this.renderLayer = renderLayer;
            this.additionalVertexCount = renderLayer.getDrawMode().additionalVertexCount;
        }

        private BuiltRecord build() {
            int size = vertices.size() / additionalVertexCount;
            Vertex[][] list = new Vertex[size][additionalVertexCount];
            for (int i = 0; i < size; i++) {
                int index = i * additionalVertexCount;
                for (int j = 0; j < additionalVertexCount; j++) {
                    list[i][j] = vertices.get(index + j);
                }
            }
            return new BuiltRecord(renderLayer, list, size, additionalVertexCount);
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            pos = new Vec3d(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            argb = alpha << 24 | red << 16 | green << 8 | blue;
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            overlay = (short) u | (short) v << 16;
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            light = (short) u | (short) v << 16;
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            normal = new Vec3d(x, y, z);
            return this;
        }

        @Override
        public void next() {
            vertices.add(new Vertex(pos, argb, u, v, overlay, light, normal));
            pos = normal = Vec3d.ZERO;
            u = v = overlay = light = argb = 0;
        }

        @Override
        public void fixedColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unfixColor() {
        }
    }

    protected record BuiltRecord(RenderLayer renderLayer, Vertex[][] vertices, int quadCount, int additionalVertexCount) {}

    public record Vertex(Vec3d pos, int argb, float u, float v, int overlay, int light, Vec3d normal) {}

    public interface VertexPredicate {
        boolean test(RenderLayer renderLayer, Vertex[] vertices, int index);
    }
}
