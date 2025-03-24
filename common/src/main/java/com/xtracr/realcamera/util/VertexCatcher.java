package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.api.IVertexCatcher;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class VertexCatcher implements IVertexCatcher, VertexConsumer {
    private final List<IVertexRecorder.Vertex> vertices = new ArrayList<>();
    private final RenderType renderType;
    private final Vector3f pos = new Vector3f(), normal = new Vector3f();
    private int argb, overlay, light;
    private float u, v;
    private boolean active;

    VertexCatcher(RenderType renderType) {
        this.renderType = renderType;
    }

    @Override
    public RenderType renderType() {
        return renderType;
    }

    @Override
    public IVertexRecorder.Vertex[] collectVertices() {
        endVertex();
        return this.vertices.toArray(IVertexRecorder.Vertex[]::new);
    }

    private void endVertex() {
        if (!active) return;
        vertices.add(new IVertexRecorder.Vertex(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z()));
        pos.set(normal.set(0));
        u = v = overlay = light = argb = 0;
        active = false;
    }

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        endVertex();
        active = true;
        pos.set(x, y, z);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
        argb = alpha << 24 | red << 16 | green << 8 | blue;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        overlay = (short) u | (short) v << 16;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        light = (short) u | (short) v << 16;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        normal.set(x, y, z);
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        vertices.add(new IVertexRecorder.Vertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ));
    }

    @Override
    public @NotNull VertexConsumer setColor(int argb) {
        this.argb = argb;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setOverlay(int overlay) {
        this.overlay = overlay;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setLight(int light) {
        this.light = light;
        return this;
    }
}
