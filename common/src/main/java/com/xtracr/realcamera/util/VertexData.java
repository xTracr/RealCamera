package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public record VertexData(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
    private static final ThreadLocal<org.joml.Vector3f> TL_POS = ThreadLocal.withInitial(org.joml.Vector3f::new);
    private static final ThreadLocal<org.joml.Vector3f> TL_NRM = ThreadLocal.withInitial(org.joml.Vector3f::new);

    public static void renderVertices(VertexData[] vertices, VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix) {
        org.joml.Vector3f pos = TL_POS.get();
        org.joml.Vector3f normal = TL_NRM.get();
        for (int i = 0; i < vertices.length; i++) {
            VertexData vertex = vertices[i];
            pos.set(vertex.x, vertex.y, vertex.z).mulPosition(positionMatrix);
            normal.set(vertex.normalX, vertex.normalY, vertex.normalZ).mul(normalMatrix);
            buffer.addVertex(pos.x(), pos.y(), pos.z(), vertex.argb, vertex.u, vertex.v, vertex.overlay, vertex.light, normal.x(), normal.y(), normal.z());
        }
    }

    public static void renderVertices(VertexData[] vertices, VertexConsumer buffer) {
        for (VertexData vertex : vertices) {
            buffer.addVertex(vertex.x, vertex.y, vertex.z, vertex.argb, vertex.u, vertex.v, vertex.overlay, vertex.light, vertex.normalX, vertex.normalY, vertex.normalZ);
        }
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }

    public Vec3 normal() {
        return new Vec3(normalX, normalY, normalZ).normalize();
    }

    public void render(VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix) {
        // Retained for compatibility, but bulk renderer above is preferred for performance
        Vector3f pos = new Vector3f(x, y, z).mulPosition(positionMatrix);
        Vector3f normal = new Vector3f(normalX, normalY, normalZ).mul(normalMatrix);
        buffer.addVertex(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z());
    }

    public void render(VertexConsumer buffer) {
        buffer.addVertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
    }

}
