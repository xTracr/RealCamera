package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public record VertexData(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
    public static void renderVertices(VertexData[] vertices, VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix) {
        for (VertexData vertex : vertices) vertex.render(buffer, positionMatrix, normalMatrix);
    }

    public static void renderVertices(VertexData[] vertices, VertexConsumer buffer) {
        for (VertexData vertex : vertices) vertex.render(buffer);
    }

    public static Vec3 normal(VertexData[] polygon) {
        return switch (polygon.length) {
            case 0 -> Vec3.ZERO;
            case 1 -> polygon[0].normal();
            case 2 -> polygon[1].pos().subtract(polygon[0].pos()).normalize();
            default -> {
                Vec3 a = polygon[0].pos(), b = polygon[1].pos(), c = polygon[2].pos();
                yield b.subtract(a).cross(c.subtract(a)).normalize();
            }
        };
    }

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
