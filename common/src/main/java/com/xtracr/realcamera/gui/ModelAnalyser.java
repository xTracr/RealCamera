package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.util.Triple;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModelAnalyser extends VertexRecorder {
    BuiltRecord focusedRecord;

    public String focusedTextureId() {
        if (focusedRecord == null) return null;
        return getTextureId(focusedRecord);
    }

    public Pair<Float, Float> getCenterUV(int quadIndex) {
        if (quadIndex == -1 || focusedRecord == null || quadIndex >= focusedRecord.quadCount()) return null;
        float u = 0, v = 0;
        Vertex[] quad = focusedRecord.vertices()[quadIndex];
        for (Vertex vertex : quad) {
            u += vertex.u();
            v += vertex.v();
        }
        return new Pair<>(u / quad.length, v / quad.length);
    }

    public int getFocusedIndex(int mouseX, int mouseY, int layers) {
        List<Triple<Double, BuiltRecord, Integer>> sortByDepth = new ArrayList<>();
        records.forEach(record -> {
            Vertex[][] vertices = record.vertices();
            for (int i = 0, size = vertices.length; i < size; i++) {
                Polygon polygon = new Polygon();
                Vertex[] quad = vertices[i];
                for (Vertex vertex : quad) polygon.addPoint((int) vertex.x(), (int) vertex.y());
                Vertex point = quad[0];
                double deltaZ = 0;
                if (point.normalZ() != 0) deltaZ = (point.normalX() * (mouseX - point.x()) + point.normalY() * (mouseY - point.y())) / point.normalZ();
                if (polygon.contains(mouseX, mouseY)) sortByDepth.add(new Triple<>(point.z() + deltaZ, record, i));
            }
        });
        if (sortByDepth.isEmpty()) return -1;
        sortByDepth.sort(Comparator.comparingDouble(triple -> -triple.getLeft()));
        Triple<Double, BuiltRecord, Integer> result = sortByDepth.get(Math.min(sortByDepth.size() - 1, layers));
        focusedRecord = result.getMiddle();
        return result.getRight();
    }

    public void drawQuad(DrawContext context, float u, float v, int argb) {
        if (currentRecord == null) return;
        drawQuad(context, getQuadIndex(currentRecord, u, v), argb, false);
    }

    public void drawQuad(DrawContext context, int quadIndex, int argb, boolean drawFocused) {
        BuiltRecord record = drawFocused ? focusedRecord : currentRecord;
        if (quadIndex == -1 || record == null || quadIndex >= record.quadCount()) return;
        drawQuad(context, record.vertices()[quadIndex], argb, 1000);
        if (drawFocused) {
            Vertex[] highlight = record.vertices()[quadIndex];
            int size = highlight.length;
            Vertex[] reversed = new Vertex[size];
            for (int i = 0; i < size; i++) reversed[i] = highlight[size - 1 - i];
            drawQuad(context, reversed, argb, 1000);
        }
    }

    public void drawPolyhedron(DrawContext context, int quadIndex, int argb) {
        if (focusedRecord == null || quadIndex >= focusedRecord.quadCount()) return;
        Vertex[] highlight = focusedRecord.vertices()[quadIndex];
        List<Vertex[]> polyhedron = new ArrayList<>();
        polyhedron.add(highlight);
        List<Integer> indexes = new ArrayList<>(List.of(quadIndex));
        Vertex[][] vertices = focusedRecord.vertices();
        boolean added;
        int size = focusedRecord.quadCount();
        do {
            added = false;
            for (int i = 0; i < size; i++) {
                Vertex[] quad = vertices[i];
                if (indexes.contains(i) | !intersects(quad, polyhedron)) continue;
                polyhedron.add(quad);
                indexes.add(i);
                added = true;
            }
        } while (added);
        List<Integer> resultIndexes = new ArrayList<>(List.of(quadIndex));
        for (int i = quadIndex + 1; i < size; i++) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        for (int i = quadIndex - 1; i >= 0; i--) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        resultIndexes.forEach(i -> drawQuad(context, vertices[i], argb, 1000));
        drawQuad(context, highlight, argb, 1100);
        size = highlight.length;
        Vertex[] reversed = new Vertex[size];
        for (int i = 0; i < size; i++) reversed[i] = highlight[size - 1 - i];
        drawQuad(context, reversed, argb, 1100);
    }

    public void drawNormal(DrawContext context,  float u, float v, int length, int argb) {
        if (currentRecord == null) return;
        int quadIndex = getQuadIndex(currentRecord, u, v);
        if (quadIndex == -1) return;
        Vertex vertex = currentRecord.vertices()[quadIndex][0];
        Vec3d start = getPos(u, v);
        Vec3d end = vertex.normal().multiply(length).add(start);
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getLineStrip());
        vertexConsumer.vertex(start.getX(), start.getY(), start.getZ() + 1200f).color(argb)
                .normal(vertex.normalX(), vertex.normalY(), vertex.normalZ()).next();
        vertexConsumer.vertex(end.getX(), end.getY(), end.getZ() + 1200f).color(argb)
                .normal(vertex.normalX(), vertex.normalY(), vertex.normalZ()).next();
        context.draw();
    }

    private static boolean intersects(Vertex[] quad, List<Vertex[]> quads) {
        final float precision = 0.00001f;
        for (Vertex[] q : quads) for (Vertex v1 : quad)
            for (Vertex v2 : q) if (v1.pos().squaredDistanceTo(v2.pos()) < precision) return true;
        return false;
    }

    private static void drawQuad(DrawContext context, Vertex[] quad, int argb, int offset) {
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        for (Vertex vertex : quad) vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z() + offset).color(argb).next();
        if (quad.length == 3) vertexConsumer.vertex(quad[2].x(), quad[2].y(), quad[2].z() + offset).color(argb).next();
        context.draw();
    }
}
