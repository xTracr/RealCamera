package com.xtracr.realcamera.renderer.state;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface VertexData {
    static VertexData immutable(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        return new ImmutableVertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
    }

    static MutableVertex mutable() {
        return new MutableVertex();
    }

    static VertexData[] asImmutable(VertexData[] vertices) {
        VertexData[] immutable = new VertexData[vertices.length];
        for (int i = 0; i < vertices.length; i++) immutable[i] = vertices[i].asImmutable();
        return immutable;
    }

    static Vec3 position(VertexData[] vertices, float u, float v) {
        if (vertices.length < 3) return vertices[0].position();
        float u0 = vertices[0].u(), v0 = vertices[0].v(), u1 = vertices[1].u(), v1 = vertices[1].v(), u2 = vertices[2].u(), v2 = vertices[2].v();
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return vertices[0].position().scale(alpha).add(vertices[1].position().scale(beta)).add(vertices[2].position().scale(1 - alpha - beta));
    }

    static Vec3 normal(VertexData[] vertices) {
        return switch (vertices.length) {
            case 0 -> Vec3.ZERO;
            case 1 -> vertices[0].normal();
            case 2 -> vertices[1].position().subtract(vertices[0].position()).normalize();
            default -> {
                Vec3 a = vertices[0].position(), b = vertices[1].position(), c = vertices[2].position();
                yield b.subtract(a).cross(c.subtract(a)).normalize();
            }
        };
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

    default void render(VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix) {
        Vector3f pos = new Vector3f(x(), y(), z()).mulPosition(positionMatrix);
        Vector3f normal = new Vector3f(normalX(), normalY(), normalZ()).mul(normalMatrix);
        buffer.addVertex(pos.x(), pos.y(), pos.z(), argb(), u(), v(), overlay(), light(), normal.x(), normal.y(), normal.z());
    }

    default void render(VertexConsumer buffer) {
        buffer.addVertex(x(), y(), z(), argb(), u(), v(), overlay(), light(), normalX(), normalY(), normalZ());
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

    record ImmutableVertex(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) implements VertexData { }

    class MutableVertex implements VertexData {
        public float x, y, z;
        public int argb;
        public float u, v;
        public int overlay, light;
        public float normalX, normalY, normalZ;

        private MutableVertex() { }

        @Override
        public float x() {
            return x;
        }

        @Override
        public float y() {
            return y;
        }

        @Override
        public float z() {
            return z;
        }

        @Override
        public int argb() {
            return argb;
        }

        @Override
        public float u() {
            return u;
        }

        @Override
        public float v() {
            return v;
        }

        @Override
        public int overlay() {
            return overlay;
        }

        @Override
        public int light() {
            return light;
        }

        @Override
        public float normalX() {
            return normalX;
        }

        @Override
        public float normalY() {
            return normalY;
        }

        @Override
        public float normalZ() {
            return normalZ;
        }
    }
}
