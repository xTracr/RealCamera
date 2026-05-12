package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.renderer.state.VertexData.MutableVertex;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class IterableVertexBuffer implements Iterable<VertexData> {
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final float NORMAL_SCALE = 1.0f / 127.0f;
    public final int vertexSize, vertexCount, primitiveLength, primitiveStride, primitiveCount;
    private final Iterable<VertexData[]> primitives;
    private final ByteBuffer buffer;
    private final int positionOffset, colorOffset, uvOffset, overlayOffset, lightOffset, normalOffset;
    private final boolean hasPosition, hasColor, hasUV, hasOverlay, hasLight, hasNormal, fullFormat;
    private final boolean startWithFirst;

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
        primitiveCount = (vertexCount - primitiveLength) / primitiveStride + 1;
        startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
        boolean isQuad = drawMode == VertexFormat.Mode.QUADS;
        primitives = fullFormat && isQuad ? new FastQuadReader() : new PrimitiveReader();
    }

    public Iterable<VertexData[]> primitives() {
        return primitives;
    }

    public Stream<VertexData> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<VertexData[]> primitiveStream() {
        return StreamSupport.stream(primitives.spliterator(), false);
    }

    public VertexData readVertexAt(int index) {
        Objects.checkIndex(index, vertexCount);
        return readVertexAt(index, VertexData.mutable());
    }

    public VertexData[] readPrimitiveAt(int primitiveIndex) {
        Objects.checkIndex(primitiveIndex, primitiveCount);
        MutableVertex[] mutableVertices = new MutableVertex[primitiveLength];
        for (int i = 0; i < primitiveLength; i++) mutableVertices[i] = VertexData.mutable();
        return readPrimitiveAt(primitiveIndex, mutableVertices);
    }

    @Override
    public @NonNull Iterator<VertexData> iterator() {
        return new VertexIterator();
    }

    @Override
    public Spliterator<VertexData> spliterator() {
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

    private MutableVertex[] readPrimitiveAt(int index, MutableVertex[] mutableVertices) {
        int vertexIndex = index * primitiveStride;
        for (int i = startWithFirst ? 1 : 0; i < primitiveLength; i++) {
            readVertexAt(vertexIndex + i, mutableVertices[i]);
        }
        return mutableVertices;
    }

    private final class VertexIterator implements Iterator<VertexData> {
        private final MutableVertex reusableVertex = VertexData.mutable();
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            return currentIndex < vertexCount;
        }

        @Override
        public @NonNull VertexData next() {
            return readVertexAt(currentIndex++, reusableVertex);
        }
    }

    private final class VertexSpliterator implements Spliterator<VertexData> {
        private final MutableVertex reusableVertex = VertexData.mutable();
        private final int endIndex;
        private int currentIndex;

        public VertexSpliterator(int start, int end) {
            currentIndex = start;
            endIndex = end;
        }

        @Override
        public boolean tryAdvance(Consumer<? super VertexData> action) {
            if (currentIndex < endIndex) {
                action.accept(readVertexAt(currentIndex, reusableVertex));
                currentIndex++;
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<VertexData> trySplit() {
            int remaining = endIndex - currentIndex;
            if (remaining <= 1) {
                return null;
            }
            int splitPos = currentIndex + remaining / 2;
            VertexSpliterator newSpliterator = new VertexSpliterator(currentIndex, splitPos);
            currentIndex = splitPos;
            return newSpliterator;
        }

        @Override
        public long estimateSize() {
            return endIndex - currentIndex;
        }

        @Override
        public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | NONNULL;
        }
    }

    private final class PrimitiveReader implements Iterable<VertexData[]> {
        @Override
        public @NonNull Iterator<VertexData[]> iterator() {
            return new PrimitiveIterator();
        }

        @Override
        public Spliterator<VertexData[]> spliterator() {
            return new PrimitiveSpliterator(0, primitiveCount);
        }

        private final class PrimitiveIterator implements Iterator<VertexData[]> {
            private final MutableVertex[] reusablePrimitive = new MutableVertex[primitiveLength];
            private int currentIndex = 0;

            public PrimitiveIterator() {
                for (int i = 0; i < primitiveLength; i++) {
                    reusablePrimitive[i] = VertexData.mutable();
                }
                if (0 < vertexCount) readVertexAt(0, reusablePrimitive[0]);
            }

            @Override
            public boolean hasNext() {
                return currentIndex < primitiveCount - 1;
            }

            @Override
            public VertexData[] next() {
                readPrimitiveAt(currentIndex, reusablePrimitive);
                currentIndex++;
                return reusablePrimitive;
            }
        }

        private final class PrimitiveSpliterator implements Spliterator<VertexData[]> {
            private final MutableVertex[] reusablePrimitive = new MutableVertex[primitiveLength];
            private final int endIndex;
            private int currentIndex;

            public PrimitiveSpliterator(int start, int end) {
                currentIndex = start;
                endIndex = end;
                for (int i = 0; i < primitiveLength; i++) {
                    reusablePrimitive[i] = VertexData.mutable();
                }
                if (0 < vertexCount) readVertexAt(0, reusablePrimitive[0]);
            }

            @Override
            public boolean tryAdvance(Consumer<? super VertexData[]> action) {
                if (currentIndex < endIndex) {
                    readPrimitiveAt(currentIndex, reusablePrimitive);
                    action.accept(reusablePrimitive);
                    currentIndex++;
                    return true;
                }
                return false;
            }

            @Override
            public Spliterator<VertexData[]> trySplit() {
                int remaining = endIndex - currentIndex;
                if (remaining <= 1) {
                    return null;
                }
                int splitPos = currentIndex + remaining / 2;
                PrimitiveSpliterator newSpliterator = new PrimitiveSpliterator(currentIndex, splitPos);
                currentIndex = splitPos;
                return newSpliterator;
            }

            @Override
            public long estimateSize() {
                return endIndex - currentIndex;
            }

            @Override
            public int characteristics() {
                return ORDERED | SIZED | SUBSIZED | NONNULL;
            }
        }

    }

    private final class FastQuadReader implements Iterable<VertexData[]> {
        private final int quadCount = vertexCount / 4;

        @Override
        public @NonNull Iterator<VertexData[]> iterator() {
            return new FastQuadIterator();
        }

        @Override
        public Spliterator<VertexData[]> spliterator() {
            return new FastQuadSpliterator(0, quadCount);
        }

        private void fastReadQuadAt(int index, MutableVertex[] quad) {
            int vertexOffset = index * 144;
            for (int i = 0; i < 4; i++, vertexOffset += 36) {
                MutableVertex mutable = quad[i];
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

        private class FastQuadIterator implements Iterator<VertexData[]> {
            private final MutableVertex[] reusableQuad = new MutableVertex[4];
            private int currentIndex = 0;

            public FastQuadIterator() {
                for (int i = 0; i < 4; i++) {
                    reusableQuad[i] = VertexData.mutable();
                }
            }

            @Override
            public boolean hasNext() {
                return currentIndex < quadCount - 1;
            }

            @Override
            public VertexData[] next() {
                fastReadQuadAt(currentIndex, reusableQuad);
                currentIndex++;
                return reusableQuad;
            }
        }

        private final class FastQuadSpliterator implements Spliterator<VertexData[]> {
            private final MutableVertex[] reusableQuad = new MutableVertex[4];
            private final int endIndex;
            private int currentIndex;

            public FastQuadSpliterator(int start, int end) {
                currentIndex = start;
                endIndex = end;
                for (int i = 0; i < 4; i++) {
                    reusableQuad[i] = VertexData.mutable();
                }
            }

            @Override
            public boolean tryAdvance(Consumer<? super VertexData[]> action) {
                if (currentIndex < endIndex) {
                    fastReadQuadAt(currentIndex, reusableQuad);
                    action.accept(reusableQuad);
                    currentIndex++;
                    return true;
                }
                return false;
            }

            @Override
            public Spliterator<VertexData[]> trySplit() {
                int remaining = endIndex - currentIndex;
                if (remaining <= 1) {
                    return null;
                }
                int splitPos = currentIndex + remaining / 2;
                FastQuadSpliterator newSpliterator = new FastQuadSpliterator(currentIndex, splitPos);
                currentIndex = splitPos;
                return newSpliterator;
            }

            @Override
            public long estimateSize() {
                return endIndex - currentIndex;
            }

            @Override
            public int characteristics() {
                return ORDERED | SIZED | SUBSIZED | NONNULL;
            }
        }
    }
}
