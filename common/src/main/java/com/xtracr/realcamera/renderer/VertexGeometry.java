package com.xtracr.realcamera.renderer;

import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Iterators may reuse mutable data; copy retained values before advancing.
 * Indexed reads return immutable values. Captured buffers remain valid until their mesh is closed.
 */
public interface VertexGeometry extends Iterable<VertexData> {
    int vertexCount();

    int primitiveCount();

    VertexData readVertexAt(int index);

    Primitive readPrimitiveAt(int index);

    Iterable<Primitive> primitives();

    default Stream<VertexData> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default Stream<Primitive> primitiveStream() {
        return StreamSupport.stream(primitives().spliterator(), false);
    }
}
