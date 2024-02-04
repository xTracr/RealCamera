package com.xtracr.realcamera.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexRecorder implements VertexConsumerProvider {
    protected final List<BuiltRecord> records = new ArrayList<>();
    protected BuiltRecord currentRecord;
    private VertexRecord lastRecord;

    public int quadCount() {
        if (currentRecord == null) return 0;
        return currentRecord.quadCount;
    }

    public String currentTextureId() {
        if (currentRecord == null) return null;
        return getTextureId(currentRecord);
    }

    public Vec3d getCenter(int index) {
        if (currentRecord == null) return null;
        Vec3d center = Vec3d.ZERO;
        for (Vertex vertex : currentRecord.vertices[index]) center = center.add(vertex.pos());
        return center.multiply(1 / (double) currentRecord.additionalVertexCount);
    }

    public Vec3d getNormal(int index) {
        if (currentRecord == null) return null;
        return currentRecord.vertices[index][0].normal();
    }

    public void buildLastRecord() {
        if (lastRecord == null) return;
        records.add(lastRecord.build());
        lastRecord = null;
    }

    public void setCurrent(Predicate<RenderLayer> predicate, int index) {
        currentRecord = records.stream().filter(record -> predicate.test(record.renderLayer))
                .sorted(Comparator.comparingInt(record -> -record.quadCount())).toList().get(index);
    }

    public void drawByAnother(VertexConsumerProvider anotherProvider, Predicate<RenderLayer> layerPredicate, VertexPredicate vertexPredicate) {
        drawByAnother(vertex -> vertex, anotherProvider, layerPredicate, vertexPredicate);
    }

    public void drawByAnother(Function<Vertex, Vertex> function, VertexConsumerProvider anotherProvider, Predicate<RenderLayer> layerPredicate, VertexPredicate vertexPredicate) {
        records.forEach(record -> {
            RenderLayer renderLayer = record.renderLayer;
            if (!layerPredicate.test(renderLayer)) return;
            Vertex[][] vertices = record.vertices;
            VertexConsumer buffer = anotherProvider.getBuffer(renderLayer);
            int quadCount = record.quadCount, vertexCount = record.additionalVertexCount, argb;
            for (int i = 0; i < quadCount; i++) {
                Vertex[] quad = new Vertex[vertexCount];
                for (int j = 0; j < vertexCount; j++) quad[j] = function.apply(vertices[i][j]);
                if (vertexPredicate.test(renderLayer, quad, i)) for (Vertex vertex : quad) {
                    argb = vertex.argb;
                    buffer.vertex((float) vertex.x, (float) vertex.y, (float) vertex.z,
                            (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                            vertex.u, vertex.v, vertex.overlay, vertex.light, vertex.normalX, vertex.normalY, vertex.normalZ);
                }
            }
        });
    }

    protected static String getTextureId(BuiltRecord record) {
        String name = record.renderLayer.toString();
        Pattern pattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        buildLastRecord();
        return lastRecord = new VertexRecord(layer);
    }

    private static class VertexRecord implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();
        private final RenderLayer renderLayer;
        private Vec3d pos = Vec3d.ZERO, normal = Vec3d.ZERO;
        private int argb, overlay, light;
        private float u, v;

        VertexRecord(RenderLayer renderLayer) {
            this.renderLayer = renderLayer;
        }

        private BuiltRecord build() {
            int additionalVertexCount = renderLayer.getDrawMode().additionalVertexCount;
            int quadCount = vertices.size() / additionalVertexCount;
            Vertex[][] quads = new Vertex[quadCount][additionalVertexCount];
            for (int i = 0; i < quadCount; i++) {
                int index = i * additionalVertexCount;
                for (int j = 0; j < additionalVertexCount; j++) {
                    quads[i][j] = vertices.get(index + j);
                }
            }
            return new BuiltRecord(renderLayer, quads, quadCount, additionalVertexCount);
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
            vertices.add(new Vertex(pos.getX(), pos.getY(), pos.getZ(), argb, u, v, overlay, light,
                    (float) normal.getX(), (float) normal.getY(), (float) normal.getZ()));
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

    public record Vertex(double x, double y, double z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        public Vec3d pos() {
            return new Vec3d(x, y, z);
        }

        public Vec3d normal() {
            return new Vec3d(normalX, normalY, normalZ);
        }

        public Vertex transform(Matrix4f positionMatrix, Matrix3f normalMatrix) {
            Vector4f pos = new Vector4f((float) x, (float) y, (float) z, 1.0f).mul(positionMatrix);
            Vector3f normal = new Vector3f(normalX, normalY, normalZ).mul(normalMatrix);
            return new Vertex(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z());
        }
    }

    public interface VertexPredicate {
        boolean test(RenderLayer renderLayer, Vertex[] vertices, int index);
    }
}
