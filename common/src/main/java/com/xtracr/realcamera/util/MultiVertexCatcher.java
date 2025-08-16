package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface MultiVertexCatcher extends MultiBufferSource {
    default void updateModel(Minecraft client, Entity cameraEntity, float x, float y, float z, float yaw, float deltaTick, PoseStack poseStack, int packedLight) {
        client.getEntityRenderDispatcher().render(cameraEntity, x, y, z, yaw, deltaTick, poseStack, this, packedLight);
    }

    void sendVertices(VertexRecorder recorder);

    abstract class VertexCatcher implements VertexConsumer {
        protected final RenderType renderType;
        private float x, y, z, u, v, normalX, normalY, normalZ;
        private int argb, overlay, light;
        private boolean active;

        protected VertexCatcher(RenderType renderType) {
            this.renderType = renderType;
        }

        protected void endVertex() {
            if (!active) return;
            addVertexInternal(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
            x = y = z = normalX = normalY = normalZ = 0;
            u = v = overlay = light = argb = 0;
            active = false;
        }

        protected abstract void addVertexInternal(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);

        public RenderType renderType() {
            return renderType;
        }

        @Override
        public @NotNull VertexConsumer addVertex(float x, float y, float z) {
            endVertex();
            active = true;
            this.x = x;
            this.y = y;
            this.z = z;
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
            normalX = x;
            normalY = y;
            normalZ = z;
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
            addVertexInternal(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
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
