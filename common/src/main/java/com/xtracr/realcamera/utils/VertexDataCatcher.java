package com.xtracr.realcamera.utils;

import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

public class VertexDataCatcher implements VertexConsumer {
    private final IntPredicate matrixPredicate;
    private final IntPredicate normalPredicate;
    private final IntPredicate posPredicate;
    public final List<Matrix3f> matrixRecorder = new ArrayList<>();
    public final List<Matrix4f> offsetRecorder = new ArrayList<>();
    public final List<Vec3d> normalRecorder = new ArrayList<>();
    public final List<Vec3d> posRecorder = new ArrayList<>();
    private int index = -1;

    public VertexDataCatcher(IntPredicate matrixPredicate, IntPredicate normalPredicate, IntPredicate posPredicate) {
        this.matrixPredicate = matrixPredicate;
        this.normalPredicate = normalPredicate;
        this.posPredicate = posPredicate;
    }

    protected void clear() {
        index = -1;
        matrixRecorder.clear();
        offsetRecorder.clear();
        normalRecorder.clear();
        posRecorder.clear();
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        index++;
        if (matrixPredicate.test(index)) {
            matrixRecorder.add(new Matrix3f(RealCameraCore.matrixStack.peek().getNormalMatrix()));
            offsetRecorder.add(new Matrix4f(RealCameraCore.matrixStack.peek().getPositionMatrix()));
        }
        if (posPredicate.test(index)) posRecorder.add(new Vec3d(x, y, z));
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
        if (normalPredicate.test(index)) normalRecorder.add(new Vec3d(x, y, z));
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
