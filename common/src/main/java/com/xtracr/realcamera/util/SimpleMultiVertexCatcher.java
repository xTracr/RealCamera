package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

public class SimpleMultiVertexCatcher implements MultiVertexCatcher {
    protected final Stack<RecordBuilder> catchers = new Stack<>();

    @Override
    public void sendVertices(VertexRecorder recorder) {
        MultiVertexCatcher.super.sendVertices(recorder);
        recorder.records().addAll(catchers.stream().map(catcher -> VertexRecorder.buildVertices(catcher.collectVertices(), catcher.renderType())).toList());
    }

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
        if (catchers.isEmpty() || !Objects.equals(catchers.peek().renderType(), renderType) || !renderType.canConsolidateConsecutiveGeometry()) {
            return catchers.push(new RecordBuilder(renderType));
        }
        return catchers.peek();
    }

    public static class RecordBuilder extends VertexCatcher {
        protected final List<VertexData> vertexList = new ArrayList<>();

        public RecordBuilder(RenderType renderType) {
            super(renderType);
        }

        public VertexData[] collectVertices() {
            endVertex();
            return this.vertexList.toArray(VertexData[]::new);
        }

        @Override
        protected void addVertexInternal(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
            vertexList.add(new VertexData(x, y, z, argb, u, v, overlay, light, normalX, normalY, normalZ));
        }
    }
}
