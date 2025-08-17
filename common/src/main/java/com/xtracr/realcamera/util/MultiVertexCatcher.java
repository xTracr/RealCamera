package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

public interface MultiVertexCatcher extends MultiBufferSource {
    void sendVertices(VertexRecorder recorder);

    abstract class VertexCatcher implements VertexConsumer {
        protected final RenderType renderType;
        private float x, y, z, u, v, normalX, normalY, normalZ;
        private int argb, overlay, light;
        private boolean active;

        protected VertexCatcher(RenderType renderType) {
            this.renderType = renderType;
        }

        public void endVertex() {
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
        public @NotNull VertexConsumer vertex(double x, double y, double z) {
            endVertex();
            active = true;
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            return this;
        }

        @Override
        public @NotNull VertexConsumer color(int red, int green, int blue, int alpha) {
            argb = alpha << 24 | red << 16 | green << 8 | blue;
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public @NotNull VertexConsumer overlayCoords(int u, int v) {
            overlay = (short) u | (short) v << 16;
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv2(int u, int v) {
            light = (short) u | (short) v << 16;
            return this;
        }

        @Override
        public @NotNull VertexConsumer normal(float x, float y, float z) {
            normalX = x;
            normalY = y;
            normalZ = z;
            return this;
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
            int argb = (int) (alpha * 255.0f) << 24 | (int) (red * 255.0f) << 16 | (int) (green * 255.0f) << 8 | (int) (blue * 255.0f);
            addVertexInternal(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ);
        }

        @Override
        public @NotNull VertexConsumer color(int argb) {
            this.argb = argb;
            return this;
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }
}
