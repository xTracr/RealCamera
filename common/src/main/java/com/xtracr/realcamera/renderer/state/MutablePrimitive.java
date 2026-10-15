package com.xtracr.realcamera.renderer.state;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Reusable decoding storage. Copy retained primitives or vertices before the next update.
 */
public sealed interface MutablePrimitive extends Primitive {
    @Override
    MutableVertex vertex(int index);

    final class Point implements MutablePrimitive {
        private final MutableVertex point = VertexData.mutable();

        Point() {
        }

        @Override
        public int vertexCount() {
            return 1;
        }

        @Override
        public MutableVertex vertex(int index) {
            Objects.checkIndex(index, 1);
            return point;
        }

        @Override
        public Primitive.Point asImmutable() {
            return new Primitive.Point(point);
        }

        @Override
        public Primitive.Point mapVertices(UnaryOperator<VertexData> mapper) {
            return new Primitive.Point(mapper.apply(point).asImmutable());
        }
    }

    final class Line implements MutablePrimitive {
        private final MutableVertex a = VertexData.mutable(), b = VertexData.mutable();

        Line() {
        }

        @Override
        public int vertexCount() {
            return 2;
        }

        @Override
        public MutableVertex vertex(int index) {
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        public Primitive.Line asImmutable() {
            return new Primitive.Line(a, b);
        }

        @Override
        public Primitive.Line mapVertices(UnaryOperator<VertexData> mapper) {
            return new Primitive.Line(mapper.apply(a).asImmutable(), mapper.apply(b).asImmutable());
        }
    }

    final class Triangle implements MutablePrimitive {
        private final MutableVertex a = VertexData.mutable(), b = VertexData.mutable(), c = VertexData.mutable();

        Triangle() {
        }

        @Override
        public int vertexCount() {
            return 3;
        }

        @Override
        public boolean isFace() {
            return true;
        }

        @Override
        public MutableVertex vertex(int index) {
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        public Primitive.Triangle asImmutable() {
            return new Primitive.Triangle(a, b, c);
        }

        @Override
        public Primitive.Triangle mapVertices(UnaryOperator<VertexData> mapper) {
            return new Primitive.Triangle(mapper.apply(a).asImmutable(), mapper.apply(b).asImmutable(), mapper.apply(c).asImmutable());
        }

        @Override
        public boolean containsUV(float u, float v) {
            return Primitive.Triangle.containsUV(a, b, c, u, v);
        }

        @Override
        public boolean containsXY(float x, float y) {
            return Primitive.Triangle.containsXY(a, b, c, x, y);
        }

        @Override
        public Vec3 positionAtUV(float u, float v) {
            return Primitive.Triangle.positionAtUV(a, b, c, u, v);
        }

        @Override
        public Vec3 faceNormal() {
            return Primitive.Triangle.faceNormal(a, b, c);
        }
    }

    final class Quad implements MutablePrimitive {
        private final MutableVertex a = VertexData.mutable(), b = VertexData.mutable(), c = VertexData.mutable(), d = VertexData.mutable();

        Quad() {
        }

        @Override
        public int vertexCount() {
            return 4;
        }

        @Override
        public boolean isFace() {
            return true;
        }

        @Override
        public MutableVertex vertex(int index) {
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        public Primitive.Quad asImmutable() {
            return new Primitive.Quad(a, b, c, d);
        }

        @Override
        public Primitive.Quad mapVertices(UnaryOperator<VertexData> mapper) {
            return new Primitive.Quad(mapper.apply(a).asImmutable(), mapper.apply(b).asImmutable(), mapper.apply(c).asImmutable(), mapper.apply(d).asImmutable());
        }

        @Override
        public boolean containsUV(float u, float v) {
            return Primitive.Quad.containsUV(a, b, c, d, u, v);
        }

        @Override
        public boolean containsXY(float x, float y) {
            return Primitive.Quad.containsXY(a, b, c, d, x, y);
        }

        @Override
        public Vec3 positionAtUV(float u, float v) {
            return Primitive.Quad.positionAtUV(a, b, c, d, u, v);
        }

        @Override
        public Vec3 faceNormal() {
            return Primitive.Triangle.faceNormal(a, b, c);
        }
    }
}
