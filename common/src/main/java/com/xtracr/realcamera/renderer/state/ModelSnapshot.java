package com.xtracr.realcamera.renderer.state;

import com.xtracr.realcamera.renderer.IdentifiedGeometry;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;

/**
 * Primitives reference immutable vertices from vertexArray.
 */
public record ModelSnapshot(
        RenderType renderType,
        String textureId,
        VertexData[] vertexArray,
        Primitive[] primitiveArray
) implements IdentifiedGeometry {
    @Override
    public int vertexCount() {
        return vertexArray.length;
    }

    @Override
    public int primitiveCount() {
        return primitiveArray.length;
    }

    @Override
    public VertexData readVertexAt(int index) {
        return vertexArray[index];
    }

    @Override
    public Primitive readPrimitiveAt(int index) {
        return primitiveArray[index];
    }

    @Override
    public Iterable<Primitive> primitives() {
        return Arrays.asList(primitiveArray);
    }

    @Override
    public @NonNull Iterator<VertexData> iterator() {
        return Arrays.asList(vertexArray).iterator();
    }

    @Override
    public Spliterator<VertexData> spliterator() {
        return Arrays.spliterator(vertexArray);
    }
}
