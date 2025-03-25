package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;

public class SimpleMultiVertexCatcher implements MultiVertexCatcher, MultiBufferSource {
    private final Stack<SimpleVertexCatcher> catchers = new Stack<>();

    @Override
    public void updateModel(Minecraft client, Entity cameraEntity, float x, float y, float z, float yaw, float tickDelta, PoseStack poseStack, int packedLight) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.render(cameraEntity, x, y, z, yaw, tickDelta, poseStack, this, packedLight);
    }

    @Override
    public void forEachCatcher(Consumer<VertexCatcher> consumer) {
        catchers.forEach(consumer);
    }

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
        if (catchers.isEmpty() || !Objects.equals(catchers.peek().renderType(), renderType) || !renderType.canConsolidateConsecutiveGeometry()) {
            return catchers.push(new SimpleVertexCatcher(renderType));
        }
        return catchers.peek();
    }

    public static class SimpleVertexCatcher implements VertexCatcher, VertexConsumer {
        private final List<VertexRecorder.Vertex> vertices = new ArrayList<>();
        private final RenderType renderType;
        private final Vector3f pos = new Vector3f(), normal = new Vector3f();
        private int argb, overlay, light;
        private float u, v;
        private boolean active;

        SimpleVertexCatcher(RenderType renderType) {
            this.renderType = renderType;
        }

        @Override
        public RenderType renderType() {
            return renderType;
        }

        @Override
        public VertexRecorder.Vertex[] collectVertices() {
            endVertex();
            return this.vertices.toArray(VertexRecorder.Vertex[]::new);
        }

        private void endVertex() {
            if (!active) return;
            vertices.add(new VertexRecorder.Vertex(pos.x(), pos.y(), pos.z(), argb, u, v, overlay, light, normal.x(), normal.y(), normal.z()));
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
            vertices.add(new VertexRecorder.Vertex(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ));
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
}
