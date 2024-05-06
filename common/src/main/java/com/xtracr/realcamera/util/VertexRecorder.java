package com.xtracr.realcamera.util;

import com.xtracr.realcamera.config.BindingTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexRecorder implements VertexConsumerProvider {
    protected final List<BuiltRecord> records = new ArrayList<>();
    private final Stack<VertexRecord> recordStack = new Stack<>();

    protected static Vec3d getPos(Vertex[] quad, float u, float v) {
        if (quad.length < 3) return quad[0].pos();
        float u0 = quad[0].u, v0 = quad[0].v, u1 = quad[1].u, v1 = quad[1].v, u2 = quad[2].u, v2 = quad[2].v;
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return quad[0].pos().multiply(alpha).add(quad[1].pos().multiply(beta)).add(quad[2].pos().multiply(1 - alpha - beta));
    }

    protected static Vertex[] getQuad(BuiltRecord record, float u, float v) {
        final int resolution = 1000000;
        return Arrays.stream(record.vertices).filter(quad -> {
            Polygon polygon = new Polygon();
            for (Vertex vertex : quad) polygon.addPoint((int) (resolution * vertex.u), (int) (resolution * vertex.v));
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
            Vec3d forward = getQuad(record, target.forwardU, target.forwardV)[0].normal().normalize();
            Vec3d left = getQuad(record, target.upwardU, target.upwardV)[0].normal().crossProduct(forward).normalize();
            Vertex[] positionQuad = getQuad(record, target.posU, target.posV);
            if (positionQuad[0].normal().equals(Vec3d.ZERO) && forward.equals(Vec3d.ZERO) && left.equals(Vec3d.ZERO)) return null;
            normal.set(left.toVector3f(), forward.crossProduct(left).toVector3f(), forward.toVector3f());
            Vec3d center = getPos(positionQuad, target.posU, target.posV);
            if (!Double.isFinite(center.lengthSquared())) return null;
            position.set((float) target.getOffsetZ(), (float) target.getOffsetY(), (float) target.getOffsetX()).mul(normal).add(center.toVector3f());
            return record;
        }).filter(Objects::nonNull).findAny().orElse(null);
    }

    public void drawByAnother(VertexConsumerProvider anotherProvider, Function<BuiltRecord, Vertex[][]> function) {
        records.forEach(record -> {
            int argb;
            VertexConsumer buffer = anotherProvider.getBuffer(record.renderLayer);
            Vertex[][] vertices = function.apply(record);
            for (Vertex[] quad : vertices) for (Vertex vertex : quad) {
                argb = vertex.argb;
                buffer.vertex((float) vertex.x, (float) vertex.y, (float) vertex.z,
                        (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                        vertex.u, vertex.v, vertex.overlay, vertex.light, vertex.normalX, vertex.normalY, vertex.normalZ);
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
            String layerName = renderLayer.toString();
            Matcher matcher = Pattern.compile("texture\\[Optional\\[(.*?)]").matcher(layerName);
            return new BuiltRecord(renderLayer, matcher.find() ? matcher.group(1) : layerName, quads, quadCount, additionalVertexCount);
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

    public record BuiltRecord(RenderLayer renderLayer, String textureId, Vertex[][] vertices, int quadCount, int additionalVertexCount) { }

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
    }
}
