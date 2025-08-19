package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;

public record ColoredQuadRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        int x0,
        int y0,
        int z0,
        int x1,
        int y1,
        int z1,
        int x2,
        int y2,
        int z2,
        int x3,
        int y3,
        int z3,
        int argb,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColoredQuadRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            int x0,
            int y0,
            int z0,
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2,
            int x3,
            int y3,
            int z3,
            int argb,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, argb, scissorArea,
                getBounds(Math.min(x0, Math.min(x1, Math.min(x2, x3))), Math.min(y0, Math.min(y1, Math.min(y2, y3))),
                        Math.max(x0, Math.max(x1, Math.max(x2, x3))), Math.max(y0, Math.max(y1, Math.max(y2, y3))), scissorArea));
    }

    @Nullable
    private static ScreenRectangle getBounds(int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle scissorArea2 = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
        return scissorArea != null ? scissorArea.intersection(scissorArea2) : scissorArea2;
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer, float depth) {
        vertexConsumer.addVertex(x0(), y0(), z0()).setColor(argb());
        vertexConsumer.addVertex(x1(), y1(), z1()).setColor(argb());
        vertexConsumer.addVertex(x2(), y2(), z2()).setColor(argb());
        vertexConsumer.addVertex(x3(), y3(), z3()).setColor(argb());
    }
}
