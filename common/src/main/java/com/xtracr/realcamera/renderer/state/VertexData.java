package com.xtracr.realcamera.renderer.state;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public interface VertexData {
    static VertexData immutable(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        return new ImmutableVertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
    }

    static MutableVertex mutable() {
        return new MutableVertex();
    }

    float x();

    float y();

    float z();

    default Vec3 position() {
        return new Vec3(x(), y(), z());
    }

    int argb();

    float u();

    float v();

    default UV uv() {
        return new UV(u(), v());
    }

    int overlay();

    int light();

    float normalX();

    float normalY();

    float normalZ();

    default Vec3 normal() {
        return new Vec3(normalX(), normalY(), normalZ());
    }

    default VertexData asImmutable() {
        return new ImmutableVertex(x(), y(), z(), argb(), u(), v(), overlay(), light(), normalX(), normalY(), normalZ());
    }

    default void render(VertexConsumer buffer, Matrix4fc positionMatrix, Matrix3fc normalMatrix) {
        Vector3f position = new Vector3f(x(), y(), z()).mulPosition(positionMatrix);
        Vector3f normal = transformedNormal(normalMatrix);
        buffer.addVertex(position.x, position.y, position.z, argb(), u(), v(), overlay(), light(), normal.x, normal.y, normal.z);
    }

    default void render(VertexConsumer buffer) {
        buffer.addVertex(x(), y(), z(), argb(), u(), v(), overlay(), light(), normalX(), normalY(), normalZ());
    }

    private Vector3f transformedNormal(Matrix3fc normalMatrix) {
        Vector3f normal = new Vector3f(normalX(), normalY(), normalZ()).mul(normalMatrix);
        return normal.lengthSquared() > 0 ? normal.normalize() : normal;
    }

    record UV(float u, float v) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UV(float u1, float v1))) return false;
            return Float.compare(u, u1) == 0 && Float.compare(v, v1) == 0;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(u) * 31 + Float.hashCode(v);
        }
    }

    record ImmutableVertex(float x, float y, float z, int argb, float u, float v, int overlay, int light,
                           float normalX, float normalY, float normalZ) implements VertexData {
        @Override
        public VertexData asImmutable() {
            return this;
        }
    }
}
