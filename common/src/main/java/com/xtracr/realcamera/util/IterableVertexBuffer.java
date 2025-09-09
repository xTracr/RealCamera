package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xtracr.realcamera.util.VertexData.MutableVertex;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IterableVertexBuffer implements Iterable<VertexData> {
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    public final int vertexCount;
    private final MutableVertex reusableVertex = VertexData.mutable();
    private final VertexFormat.Mode drawMode;
    private final ByteBuffer buffer;
    private final int vertexSize;
    private final int positionOffset, colorOffset, uvOffset, overlayOffset, lightOffset, normalOffset;
    private final boolean hasPosition, hasColor, hasUV, hasOverlay, hasLight, hasNormal;
    private final boolean fastFormat;

    public IterableVertexBuffer(MeshData meshData) {
        buffer = meshData.vertexBuffer();
        MeshData.DrawState drawState = meshData.drawState();
        VertexFormat format = drawState.format();
        drawMode = drawState.mode();
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
    }

    @Override
    public @NotNull Iterator<VertexData> iterator() {
        return new VertexIterator();
    }

    @Override
    public Spliterator<VertexData> spliterator() {
        return new VertexSpliterator(0, vertexCount);
    }

    public Stream<VertexData> stream() {
        return StreamSupport.stream(new VertexSpliterator(0, vertexCount), false);
    }

    public Stream<VertexData> parallelStream() {
        return StreamSupport.stream(new VertexSpliterator(0, vertexCount), true);
    }

    public Stream<VertexData[]> primitiveStream() {
        return StreamSupport.stream(primitiveSpliterator(), false);
    }

    public Stream<VertexData[]> parallelPrimitiveStream() {
        return StreamSupport.stream(primitiveSpliterator(), true);
    }

    public VertexData readVertexAt(int index) {
        return readVertexAt(index, reusableVertex);
    }

    public MutableVertex readVertexAt(int index, MutableVertex mutable) {
        if (index < 0 || index >= vertexCount) {
            throw new IndexOutOfBoundsException("Vertex index out of bounds: " + index);
        }
        int vertexOffset = index * vertexSize;
        if (fastFormat) {
            mutable.x = buffer.getFloat(vertexOffset);
            mutable.y = buffer.getFloat(vertexOffset + 4);
            mutable.z = buffer.getFloat(vertexOffset + 8);
            int argb = buffer.getInt(vertexOffset + 12);
            mutable.argb = IS_LITTLE_ENDIAN ? argb : Integer.reverseBytes(argb);
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
            mutable.argb = buffer.getInt(vertexOffset + colorOffset);
            mutable.argb = IS_LITTLE_ENDIAN ? mutable.argb : Integer.reverseBytes(mutable.argb);
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

    private Spliterator<VertexData[]> primitiveSpliterator() {
        if (fastFormat && drawMode == VertexFormat.Mode.QUADS)
            return new FastQuadSpliterator(0, vertexCount / 4);
        return new PrimitiveSpliterator(0);
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
        public int argb() {
            if (hasColor) return buffer.getInt(bytePointer + colorOffset);
            return 0;
        }

        @Override
        public Vec3 position() {
            if (hasPosition) return new Vec3(buffer.getFloat(bytePointer + positionOffset), buffer.getFloat(bytePointer + positionOffset + 4), buffer.getFloat(bytePointer + positionOffset + 8));
            return Vec3.ZERO;
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

    private class PrimitiveSpliterator implements Spliterator<VertexData[]> {
        private final int primitiveLength = drawMode.primitiveLength, primitiveStride = drawMode.primitiveStride;
        private final MutableVertex[] reusablePrimitive = new MutableVertex[primitiveLength];
        private final boolean startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
        private final int endIndex;
        private int currentIndex;

        public PrimitiveSpliterator(int start) {
            currentIndex = start;
            endIndex = (vertexCount - primitiveLength) / primitiveStride + 1;
            for (int i = 0; i < primitiveLength; i++) {
                reusablePrimitive[i] = VertexData.mutable();
            }
            readVertexAt(0, reusablePrimitive[0]);
        }

        public PrimitiveSpliterator(int start, int end) {
            currentIndex = start;
            endIndex = end;
            for (int i = 0; i < primitiveLength; i++) {
                reusablePrimitive[i] = VertexData.mutable();
            }
            readVertexAt(0, reusablePrimitive[0]);
        }

        @Override
        public boolean tryAdvance(Consumer<? super VertexData[]> action) {
            if (currentIndex < endIndex) {
                int vertexIndex = currentIndex * primitiveStride;
                for (int i = startWithFirst ? 1 : 0; i < primitiveLength; i++) {
                    readVertexAt(vertexIndex + i, reusablePrimitive[i]);
                }
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

    private class FastQuadSpliterator implements Spliterator<VertexData[]> {
        private final MutableVertex[] reusablePrimitive = new MutableVertex[4];
        private final int endIndex;
        private int currentIndex;

        public FastQuadSpliterator(int start, int end) {
            currentIndex = start;
            endIndex = end;
            for (int i = 0; i < 4; i++) {
                reusablePrimitive[i] = VertexData.mutable();
            }
        }

        private void fastReadQuad(int quadIndex) {
            int vertexOffset = quadIndex * 144;
            for (int i = 0; i < 4; i++, vertexOffset += 36) {
                MutableVertex mutable = reusablePrimitive[i];
                mutable.x = buffer.getFloat(vertexOffset);
                mutable.y = buffer.getFloat(vertexOffset + 4);
                mutable.z = buffer.getFloat(vertexOffset + 8);
                int argb = buffer.getInt(vertexOffset + 12);
                mutable.argb = IS_LITTLE_ENDIAN ? argb : Integer.reverseBytes(argb);
                mutable.u = buffer.getFloat(vertexOffset + 16);
                mutable.v = buffer.getFloat(vertexOffset + 20);
                mutable.overlay = buffer.getInt(vertexOffset + 24);
                mutable.light = buffer.getInt(vertexOffset + 28);
                mutable.normalX = buffer.get(vertexOffset + 32) / 127.0f;
                mutable.normalY = buffer.get(vertexOffset + 33) / 127.0f;
                mutable.normalZ = buffer.get(vertexOffset + 34) / 127.0f;
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super VertexData[]> action) {
            if (currentIndex < endIndex) {
                fastReadQuad(currentIndex);
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
