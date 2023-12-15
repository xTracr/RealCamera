package com.xtracr.realcamera.utils;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;

import java.util.ArrayList;
import java.util.List;

public class VertexDataCatcher implements VertexConsumer {
    protected final List<Vec3d> normalRecorder = new ArrayList<>();
    protected final List<Matrix3f> matrixRecorder = new ArrayList<>();
    protected MatrixStack matrixStack = new MatrixStack();
    private int index;

    protected void clear() {
        index = -1;
        normalRecorder.clear();
        matrixRecorder.clear();
        matrixStack = new MatrixStack();
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        index++;
        if (VertexDataAnalyser.isAnalysing()) {
            matrixRecorder.add(new Matrix3f(matrixStack.peek().getNormalMatrix()));
        }
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
        if (VertexDataAnalyser.isAnalysing()) {
            normalRecorder.add(new Vec3d(x, y, z));
        }
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
