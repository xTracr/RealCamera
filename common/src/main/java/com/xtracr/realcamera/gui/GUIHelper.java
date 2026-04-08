package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.mixin.accessor.GuiGraphicsExtractorAccessor;
import com.xtracr.realcamera.renderer.VertexData;
import com.xtracr.realcamera.renderer.state.gui.ColoredFloatLineRenderState;
import com.xtracr.realcamera.renderer.state.gui.ColoredFloatQuadRenderState;
import com.xtracr.realcamera.renderer.state.gui.ColoredFloatRectangleRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public class GUIHelper {
    @Nullable
    public static ScreenRectangle getBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle scissorArea2 = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(scissorArea2) : scissorArea2;
    }

    public static void enableScissor(GuiGraphicsExtractor graphics, ScreenRectangle rectangle) {
        graphics.enableScissor(rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom());
    }

    public static void fill(GuiGraphicsExtractor graphics, float x0, float y0, float x1, float y1, int argb) {
        fill(graphics, x0, y0, x1, y1, 0, argb);
    }

    public static void fill(GuiGraphicsExtractor graphics, float x0, float y0, float x1, float y1, float z, int argb) {
        ((GuiGraphicsExtractorAccessor) graphics).getGuiRenderState().addGuiElement(new ColoredFloatRectangleRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
                x0, y0, x1, y1, z, argb, ((GuiGraphicsExtractorAccessor) graphics).getScissorStack().peek()));
    }

    public static void renderOutline(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int argb) {
        fill(graphics, x, y, x + width, y + 1, argb);
        fill(graphics, x, y + height - 1, x + width, y + height, argb);
        fill(graphics, x, y + 1, x + 1, y + height - 1, argb);
        fill(graphics, x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public static void renderPolygon(GuiGraphicsExtractor graphics, VertexData[] polygon, float z, int argb) {
        if (polygon.length < 3) return;
        float x0 = polygon[0].x(), y0 = polygon[0].y();
        float x1 = polygon[1].x(), y1 = polygon[1].y();
        float x2 = polygon[2].x(), y2 = polygon[2].y();
        float x3 = polygon.length > 3 ? polygon[3].x() : x2, y3 = polygon.length > 3 ? polygon[3].y() : y2;
        ((GuiGraphicsExtractorAccessor) graphics).getGuiRenderState().addGuiElement(new ColoredFloatQuadRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
                x0, y0, z, x1, y1, z, x2, y2, z, x3, y3, z, argb, ((GuiGraphicsExtractorAccessor) graphics).getScissorStack().peek()));
    }

    public static void renderVector(GuiGraphicsExtractor graphics, Vec3 start, Vec3 vector, float z, int argb) {
        ((GuiGraphicsExtractorAccessor) graphics).getGuiRenderState().addGuiElement(new ColoredFloatLineRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
                (float) start.x(), (float) start.y(), z,
                (float) vector.x(), (float) vector.y(), 0, argb, ((GuiGraphicsExtractorAccessor) graphics).getScissorStack().peek()));
    }
}
