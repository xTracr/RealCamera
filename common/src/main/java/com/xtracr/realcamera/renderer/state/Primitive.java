package com.xtracr.realcamera.renderer.state;

import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Buffer iterators may reuse the primitive and its vertices; call asImmutable() before retaining it.
 * Quad queries test triangles (0, 1, 2), then (0, 2, 3).
 */
public sealed interface Primitive extends Iterable<VertexData> permits MutablePrimitive, Primitive.Line, Primitive.Point, Primitive.Quad, Primitive.Triangle {
    static Primitive immutablePoint(VertexData point) {
        return new Point(point);
    }

    static Primitive immutableLine(VertexData a, VertexData b) {
        return new Line(a, b);
    }

    static Primitive immutableTriangle(VertexData a, VertexData b, VertexData c) {
        return new Triangle(a, b, c);
    }

    static Primitive immutableQuad(VertexData a, VertexData b, VertexData c, VertexData d) {
        return new Quad(a, b, c, d);
    }

    static MutablePrimitive mutablePoint() {
        return new MutablePrimitive.Point();
    }

    static MutablePrimitive mutableLine() {
        return new MutablePrimitive.Line();
    }

    static MutablePrimitive mutableTriangle() {
        return new MutablePrimitive.Triangle();
    }

    static MutablePrimitive mutableQuad() {
        return new MutablePrimitive.Quad();
    }

    static MutablePrimitive mutable(int vertexCount) {
        return switch (vertexCount) {
            case 1 -> mutablePoint();
            case 2 -> mutableLine();
            case 3 -> mutableTriangle();
            case 4 -> mutableQuad();
            default -> throw new IllegalArgumentException("Unsupported primitive vertex count: " + vertexCount);
        };
    }

    int vertexCount();

    default boolean isFace() {
        return false;
    }

    VertexData vertex(int index);

    Primitive asImmutable();

    /**
     * Returns an immutable primitive, copying each mapper result before the next call.
     */
    Primitive mapVertices(UnaryOperator<VertexData> mapper);

    /**
     * Points, lines and degenerate UV triangles never match.
     */
    default boolean containsUV(float u, float v) {
        return false;
    }

    default boolean containsXY(float x, float y) {
        return false;
    }

    /**
     * Unmatched or degenerate UVs return a non-finite position.
     * Points and lines do not support surface interpolation.
     */
    default Vec3 positionAtUV(float u, float v) {
        throw new UnsupportedOperationException("Primitive has no surface: " + getClass().getSimpleName());
    }

    /**
     * Winding normal of the first three vertices, or zero when degenerate.
     */
    default Vec3 faceNormal() {
        throw new UnsupportedOperationException("Primitive has no face normal: " + getClass().getSimpleName());
    }

    @Override
    default @NonNull Iterator<VertexData> iterator() {
        return new Iterator<>() {
            private int nextIndex;

            @Override
            public boolean hasNext() {
                return nextIndex < vertexCount();
            }

            @Override
            public @NonNull VertexData next() {
                if (!hasNext()) throw new NoSuchElementException();
                return vertex(nextIndex++);
            }
        };
    }

    final class Point implements Primitive {
        private final VertexData point;

        Point(VertexData point) {
            this.point = point.asImmutable();
        }

        @Override
        public int vertexCount() {
            return 1;
        }

        @Override
        public VertexData vertex(int index) {
            Objects.checkIndex(index, 1);
            return point;
        }

        @Override
        public Point asImmutable() {
            return this;
        }

        @Override
        public Point mapVertices(UnaryOperator<VertexData> mapper) {
            return new Point(mapper.apply(point).asImmutable());
        }
    }

    final class Line implements Primitive {
        private final VertexData a, b;

        Line(VertexData a, VertexData b) {
            this.a = a.asImmutable();
            this.b = b.asImmutable();
        }

        @Override
        public int vertexCount() {
            return 2;
        }

        @Override
        public VertexData vertex(int index) {
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        public Line asImmutable() {
            return this;
        }

        @Override
        public Line mapVertices(UnaryOperator<VertexData> mapper) {
            return new Line(mapper.apply(a).asImmutable(), mapper.apply(b).asImmutable());
        }
    }

    final class Triangle implements Primitive {
        private final VertexData a, b, c;

        Triangle(VertexData a, VertexData b, VertexData c) {
            this.a = a.asImmutable();
            this.b = b.asImmutable();
            this.c = c.asImmutable();
        }

        static boolean containsUV(VertexData a, VertexData b, VertexData c, float u, float v) {
            return MathUtil.pointInTriangle(u, v, a.u(), a.v(), b.u(), b.v(), c.u(), c.v());
        }

        static boolean containsXY(VertexData a, VertexData b, VertexData c, float x, float y) {
            return MathUtil.pointInTriangle(x, y, a.x(), a.y(), b.x(), b.y(), c.x(), c.y());
        }

        static Vec3 positionAtUV(VertexData a, VertexData b, VertexData c, float u, float v) {
            return containsUV(a, b, c, u, v) ? interpolateUV(a, b, c, u, v) : new Vec3(Double.NaN, Double.NaN, Double.NaN);
        }

        static Vec3 interpolateUV(VertexData a, VertexData b, VertexData c, float u, float v) {
            double abU = (double) b.u() - a.u(), abV = (double) b.v() - a.v();
            double acU = (double) c.u() - a.u(), acV = (double) c.v() - a.v();
            double apU = (double) u - a.u(), apV = (double) v - a.v();
            double determinant = abU * acV - abV * acU;
            double beta = (apU * acV - apV * acU) / determinant;
            double gamma = (abU * apV - abV * apU) / determinant;
            return a.position().scale(1 - beta - gamma).add(b.position().scale(beta)).add(c.position().scale(gamma));
        }

        static Vec3 faceNormal(VertexData a, VertexData b, VertexData c) {
            Vec3 origin = a.position();
            return b.position().subtract(origin).cross(c.position().subtract(origin)).normalize();
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
        public VertexData vertex(int index) {
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        public Triangle asImmutable() {
            return this;
        }

        @Override
        public Triangle mapVertices(UnaryOperator<VertexData> mapper) {
            return new Triangle(mapper.apply(a).asImmutable(), mapper.apply(b).asImmutable(), mapper.apply(c).asImmutable());
        }

        @Override
        public boolean containsUV(float u, float v) {
            return containsUV(a, b, c, u, v);
        }

        @Override
        public boolean containsXY(float x, float y) {
            return containsXY(a, b, c, x, y);
        }

        @Override
        public Vec3 positionAtUV(float u, float v) {
            return positionAtUV(a, b, c, u, v);
        }

        @Override
        public Vec3 faceNormal() {
            return faceNormal(a, b, c);
        }
    }

    final class Quad implements Primitive {
        private final VertexData a, b, c, d;

        Quad(VertexData a, VertexData b, VertexData c, VertexData d) {
            this.a = a.asImmutable();
            this.b = b.asImmutable();
            this.c = c.asImmutable();
            this.d = d.asImmutable();
        }

        static boolean containsUV(VertexData a, VertexData b, VertexData c, VertexData d, float u, float v) {
            return Triangle.containsUV(a, b, c, u, v) || Triangle.containsUV(a, c, d, u, v);
        }

        static boolean containsXY(VertexData a, VertexData b, VertexData c, VertexData d, float x, float y) {
            return Triangle.containsXY(a, b, c, x, y) || Triangle.containsXY(a, c, d, x, y);
        }

        static Vec3 positionAtUV(VertexData a, VertexData b, VertexData c, VertexData d, float u, float v) {
            if (Triangle.containsUV(a, b, c, u, v)) return Triangle.interpolateUV(a, b, c, u, v);
            if (Triangle.containsUV(a, c, d, u, v)) return Triangle.interpolateUV(a, c, d, u, v);
            return new Vec3(Double.NaN, Double.NaN, Double.NaN);
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
        public VertexData vertex(int index) {
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        public Quad asImmutable() {
            return this;
        }

        @Override
        public Quad mapVertices(UnaryOperator<VertexData> mapper) {
            return new Quad(mapper.apply(a).asImmutable(), mapper.apply(b).asImmutable(), mapper.apply(c).asImmutable(), mapper.apply(d).asImmutable());
        }

        @Override
        public boolean containsUV(float u, float v) {
            return containsUV(a, b, c, d, u, v);
        }

        @Override
        public boolean containsXY(float x, float y) {
            return containsXY(a, b, c, d, x, y);
        }

        @Override
        public Vec3 positionAtUV(float u, float v) {
            return positionAtUV(a, b, c, d, u, v);
        }

        @Override
        public Vec3 faceNormal() {
            return Triangle.faceNormal(a, b, c);
        }
    }
}
