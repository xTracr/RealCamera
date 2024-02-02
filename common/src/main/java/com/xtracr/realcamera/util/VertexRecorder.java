package com.xtracr.realcamera.util;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class VertexRecorder implements VertexConsumer {
    protected final List<Vertex> vertices = new ArrayList<>();
    private Vec3d pos = Vec3d.ZERO, normal = Vec3d.ZERO;
    private int argb, overlay, light;
    private float u, v;

    public VertexRecorder() {}

    public int vertexCount() {
        return vertices.size();
    }

    public Vec3d getPos(int index) {
        return vertices.get(index).pos();
    }

    public Vec3d getNormal(int index) {
        return vertices.get(index).normal();
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        pos = new Vec3d(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        argb = alpha << 24 | red << 16 | green << 8 | blue;
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        overlay = (short) u | (short) v << 16;
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        light = (short) u | (short) v << 16;
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        normal = new Vec3d(x, y, z);
        return this;
    }

    @Override
    public void next() {
        vertices.add(new Vertex(pos, argb, u, v, overlay, light, normal));
        pos = normal = Vec3d.ZERO;
        u = v = overlay = light = argb = 0;
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha) {
    }

    @Override
    public void unfixColor() {
    }

    public record Vertex(Vec3d pos, int argb, float u, float v, int overlay, int light, Vec3d normal) {}
}
