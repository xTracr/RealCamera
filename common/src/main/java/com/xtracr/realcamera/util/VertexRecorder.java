package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.config.BindingTarget;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexRecorder {
    protected static final Pattern textureIdPattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
    private final List<BuiltRecord> records = new ArrayList<>();

    protected static Vec3 getCenter(Vertex[] primitive, float u, float v) {
        if (primitive.length < 3) return primitive[0].pos();
        float u0 = primitive[0].u, v0 = primitive[0].v, u1 = primitive[1].u, v1 = primitive[1].v, u2 = primitive[2].u, v2 = primitive[2].v;
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return primitive[0].pos().scale(alpha).add(primitive[1].pos().scale(beta)).add(primitive[2].pos().scale(1 - alpha - beta));
    }

    protected static BuiltRecord buildVertices(Vertex[] vertices, RenderType renderType) {
        String renderTypeName = renderType.toString();
        Matcher matcher = textureIdPattern.matcher(renderTypeName);
        String textureId = matcher.find() ? matcher.group(1) : renderTypeName;
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

    public static void renderVertices(Vertex[] vertices, VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix) {
        for (Vertex vertex : vertices) vertex.render(buffer, positionMatrix, normalMatrix);
    }

    public static void renderVertices(Vertex[] vertices, VertexConsumer buffer) {
        for (Vertex vertex : vertices) vertex.render(buffer);
    }

    public List<BuiltRecord> records() {
        return records;
    }

    public void recordVertices(Vertex[] vertices, RenderType renderType) {
        records.add(buildVertices(vertices, renderType));
    }

    public record BuiltRecord(RenderType renderType, String textureId, Vertex[] vertices, Vertex[][] primitives) {
        public Vertex[] findPrimitive(float u, float v) {
            final int resolution = 1000000;
            for (Vertex[] primitive : primitives) {
                Polygon polygon = new Polygon();
                for (Vertex vertex : primitive) polygon.addPoint((int) (resolution * vertex.u), (int) (resolution * vertex.v));
                if (polygon.contains(resolution * u, resolution * v)) return primitive;
            }
            return new Vertex[]{new Vertex(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)};
        }

        public BindingContext genContext(BindingTarget target, boolean mirrored) {
            if (!textureId.contains(target.textureId)) return BindingContext.EMPTY;
            BindingContext context = new BindingContext(target, mirrored);
            Vertex[] face = findPrimitive(target.getPosU(), target.getPosV());
            context.setPosition(getCenter(face, target.getPosU(), target.getPosV()));
            Vec3 forward = findPrimitive(target.getForwardU(), target.getForwardV())[0].normal();
            Vec3 upward = findPrimitive(target.getUpwardU(), target.getUpwardV())[0].normal();
            context.setDirections(forward, upward);
            return context;
        }
    }

    public record Vertex(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        public Vec3 pos() {
            return new Vec3(x, y, z);
        }

        public Vec3 normal() {
            return new Vec3(normalX, normalY, normalZ).normalize();
        }

        public void render(VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix) {
            Vector3f pos = new Vector3f(x, y, z).mulPosition(positionMatrix);
            Vector3f normal = new Vector3f(normalX, normalY, normalZ).mul(normalMatrix);
            buffer.addVertex(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z());
        }

        public void render(VertexConsumer buffer) {
            buffer.addVertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
        }
    }
}
