package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public record VertexData(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
    public static void renderVertices(VertexData[] vertices, VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix) {
        for (VertexData vertex : vertices) vertex.transform(positionMatrix, normalMatrix).render(buffer);
    }

    public static void renderVertices(VertexData[] vertices, VertexConsumer buffer) {
        for (VertexData vertex : vertices) vertex.render(buffer);
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }

    public Vec3 normal() {
        return new Vec3(normalX, normalY, normalZ).normalize();
    }

    public VertexData transform(Matrix4f positionMatrix, Matrix3f normalMatrix) {
        Vector3f pos = new Vector3f(x, y, z).mulPosition(positionMatrix);
        Vector3f normal = new Vector3f(normalX, normalY, normalZ).mul(normalMatrix);
        return new VertexData(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z());
    }

    public void render(VertexConsumer buffer) {
        buffer.vertex(x, y, z,
                (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                u, v, overlay, light, normalX, normalY, normalZ);
    }
}
