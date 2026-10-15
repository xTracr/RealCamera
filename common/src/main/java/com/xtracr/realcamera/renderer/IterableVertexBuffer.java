package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xtracr.realcamera.renderer.state.MutableVertex;
import com.xtracr.realcamera.renderer.state.MutablePrimitive;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

public class IterableVertexBuffer implements VertexGeometry {
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final float NORMAL_SCALE = 1.0f / 127.0f;
    protected final int vertexCount, primitiveCount, primitiveLength, primitiveStride;
    protected final boolean startWithFirst;
    private final Iterable<Primitive> primitives;
    private final ByteBuffer buffer;
    private final int vertexSize, positionOffset, colorOffset, uvOffset, overlayOffset, lightOffset, normalOffset;
    private final boolean hasPosition, hasColor, hasUV, hasOverlay, hasLight, hasNormal, fullFormat;

    public IterableVertexBuffer(MeshData meshData) {
        buffer = meshData.vertexBuffer();
        MeshData.DrawState drawState = meshData.drawState();
        VertexFormat format = drawState.format();
        vertexSize = format.getVertexSize();
        positionOffset = format.getOffset(VertexFormatElement.POSITION);
        colorOffset = format.getOffset(VertexFormatElement.COLOR);
        uvOffset = format.getOffset(VertexFormatElement.UV0);
        overlayOffset = format.getOffset(VertexFormatElement.UV1);
        lightOffset = format.getOffset(VertexFormatElement.UV2);
        normalOffset = format.getOffset(VertexFormatElement.NORMAL);
        hasPosition = positionOffset != -1;
        hasColor = colorOffset != -1;
        hasUV = uvOffset != -1;
        hasOverlay = overlayOffset != -1;
        hasLight = lightOffset != -1;
        hasNormal = normalOffset != -1;
        fullFormat = vertexSize == DefaultVertexFormat.ENTITY.getVertexSize();
        vertexCount = drawState.vertexCount();
        VertexFormat.Mode drawMode = drawState.mode();
        primitiveLength = drawMode.primitiveLength;
        primitiveStride = drawMode.primitiveStride;
        primitiveCount = vertexCount < primitiveLength ? 0 : (vertexCount - primitiveLength) / primitiveStride + 1;
        startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
        boolean isQuad = drawMode == VertexFormat.Mode.QUADS;
        primitives = fullFormat && isQuad ? new FastQuadReader() : new PrimitiveReader();
    }

    @Override
    public final int vertexCount() {
        return vertexCount;
    }

    @Override
    public final int primitiveCount() {
        return primitiveCount;
    }

    @Override
    public final Iterable<Primitive> primitives() {
        return primitives;
    }

    @Override
    public final VertexData readVertexAt(int index) {
        Objects.checkIndex(index, vertexCount);
        return readVertexAt(index, VertexData.mutable()).asImmutable();
    }

    @Override
    public final Primitive readPrimitiveAt(int primitiveIndex) {
        Objects.checkIndex(primitiveIndex, primitiveCount);
        MutablePrimitive primitive = Primitive.mutable(primitiveLength);
        if (startWithFirst) readVertexAt(0, primitive.vertex(0));
        readPrimitiveAt(primitiveIndex, primitive);
        return primitive.asImmutable();
    }

    @Override
    public final @NonNull Iterator<VertexData> iterator() {
        return new VertexIterator();
    }

    @Override
    public final Spliterator<VertexData> spliterator() {
        return new VertexSpliterator(0, vertexCount);
    }

    private int readColor(int offset) {
        // net.minecraft.util.ARGB.fromABGR(abgr)
        int abgr = IS_LITTLE_ENDIAN ? buffer.getInt(offset) : Integer.reverseBytes(buffer.getInt(offset));
        return (abgr & 0xFF00FF00) | (abgr & 0xFF0000) >> 16 | (abgr & 0xFF) << 16;
    }

    private MutableVertex readVertexAt(int index, MutableVertex mutable) {
        int vertexOffset = index * vertexSize;
        if (fullFormat) {
            mutable.x = buffer.getFloat(vertexOffset);
            mutable.y = buffer.getFloat(vertexOffset + 4);
            mutable.z = buffer.getFloat(vertexOffset + 8);
            mutable.argb = readColor(vertexOffset + 12);
            mutable.u = buffer.getFloat(vertexOffset + 16);
            mutable.v = buffer.getFloat(vertexOffset + 20);
            mutable.overlay = buffer.getInt(vertexOffset + 24);
            mutable.light = buffer.getInt(vertexOffset + 28);
            mutable.normalX = buffer.get(vertexOffset + 32) * NORMAL_SCALE;
            mutable.normalY = buffer.get(vertexOffset + 33) * NORMAL_SCALE;
            mutable.normalZ = buffer.get(vertexOffset + 34) * NORMAL_SCALE;
            return mutable;
        }
        if (hasPosition) {
            int offset = vertexOffset + positionOffset;
            mutable.x = buffer.getFloat(offset);
            mutable.y = buffer.getFloat(offset + 4);
            mutable.z = buffer.getFloat(offset + 8);
        }
        if (hasColor) {
            mutable.argb = readColor(vertexOffset + colorOffset);
        }
        if (hasUV) {
            int offset = vertexOffset + uvOffset;
            mutable.u = buffer.getFloat(offset);
            mutable.v = buffer.getFloat(offset + 4);
        }
        if (hasOverlay) {
            mutable.overlay = buffer.getInt(vertexOffset + overlayOffset);
        }
        if (hasLight) {
            mutable.light = buffer.getInt(vertexOffset + lightOffset);
        }
        if (hasNormal) {
            int offset = vertexOffset + normalOffset;
            mutable.normalX = buffer.get(offset) * NORMAL_SCALE;
            mutable.normalY = buffer.get(offset + 1) * NORMAL_SCALE;
            mutable.normalZ = buffer.get(offset + 2) * NORMAL_SCALE;
        }
        return mutable;
    }

    // If startWithFirst, callers must initialize vertex 0; only the other vertices are updated here.
    private void readPrimitiveAt(int index, MutablePrimitive primitive) {
        int vertexIndex = index * primitiveStride;
        for (int i = startWithFirst ? 1 : 0; i < primitiveLength; i++) {
            readVertexAt(vertexIndex + i, primitive.vertex(i));
        }
    }

    private final class VertexIterator implements Iterator<VertexData> {
        private final MutableVertex reusableVertex = VertexData.mutable();
        private int nextIndex = 0;

        @Override
        public boolean hasNext() {
            return nextIndex < vertexCount;
        }

        @Override
        public @NonNull VertexData next() {
            if (!hasNext()) throw new NoSuchElementException();
            return readVertexAt(nextIndex++, reusableVertex);
        }
    }

    private final class VertexSpliterator implements Spliterator<VertexData> {
        private final MutableVertex reusableVertex = VertexData.mutable();
        private final int endIndex;
        private int nextIndex;

        public VertexSpliterator(int start, int end) {
            nextIndex = start;
            endIndex = end;
        }

        @Override
        public boolean tryAdvance(Consumer<? super VertexData> action) {
            if (nextIndex < endIndex) {
                action.accept(readVertexAt(nextIndex, reusableVertex));
                nextIndex++;
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<VertexData> trySplit() {
            int remaining = endIndex - nextIndex;
            if (remaining <= 1) {
                return null;
            }
            int splitPos = nextIndex + remaining / 2;
            VertexSpliterator newSpliterator = new VertexSpliterator(nextIndex, splitPos);
            nextIndex = splitPos;
            return newSpliterator;
        }

        @Override
        public long estimateSize() {
            return endIndex - nextIndex;
        }

        @Override
        public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | NONNULL;
        }
    }

    private final class PrimitiveReader implements Iterable<Primitive> {
        @Override
        public @NonNull Iterator<Primitive> iterator() {
            return new PrimitiveIterator();
        }

        @Override
        public Spliterator<Primitive> spliterator() {
            return new PrimitiveSpliterator(0, primitiveCount);
        }

        private final class PrimitiveIterator implements Iterator<Primitive> {
            private final MutablePrimitive reusablePrimitive = Primitive.mutable(primitiveLength);
            private int nextIndex = 0;

            public PrimitiveIterator() {
                if (0 < vertexCount) readVertexAt(0, reusablePrimitive.vertex(0));
            }

            @Override
            public boolean hasNext() {
                return nextIndex < primitiveCount;
            }

            @Override
            public Primitive next() {
                if (!hasNext()) throw new NoSuchElementException();
                readPrimitiveAt(nextIndex, reusablePrimitive);
                nextIndex++;
                return reusablePrimitive;
            }
        }

        private final class PrimitiveSpliterator implements Spliterator<Primitive> {
            private final MutablePrimitive reusablePrimitive = Primitive.mutable(primitiveLength);
            private final int endIndex;
            private int nextIndex;

            public PrimitiveSpliterator(int start, int end) {
                nextIndex = start;
                endIndex = end;
                if (0 < vertexCount) readVertexAt(0, reusablePrimitive.vertex(0));
            }

            @Override
            public boolean tryAdvance(Consumer<? super Primitive> action) {
                if (nextIndex < endIndex) {
                    readPrimitiveAt(nextIndex, reusablePrimitive);
                    action.accept(reusablePrimitive);
                    nextIndex++;
                    return true;
                }
                return false;
            }

            @Override
            public Spliterator<Primitive> trySplit() {
                int remaining = endIndex - nextIndex;
                if (remaining <= 1) {
                    return null;
                }
                int splitPos = nextIndex + remaining / 2;
                PrimitiveSpliterator newSpliterator = new PrimitiveSpliterator(nextIndex, splitPos);
                nextIndex = splitPos;
                return newSpliterator;
            }

            @Override
            public long estimateSize() {
                return endIndex - nextIndex;
            }

            @Override
            public int characteristics() {
                return ORDERED | SIZED | SUBSIZED | NONNULL;
            }
        }
    }

    private final class FastQuadReader implements Iterable<Primitive> {
        private final int quadCount = vertexCount / 4;

        @Override
        public @NonNull Iterator<Primitive> iterator() {
            return new FastQuadIterator();
        }

        @Override
        public Spliterator<Primitive> spliterator() {
            return new FastQuadSpliterator(0, quadCount);
        }

        private void fastReadQuadAt(int index, MutablePrimitive quad) {
            int vertexOffset = index * 144;
            for (int i = 0; i < 4; i++, vertexOffset += 36) {
                MutableVertex mutable = quad.vertex(i);
                mutable.x = buffer.getFloat(vertexOffset);
                mutable.y = buffer.getFloat(vertexOffset + 4);
                mutable.z = buffer.getFloat(vertexOffset + 8);
                mutable.argb = readColor(vertexOffset + 12);
                mutable.u = buffer.getFloat(vertexOffset + 16);
                mutable.v = buffer.getFloat(vertexOffset + 20);
                mutable.overlay = buffer.getInt(vertexOffset + 24);
                mutable.light = buffer.getInt(vertexOffset + 28);
                mutable.normalX = buffer.get(vertexOffset + 32) * NORMAL_SCALE;
                mutable.normalY = buffer.get(vertexOffset + 33) * NORMAL_SCALE;
                mutable.normalZ = buffer.get(vertexOffset + 34) * NORMAL_SCALE;
            }
        }

        private class FastQuadIterator implements Iterator<Primitive> {
            private final MutablePrimitive reusableQuad = Primitive.mutableQuad();
            private int nextIndex = 0;

            @Override
            public boolean hasNext() {
                return nextIndex < quadCount;
            }

            @Override
            public Primitive next() {
                if (!hasNext()) throw new NoSuchElementException();
                fastReadQuadAt(nextIndex, reusableQuad);
                nextIndex++;
                return reusableQuad;
            }
        }

        private final class FastQuadSpliterator implements Spliterator<Primitive> {
            private final MutablePrimitive reusableQuad = Primitive.mutableQuad();
            private final int endIndex;
            private int nextIndex;

            public FastQuadSpliterator(int start, int end) {
                nextIndex = start;
                endIndex = end;
            }

            @Override
            public boolean tryAdvance(Consumer<? super Primitive> action) {
                if (nextIndex < endIndex) {
                    fastReadQuadAt(nextIndex, reusableQuad);
                    action.accept(reusableQuad);
                    nextIndex++;
                    return true;
                }
                return false;
            }

            @Override
            public Spliterator<Primitive> trySplit() {
                int remaining = endIndex - nextIndex;
                if (remaining <= 1) {
                    return null;
                }
                int splitPos = nextIndex + remaining / 2;
                FastQuadSpliterator newSpliterator = new FastQuadSpliterator(nextIndex, splitPos);
                nextIndex = splitPos;
                return newSpliterator;
            }

            @Override
            public long estimateSize() {
                return endIndex - nextIndex;
            }

            @Override
            public int characteristics() {
                return ORDERED | SIZED | SUBSIZED | NONNULL;
            }
        }
    }
}
