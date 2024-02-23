package com.xtracr.realcamera.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
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

    public Vec3d getPos(float u, float v) {
        if (currentRecord == null) return null;
        return getPos(getQuadIndex(currentRecord, u, v), u, v);
    }

    public Vec3d getPos(int quadIndex, float u, float v) {
        if (currentRecord == null || quadIndex >= currentRecord.quadCount) return null;
        Vertex[] quad = currentRecord.vertices[quadIndex];
        if (quad.length < 3) return null;
        float u0 = quad[0].u, v0 = quad[0].v, u1 = quad[1].u, v1 = quad[1].v, u2 = quad[2].u, v2 = quad[2].v;
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return quad[0].pos().multiply(alpha).add(quad[1].pos().multiply(beta)).add(quad[2].pos().multiply(1 - alpha - beta));
    }

    public Vec3d getNormal(float u, float v) {
        if (currentRecord == null) return null;
        Vertex[] quad = getQuad(currentRecord, u, v);
        if (quad == null || quad.length < 1) return null;
        return quad[0].normal();
    }

    public void buildLastRecord() {
        if (lastRecord != null && !lastRecord.vertices.isEmpty()) records.add(lastRecord.build());
        lastRecord = null;
    }

    public void setCurrent(Predicate<RenderLayer> predicate) {
        currentRecord = records.stream().filter(record -> predicate.test(record.renderLayer)).max(Comparator.comparingInt(BuiltRecord::quadCount)).orElse(null);
    }

    public void drawByAnother(VertexConsumerProvider anotherProvider, Predicate<RenderLayer> layerPredicate, BiPredicate<RenderLayer, Vertex[]> biPredicate) {
        drawByAnother(vertex -> vertex, anotherProvider, layerPredicate, biPredicate);
    }

    public void drawByAnother(Function<Vertex, Vertex> function, VertexConsumerProvider anotherProvider, Predicate<RenderLayer> layerPredicate, BiPredicate<RenderLayer, Vertex[]> biPredicate) {
        records.forEach(record -> {
            RenderLayer renderLayer = record.renderLayer;
            if (!layerPredicate.test(renderLayer)) return;
            int argb, length;
            Vertex[] newQuad;
            VertexConsumer buffer = anotherProvider.getBuffer(renderLayer);
            for (Vertex[] quad : record.vertices) {
                length = quad.length;
                newQuad = new Vertex[length];
                for (int i = 0; i < length; i++) newQuad[i] = function.apply(quad[i]);
                if (biPredicate.test(renderLayer, newQuad)) for (Vertex vertex : newQuad) {
                    argb = vertex.argb;
                    buffer.vertex((float) vertex.x, (float) vertex.y, (float) vertex.z,
                            (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                            vertex.u, vertex.v, vertex.overlay, vertex.light, vertex.normalX, vertex.normalY, vertex.normalZ);
                }
            }
        });
    }

    protected static Vertex[] getQuad(BuiltRecord record, float u, float v) {
        int i = getQuadIndex(record, u, v);
        if (i >= 0) return record.vertices[i];
        return null;
    }

    protected static int getQuadIndex(BuiltRecord record, float u, float v) {
        final int resolution = 1000000;
        for (int i = 0, size = record.vertices.length; i < size; i++) {
            Vertex[] quad = record.vertices[i];
            Polygon polygon = new Polygon();
            for (Vertex vertex : quad) polygon.addPoint((int) (resolution * vertex.u), (int) (resolution * vertex.v));
            if (polygon.contains(resolution * u, resolution * v)) return i;
        }
        return -1;
    }

    protected static String getTextureId(BuiltRecord record) {
        String name = record.renderLayer.toString();
        Pattern pattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) return matcher.group(1);
        return name;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
        if (lastRecord == null || !Objects.equals(lastRecord.renderLayer, renderLayer) || !renderLayer.areVerticesNotShared()) {
            buildLastRecord();
            lastRecord = new VertexRecord(renderLayer);
        }
        return lastRecord;
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
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
            int argb = (int) (alpha * 255.0f) << 24 | (int) (red * 255.0f) << 16 | (int) (green * 255.0f) << 8 | (int) (blue * 255.0f);
            vertices.add(new Vertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ));
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
}
