package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.mixin.accessor.GuiGraphicsExtractorAccessor;
import com.xtracr.realcamera.renderer.state.ModelSnapshot;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.renderer.state.gui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class GUIHelper {
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

    public static void outline(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int argb) {
        fill(graphics, x, y, x + width, y + 1, argb);
        fill(graphics, x, y + height - 1, x + width, y + height, argb);
        fill(graphics, x, y + 1, x + 1, y + height - 1, argb);
        fill(graphics, x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public static void triangleOrQuad(GuiGraphicsExtractor graphics, Primitive primitive, float z, int argb) {
        triangleOrQuad(graphics, primitive, z, argb, false);
    }

    public static void triangleOrQuad(GuiGraphicsExtractor graphics, Primitive primitive, float z, int argb, boolean reversed) {
        if (!primitive.isFace()) return;
        int last = primitive.vertexCount() - 1;
        VertexData a = primitive.vertex(reversed ? last : 0);
        VertexData b = primitive.vertex(reversed ? last - 1 : 1);
        VertexData c = primitive.vertex(reversed ? last - 2 : 2);
        VertexData d = primitive.vertexCount() == 4 ? primitive.vertex(reversed ? 0 : 3) : c;
        ((GuiGraphicsExtractorAccessor) graphics).getGuiRenderState().addGuiElement(new ColoredFloatQuadRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
                a.x(), a.y(), z, b.x(), b.y(), z, c.x(), c.y(), z, d.x(), d.y(), z, argb, ((GuiGraphicsExtractorAccessor) graphics).getScissorStack().peek()));
    }

    public static void vector(GuiGraphicsExtractor graphics, Vec3 start, Vec3 vector, float z, int argb) {
        ((GuiGraphicsExtractorAccessor) graphics).getGuiRenderState().addGuiElement(new ColoredFloatLineRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
                (float) start.x(), (float) start.y(), z,
                (float) vector.x(), (float) vector.y(), 0, argb, ((GuiGraphicsExtractorAccessor) graphics).getScissorStack().peek()));
    }

    public static void culledModels(GuiGraphicsExtractor graphics, List<ModelSnapshot> snapshots, float scale, Matrix4fc transform, int x0, int y0, int x1, int y1) {
        ((GuiGraphicsExtractorAccessor) graphics).getGuiRenderState().addPicturesInPictureState(
                new GuiCulledModelsRenderState(snapshots, transform, x0, y0, x1, y1, scale, ((GuiGraphicsExtractorAccessor) graphics).getScissorStack().peek())
        );
    }

    public static void flattenedModels(GuiGraphicsExtractor graphics, List<ModelSnapshot> snapshots, Vector3f translation, int x0, int y0, int x1, int y1, float scale) {
        ((GuiGraphicsExtractorAccessor) graphics).getGuiRenderState().addPicturesInPictureState(
                new GuiFlattenedModelsRenderState(snapshots, translation, x0, y0, x1, y1, scale, ((GuiGraphicsExtractorAccessor) graphics).getScissorStack().peek())
        );
    }
}
