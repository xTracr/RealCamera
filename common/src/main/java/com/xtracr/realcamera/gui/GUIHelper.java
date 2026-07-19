package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.renderer.state.BuiltModelRecord;
import com.xtracr.realcamera.renderer.state.VertexData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public class GUIHelper {
    public static final int MODEL_MIN_Z = 0, MODEL_MAX_Z = 200;

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
        buffer.vertex(matrix4f, x0, y0, z).color(argb).endVertex();
        buffer.vertex(matrix4f, x0, y1, z).color(argb).endVertex();
        buffer.vertex(matrix4f, x1, y1, z).color(argb).endVertex();
        buffer.vertex(matrix4f, x1, y0, z).color(argb).endVertex();
        graphics.flush();
    }

    public static void outline(GuiGraphics graphics, float x, float y, float width, float height, int argb) {
        fill(graphics, x, y, x + width, y + 1, argb);
        fill(graphics, x, y + height - 1, x + width, y + height, argb);
        fill(graphics, x, y + 1, x + 1, y + height - 1, argb);
        fill(graphics, x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public static void triangleOrQuad(GuiGraphics graphics, VertexData[] polygon, float z, int argb) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (VertexData vertex : polygon) buffer.vertex(vertex.x(), vertex.y(), z).color(argb).endVertex();
        if (polygon.length == 3) buffer.vertex(polygon[2].x(), polygon[2].y(), z).color(argb).endVertex();
        graphics.flush();
    }

    public static void vector(GuiGraphics graphics, Vec3 start, Vec3 vector, float z, int argb) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.lines());
        buffer.vertex((float) start.x, (float) start.y, z).color(argb).normal((float) vector.x, (float) vector.y, 0).endVertex();
        buffer.vertex((float) (start.x + vector.x), (float) (start.y + vector.y), z).color(argb).normal((float) vector.x, (float) vector.y, 0).endVertex();
        graphics.flush();
    }

    public static void culledModels(GuiGraphics graphics, List<BuiltModelRecord> modelRecords, Matrix4f transform) {
        float minEntityZ = MODEL_MIN_Z, maxEntityZ = MODEL_MAX_Z;
        for (BuiltModelRecord record : modelRecords) {
            for (VertexData vertex : record.vertices()) {
                if (vertex.z() < minEntityZ) minEntityZ = vertex.z();
                if (vertex.z() > maxEntityZ) maxEntityZ = vertex.z();
            }
        }
        Matrix4f positionMatrix = new Matrix4f(transform).scale(1, 1, MODEL_MAX_Z / (maxEntityZ - minEntityZ)).translate(0, 0, -minEntityZ);
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
        modelRecords.forEach(record -> {
            VertexConsumer buffer = graphics.bufferSource().getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                for (VertexData vertex : record.vertices()) vertex.render(buffer, positionMatrix, normalMatrix);
                return;
            }
            for (VertexData[] primitive : record.primitives()) {
                for (VertexData vertex : primitive) vertex.render(buffer, positionMatrix, normalMatrix);
            }
        });
        graphics.flush();
    }

    public static void flattenedModels(GuiGraphics graphics, List<BuiltModelRecord> textureRecords, Matrix4f positionMatrix) {
        textureRecords.forEach(record -> {
            VertexConsumer buffer = graphics.bufferSource().getBuffer(record.renderType());
            Vector3f position = new Vector3f();
            for (VertexData vertex : record.vertices()) {
                position.set(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                int argb = vertex.argb();
                buffer.vertex(position.x(), position.y(), 0 ,
                        (float) (argb >> 16 & 0xFF) / 255, (float) (argb >> 8 & 0xFF) / 255, (float) (argb & 0xFF) / 255, (float) (argb >> 24) / 255,
                        vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);

            }
        });
    }
}
