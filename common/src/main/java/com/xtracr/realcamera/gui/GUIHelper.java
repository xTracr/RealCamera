package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.renderer.state.VertexData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class GUIHelper {
    public static void enableScissor(GuiGraphics graphics, ScreenRectangle rectangle) {
        graphics.enableScissor(rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom());
    }

    public static void fill(GuiGraphics graphics, float x0, float y0, float x1, float y1, int argb) {
        fill(graphics, x0, y0, x1, y1, 0, argb);
    }

    public static void fill(GuiGraphics graphics, float x0, float y0, float x1, float y1, float z, int argb) {
        Matrix4f matrix4f = graphics.pose().last().pose();
        if (x0 < x1) {
            float o = x0;
            x0 = x1;
            x1 = o;
        }
        if (y0 < y1) {
            float o = y0;
            y0 = y1;
            y1 = o;
        }
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(matrix4f, x0, y0, z).setColor(argb);
        buffer.addVertex(matrix4f, x0, y1, z).setColor(argb);
        buffer.addVertex(matrix4f, x1, y1, z).setColor(argb);
        buffer.addVertex(matrix4f, x1, y0, z).setColor(argb);
        graphics.flush();
    }

    public static void renderOutline(GuiGraphics graphics, float x, float y, float width, float height, int argb) {
        fill(graphics, x, y, x + width, y + 1, argb);
        fill(graphics, x, y + height - 1, x + width, y + height, argb);
        fill(graphics, x, y + 1, x + 1, y + height - 1, argb);
        fill(graphics, x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public static void renderPolygon(GuiGraphics graphics, VertexData[] polygon, float z, int argb) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (VertexData vertex : polygon) buffer.addVertex(vertex.x(), vertex.y(), z).setColor(argb);
        if (polygon.length == 3) buffer.addVertex(polygon[2].x(), polygon[2].y(), z).setColor(argb);
        graphics.flush();
    }

    public static void renderVector(GuiGraphics graphics, Vec3 start, Vec3 vector, float z, int argb) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.lines());
        buffer.addVertex((float) start.x, (float) start.y, z).setColor(argb).setNormal((float) vector.x, (float) vector.y, 0);
        buffer.addVertex((float) (start.x + vector.x), (float) (start.y + vector.y), z).setColor(argb).setNormal((float) vector.x, (float) vector.y, 0);
        graphics.flush();
    }
}
