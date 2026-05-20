package com.xtracr.realcamera.renderer.state;

public final class MutableVertex implements VertexData {
    public float x, y, z;
    public int argb;
    public float u, v;
    public int overlay, light;
    public float normalX, normalY, normalZ;

    MutableVertex() {
    }

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
