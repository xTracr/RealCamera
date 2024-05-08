package com.xtracr.realcamera.util;

import com.xtracr.realcamera.config.BindingTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexRecorder implements VertexConsumerProvider {
    protected final List<BuiltRecord> records = new ArrayList<>();
    private final Stack<VertexRecord> recordStack = new Stack<>();

    protected static Vec3d getPos(Vertex[] primitive, float u, float v) {
        if (primitive.length < 3) return primitive[0].pos();
        float u0 = primitive[0].u, v0 = primitive[0].v, u1 = primitive[1].u, v1 = primitive[1].v, u2 = primitive[2].u, v2 = primitive[2].v;
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return primitive[0].pos().multiply(alpha).add(primitive[1].pos().multiply(beta)).add(primitive[2].pos().multiply(1 - alpha - beta));
    }

    protected static Vertex[] getPrimitive(BuiltRecord record, float u, float v) {
        final int resolution = 1000000;
        return Arrays.stream(record.primitives).filter(primitive -> {
            Polygon polygon = new Polygon();
            for (Vertex vertex : primitive) polygon.addPoint((int) (resolution * vertex.u), (int) (resolution * vertex.v));
            return polygon.contains(resolution * u, resolution * v);
        }).findAny().orElse(new Vertex[]{new Vertex(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)});
    }

    public void clear() {
        records.clear();
        recordStack.clear();
    }

    public void buildRecords() {
        recordStack.forEach(record -> records.add(record.build()));
        recordStack.clear();
    }

    public BuiltRecord getTargetPosAndRot(BindingTarget target, Matrix3f normal, Vector3f position) {
        return records.stream().map(record -> {
            if (!record.textureId().contains(target.textureId)) return null;
            Vec3d forward = getPrimitive(record, target.forwardU, target.forwardV)[0].normal().normalize();
            Vec3d left = getPrimitive(record, target.upwardU, target.upwardV)[0].normal().crossProduct(forward).normalize();
            Vertex[] face = getPrimitive(record, target.posU, target.posV);
            if (face[0].normal().equals(Vec3d.ZERO) && forward.equals(Vec3d.ZERO) && left.equals(Vec3d.ZERO)) return null;
            normal.set(left.toVector3f(), forward.crossProduct(left).toVector3f(), forward.toVector3f());
            Vec3d center = getPos(face, target.posU, target.posV);
            if (!Double.isFinite(center.lengthSquared())) return null;
            position.set((float) target.getOffsetZ(), (float) target.getOffsetY(), (float) target.getOffsetX()).mul(normal).add(center.toVector3f());
            return record;
        }).filter(Objects::nonNull).findAny().orElse(null);
    }

    public void drawByAnother(VertexConsumerProvider anotherProvider, Predicate<BuiltRecord> predicate, Function<BuiltRecord, Vertex[][]> function) {
        records.forEach(record -> {
            if (!predicate.test(record)) return;
            VertexConsumer buffer = anotherProvider.getBuffer(record.renderLayer);
            if (record.distinct) {
                Vertex[][] primitives = function.apply(record);
                for (Vertex[] primitive : primitives) for (Vertex vertex : primitive) vertex.apply(buffer);
            } else {
                for (Vertex vertex : record.vertices) vertex.apply(buffer);
            }
        });
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
        if (recordStack.isEmpty() || !Objects.equals(recordStack.peek().renderLayer, renderLayer) || !renderLayer.areVerticesNotShared()) {
            return recordStack.push(new VertexRecord(renderLayer));
        }
        return recordStack.peek();
    }

    private static class VertexRecord implements VertexConsumer {
        private static final Pattern textureIdPattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
        private final List<Vertex> vertices = new ArrayList<>();
        private final RenderLayer renderLayer;
        private Vec3d pos = Vec3d.ZERO, normal = Vec3d.ZERO;
        private int argb, overlay, light;
        private float u, v;

        VertexRecord(RenderLayer renderLayer) {
            this.renderLayer = renderLayer;
        }

        private BuiltRecord build() {
            String layerName = renderLayer.toString();
            Matcher matcher = textureIdPattern.matcher(layerName);
            String textureId = matcher.find() ? matcher.group(1) : layerName;
            Vertex[] vertices = this.vertices.toArray(Vertex[]::new);
            VertexFormat.DrawMode drawMode = renderLayer.getDrawMode();
            final int primitiveLength = drawMode.firstVertexCount, primitiveStride = drawMode.additionalVertexCount;
            final int primitiveCount = (vertices.length - primitiveLength) / primitiveStride + 1;
            final boolean startWithFirst = drawMode == VertexFormat.DrawMode.TRIANGLE_FAN;
            Vertex[][] primitives = new Vertex[primitiveCount][primitiveLength];
            for (int i = 0, k = 0; i < primitiveCount; i++, k += primitiveStride) {
                primitives[i][0] = vertices[startWithFirst ? 0 : k];
                System.arraycopy(vertices, k + 1, primitives[i], 1, primitiveLength - 1);
            }
            return new BuiltRecord(renderLayer, textureId, vertices, primitives, primitiveLength, primitiveLength == primitiveStride);
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
            vertices.add(new Vertex(pos.getX(), pos.getY(), pos.getZ(), argb, u, v, overlay, light, (float) normal.getX(), (float) normal.getY(), (float) normal.getZ()));
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

    public record BuiltRecord(RenderLayer renderLayer, String textureId, Vertex[] vertices, Vertex[][] primitives, int primitiveLength, boolean distinct) {}

    public record Vertex(double x, double y, double z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        public Vec3d pos() {
            return new Vec3d(x, y, z);
        }

        public Vec3d normal() {
            return new Vec3d(normalX, normalY, normalZ);
        }

        public Vertex transform(Matrix4f positionMatrix, Matrix3f normalMatrix) {
            Vector3f pos = new Vector3f((float) x, (float) y, (float) z).mulPosition(positionMatrix);
            Vector3f normal = new Vector3f(normalX, normalY, normalZ).mul(normalMatrix);
            return new Vertex(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z());
        }

        public void apply(VertexConsumer buffer) {
            buffer.vertex((float) x, (float) y, (float) z,
                    (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                    u, v, overlay, light, normalX, normalY, normalZ);
        }
    }
}
