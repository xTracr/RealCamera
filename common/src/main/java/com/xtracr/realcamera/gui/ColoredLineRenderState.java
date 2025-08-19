package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;

public record ColoredLineRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        int x0,
        int y0,
        int z0,
        int x1,
        int y1,
        int z1,
        float normalX,
        float normalY,
        float normalZ,
        int argb,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColoredLineRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            int x0,
            int y0,
            int z0,
            int x1,
            int y1,
            int z1,
            float normalX,
            float normalY,
            float normalZ,
            int argb,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, x0, y0, z0, x1, y1, z1, normalX, normalY, normalZ, argb, scissorArea, getBounds(x0, y0, x1, y1, scissorArea));
    }

    @Nullable
    private static ScreenRectangle getBounds(int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle scissorArea2 = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
        return scissorArea != null ? scissorArea.intersection(scissorArea2) : scissorArea2;
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer, float depth) {
        vertexConsumer.addVertex(x0(), y0(), z0()).setColor(argb()).setNormal(normalX(), normalY(), normalZ());
        vertexConsumer.addVertex(x1(), y1(), z1()).setColor(argb()).setNormal(normalX(), normalY(), normalZ());
    }
}
