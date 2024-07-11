package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.config.BindingTarget;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexRecorder implements MultiBufferSource {
    protected final List<BuiltRecord> records = new ArrayList<>();
    private final Stack<VertexRecord> recordStack = new Stack<>();

    public static void renderVertices(Vertex[] vertices, VertexConsumer buffer) {
        for (Vertex vertex : vertices) vertex.render(buffer);
    }

    protected static Vec3 getPos(Vertex[] primitive, float u, float v) {
        if (primitive.length < 3) return primitive[0].pos();
        float u0 = primitive[0].u, v0 = primitive[0].v, u1 = primitive[1].u, v1 = primitive[1].v, u2 = primitive[2].u, v2 = primitive[2].v;
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return primitive[0].pos().scale(alpha).add(primitive[1].pos().scale(beta)).add(primitive[2].pos().scale(1 - alpha - beta));
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
            Vec3 forward = getPrimitive(record, target.forwardU, target.forwardV)[0].normal();
            Vec3 left = getPrimitive(record, target.upwardU, target.upwardV)[0].normal().cross(forward);
            Vertex[] face = getPrimitive(record, target.posU, target.posV);
            if (face[0].normal().equals(Vec3.ZERO) && forward.equals(Vec3.ZERO) && left.equals(Vec3.ZERO)) return null;
            normal.set(left.toVector3f(), forward.cross(left).toVector3f(), forward.toVector3f());
            Vec3 center = getPos(face, target.posU, target.posV);
            if (!Double.isFinite(center.lengthSqr())) return null;
            position.set((float) target.getOffsetZ(), (float) target.getOffsetY(), (float) target.getOffsetX()).mul(normal).add(center.toVector3f());
            return record;
        }).filter(Objects::nonNull).findAny().orElse(null);
    }

    public void forEachRecord(Consumer<BuiltRecord> consumer) {
        records.forEach(consumer);
    }

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
        if (recordStack.isEmpty() || !Objects.equals(recordStack.peek().renderType, renderType) || !renderType.canConsolidateConsecutiveGeometry()) {
            return recordStack.push(new VertexRecord(renderType));
        }
        return recordStack.peek();
    }

    private static class VertexRecord implements VertexConsumer {
        private static final Pattern textureIdPattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
        private final List<Vertex> vertices = new ArrayList<>();
        private final RenderType renderType;
        private final Vector3f pos = new Vector3f(), normal = new Vector3f();
        private int argb, overlay, light;
        private float u, v;

        VertexRecord(RenderType renderType) {
            this.renderType = renderType;
        }

        private BuiltRecord build() {
            String layerName = renderType.toString();
            Matcher matcher = textureIdPattern.matcher(layerName);
            String textureId = matcher.find() ? matcher.group(1) : layerName;
            Vertex[] vertices = this.vertices.toArray(Vertex[]::new);
            VertexFormat.Mode drawMode = renderType.mode();
            final int primitiveLength = drawMode.primitiveLength, primitiveStride = drawMode.primitiveStride;
            final int primitiveCount = (vertices.length - primitiveLength) / primitiveStride + 1;
            final boolean startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
            Vertex[][] primitives = new Vertex[primitiveCount][primitiveLength];
            for (int i = 0, k = 0; i < primitiveCount; i++, k += primitiveStride) {
                primitives[i][0] = vertices[startWithFirst ? 0 : k];
                System.arraycopy(vertices, k + 1, primitives[i], 1, primitiveLength - 1);
            }
            return new BuiltRecord(renderType, textureId, vertices, primitives);
        }

        @Override
        public @NotNull VertexConsumer vertex(double x, double y, double z) {
            pos.set(x, y, z);
            return this;
        }

        @Override
        public @NotNull VertexConsumer color(int red, int green, int blue, int alpha) {
            argb = alpha << 24 | red << 16 | green << 8 | blue;
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public @NotNull VertexConsumer overlayCoords(int u, int v) {
            overlay = (short) u | (short) v << 16;
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv2(int u, int v) {
            light = (short) u | (short) v << 16;
            return this;
        }

        @Override
        public @NotNull VertexConsumer normal(float x, float y, float z) {
            normal.set(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            vertices.add(new Vertex(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z()));
            pos.set(normal.set(0));
            u = v = overlay = light = argb = 0;
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
            int argb = (int) (alpha * 255.0f) << 24 | (int) (red * 255.0f) << 16 | (int) (green * 255.0f) << 8 | (int) (blue * 255.0f);
            vertices.add(new Vertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ));
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }

        @Override
        public @NotNull VertexConsumer color(int argb) {
            this.argb = argb;
            return this;
        }

        @Override
        public @NotNull VertexConsumer overlayCoords(int overlay) {
            this.overlay = overlay;
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv2(int light) {
            this.light = light;
            return this;
        }
    }

    public record BuiltRecord(RenderType renderType, String textureId, Vertex[] vertices, Vertex[][] primitives) {}

    public record Vertex(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        public Vec3 pos() {
            return new Vec3(x, y, z);
        }

        public Vec3 normal() {
            return new Vec3(normalX, normalY, normalZ).normalize();
        }

        public Vertex transform(Matrix4f positionMatrix, Matrix3f normalMatrix) {
            Vector3f pos = new Vector3f(x, y, z).mulPosition(positionMatrix);
            Vector3f normal = new Vector3f(normalX, normalY, normalZ).mul(normalMatrix);
            return new Vertex(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z());
        }

        public void render(VertexConsumer buffer) {
            buffer.vertex(x, y, z,
                    (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                    u, v, overlay, light, normalX, normalY, normalZ);
        }
    }
}
