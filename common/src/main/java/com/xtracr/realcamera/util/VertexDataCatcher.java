package com.xtracr.realcamera.util;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class VertexDataCatcher implements VertexConsumer {
    public final List<Vec3d> normalRecorder = new ArrayList<>();
    public final List<Vec3d> posRecorder = new ArrayList<>();

    public VertexDataCatcher() {
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        posRecorder.add(new Vec3d(x, y, z));
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        normalRecorder.add(new Vec3d(x, y, z));
        return this;
    }

    @Override
    public void next() {
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha) {
    }

    @Override
    public void unfixColor() {
    }
}
