package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xtracr.realcamera.util.VertexData.MutableVertex;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IterableVertexBuffer implements Iterable<VertexData> {
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private final MutableVertex reusableVertex = VertexData.mutable();
    private final Iterable<VertexData[]> primitives;
    private final ByteBuffer buffer;
    private final int vertexCount, vertexSize;
    private final int positionOffset, colorOffset, uvOffset, overlayOffset, lightOffset, normalOffset;
    private final boolean hasPosition, hasColor, hasUV, hasOverlay, hasLight, hasNormal, fastFormat;

    public IterableVertexBuffer(MeshData meshData) {
        buffer = meshData.vertexBuffer();
        MeshData.DrawState drawState = meshData.drawState();
        VertexFormat format = drawState.format();
        vertexCount = drawState.vertexCount();
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
        fastFormat = format == DefaultVertexFormat.NEW_ENTITY;
        boolean isQuad = drawState.mode() == VertexFormat.Mode.QUADS;
        primitives = fastFormat && isQuad ? new FastQuadReader() : new PrimitiveReader(drawState.mode());
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
        return readVertexAt(index, reusableVertex);
    }

    public MutableVertex readVertexAt(int index, MutableVertex mutable) {
        Objects.checkIndex(index, vertexCount);
        int vertexOffset = index * vertexSize;
        if (fastFormat) {
            mutable.x = buffer.getFloat(vertexOffset);
            mutable.y = buffer.getFloat(vertexOffset + 4);
            mutable.z = buffer.getFloat(vertexOffset + 8);
            mutable.argb = readColor(vertexOffset + 12);
            mutable.u = buffer.getFloat(vertexOffset + 16);
            mutable.v = buffer.getFloat(vertexOffset + 20);
            mutable.overlay = buffer.getInt(vertexOffset + 24);
            mutable.light = buffer.getInt(vertexOffset + 28);
            mutable.normalX = buffer.get(vertexOffset + 32) / 127.0f;
            mutable.normalY = buffer.get(vertexOffset + 33) / 127.0f;
            mutable.normalZ = buffer.get(vertexOffset + 34) / 127.0f;
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
            mutable.normalX = buffer.get(offset) / 127.0f;
            mutable.normalY = buffer.get(offset + 1) / 127.0f;
            mutable.normalZ = buffer.get(offset + 2) / 127.0f;
        }
        return mutable;
    }

    @Override
    public @NotNull Iterator<VertexData> iterator() {
        return new VertexIterator();
    }

    @Override
    public Spliterator<VertexData> spliterator() {
        return new VertexSpliterator(0, vertexCount);
    }

    private int readColor(int offset) {
        // ABGR2ARGB is the same as ARGB2ABGR
        return FastColor.ABGR32.fromArgb32(IS_LITTLE_ENDIAN ? buffer.getInt(offset) : Integer.reverseBytes(buffer.getInt(offset)));
    }

    private class VertexPointer implements VertexData {
        protected int bytePointer = 0;

        @Override
        public float x() {
            if (hasPosition) return buffer.getFloat(bytePointer + positionOffset);
            return 0;
        }

        @Override
        public float y() {
            if (hasPosition) return buffer.getFloat(bytePointer + positionOffset + 4);
            return 0;
        }

        @Override
        public float z() {
            if (hasPosition) return buffer.getFloat(bytePointer + positionOffset + 8);
            return 0;
        }

        @Override
        public Vec3 position() {
            if (hasPosition) return new Vec3(buffer.getFloat(bytePointer + positionOffset), buffer.getFloat(bytePointer + positionOffset + 4), buffer.getFloat(bytePointer + positionOffset + 8));
            return Vec3.ZERO;
        }

        @Override
        public int argb() {
            if (hasColor) return readColor(bytePointer + colorOffset);
            return 0;
        }

        @Override
        public float u() {
            if (hasUV) return buffer.getFloat(bytePointer + uvOffset);
            return 0;
        }

        @Override
        public float v() {
            if (hasUV) return buffer.getFloat(bytePointer + uvOffset + 4);
            return 0;
        }

        @Override
        public UV uv() {
            if (hasUV) return new UV(buffer.getFloat(bytePointer + uvOffset), buffer.getFloat(bytePointer + uvOffset + 4));
            return new UV(0, 0);
        }

        @Override
        public int overlay() {
            if (hasOverlay) return buffer.getInt(bytePointer + overlayOffset);
            return 0;
        }

        @Override
        public int light() {
            if (hasLight) return buffer.getInt(bytePointer + lightOffset);
            return 0;
        }

        @Override
        public float normalX() {
            if (hasNormal) return buffer.get(bytePointer + normalOffset) / 127.0f;
            return 0;
        }

        @Override
        public float normalY() {
            if (hasNormal) return buffer.get(bytePointer + normalOffset + 1) / 127.0f;
            return 0;
        }

        @Override
        public float normalZ() {
            if (hasNormal) return buffer.get(bytePointer + normalOffset + 2) / 127.0f;
            return 0;
        }

        @Override
        public Vec3 normal() {
            if (hasNormal) return new Vec3(buffer.get(bytePointer + normalOffset) / 127.0f, buffer.get(bytePointer + normalOffset + 1) / 127.0f, buffer.get(bytePointer + normalOffset + 2) / 127.0f);
            return Vec3.ZERO;
        }

        @Override
        public VertexData asImmutable() {
            if (fastFormat) {
                return new ImmutableVertex(buffer.getFloat(bytePointer),
                        buffer.getFloat(bytePointer + 4),
                        buffer.getFloat(bytePointer + 8),
                        readColor(bytePointer + 12),
                        buffer.getFloat(bytePointer + 16),
                        buffer.getFloat(bytePointer + 20),
                        buffer.getInt(bytePointer + 24),
                        buffer.getInt(bytePointer + 28),
                        buffer.get(bytePointer + 32) / 127.0f,
                        buffer.get(bytePointer + 33) / 127.0f,
                        buffer.get(bytePointer + 34) / 127.0f);
            }
            return VertexData.super.asImmutable();
        }
    }

    private class VertexIterator extends VertexPointer implements Iterator<VertexData> {
        private final int byteCount = vertexCount * vertexSize;

        @Override
        public boolean hasNext() {
            return bytePointer < byteCount - vertexSize;
        }

        @Override
        public @NotNull VertexData next() {
            bytePointer += vertexSize;
            return this;
        }
    }

    private class VertexSpliterator extends VertexPointer implements Spliterator<VertexData> {
        private final int endIndex;
        private int currentIndex;

        public VertexSpliterator(int start, int end) {
            currentIndex = start;
            endIndex = end;
        }

        @Override
        public boolean tryAdvance(Consumer<? super VertexData> action) {
            if (currentIndex < endIndex) {
                bytePointer = currentIndex * vertexSize;
                action.accept(this);
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

    private class PrimitiveReader implements Iterable<VertexData[]> {
        private final int primitiveLength, primitiveStride, primitiveCount;
        private final boolean startWithFirst;

        public PrimitiveReader(VertexFormat.Mode drawMode) {
            primitiveLength = drawMode.primitiveLength;
            primitiveStride = drawMode.primitiveStride;
            primitiveCount = (vertexCount - primitiveLength) / primitiveStride + 1;
            startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
        }

        @Override
        public @NotNull Iterator<VertexData[]> iterator() {
            return new PrimitiveIterator();
        }

        @Override
        public Spliterator<VertexData[]> spliterator() {
            return new PrimitiveSpliterator(0, primitiveCount);
        }

        private void readPrimitiveAt(int index, MutableVertex[] primitive) {
            int vertexIndex = index * primitiveStride;
            for (int i = startWithFirst ? 1 : 0; i < primitiveLength; i++) {
                readVertexAt(vertexIndex + i, primitive[i]);
            }
        }

        private class PrimitiveIterator implements Iterator<VertexData[]> {
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

        private class PrimitiveSpliterator implements Spliterator<VertexData[]> {
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

    private class FastQuadReader implements Iterable<VertexData[]> {
        private final int quadCount = vertexCount / 4;

        @Override
        public @NotNull Iterator<VertexData[]> iterator() {
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
                mutable.normalX = buffer.get(vertexOffset + 32) / 127.0f;
                mutable.normalY = buffer.get(vertexOffset + 33) / 127.0f;
                mutable.normalZ = buffer.get(vertexOffset + 34) / 127.0f;
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

        private class FastQuadSpliterator implements Spliterator<VertexData[]> {
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
