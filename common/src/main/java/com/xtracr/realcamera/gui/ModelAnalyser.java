package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.util.Triple;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ModelAnalyser extends VertexRecorder {
    BuiltRecord focusedRecord;

    public String focusedRenderLayerName() {
        if (focusedRecord == null) return null;
        return focusedRecord.renderLayer().toString();
    }

    public int getFocusedIndex(int mouseX, int mouseY, int layers) {
        List<Triple<Float, BuiltRecord, Integer>> sortByDepth = new ArrayList<>();
        for (BuiltRecord record : records) for (int i = 0; i < record.quadCount(); i++) {
            Vertex[] quad = record.vertices()[i];
            Polygon polygon = new Polygon();
            for (Vertex vertex : quad) polygon.addPoint((int) vertex.x(), (int) vertex.y());
            Vertex point = quad[0];
            Vector3f normal = new Vector3f(point.normalX(), point.normalY(), point.normalZ());
            Vector3f mousePos = new Vector3f((float) point.x(), (float) point.y(), (float) point.z());
            float deltaZ = 0;
            if (normal.z() != 0) deltaZ = (normal.x() * (mouseX - mousePos.x()) + normal.y() * (mouseY - mousePos.y())) / normal.z();
            if (polygon.contains(mouseX, mouseY)) sortByDepth.add(new Triple<>(mousePos.z() + deltaZ, record, i));
        }
        if (sortByDepth.isEmpty()) return -1;
        sortByDepth.sort(Comparator.comparingDouble(triple -> -triple.getLeft()));
        Triple<Float, BuiltRecord, Integer> result = sortByDepth.get(Math.min(sortByDepth.size() - 1, layers));
        focusedRecord = result.getMiddle();
        return result.getRight();
    }

    public void drawQuad(DrawContext context, int quadIndex, int argb, boolean drawFocused) {
        BuiltRecord record = drawFocused ? focusedRecord : currentRecord;
        if (record == null || quadIndex >= record.quadCount()) return;
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

    public void drawNormal(DrawContext context, int quadIndex, int length, int argb) {
        if (quadIndex >= quadCount()) return;
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getLineStrip());
        Vector3f normal = getNormal(quadIndex).toVector3f();
        Vector3f start = getCenter(quadIndex).toVector3f();
        Vector3f end = new Vector3f(normal).mul(length).add(start);
        vertexConsumer.vertex(start.x(), start.y(), start.z() + 1200f).color(argb).normal(normal.x(), normal.y(), normal.z()).next();
        vertexConsumer.vertex(end.x(), end.y(), end.z() + 1200f).color(argb).normal(normal.x(), normal.y(), normal.z()).next();
        context.draw();
    }

    private static boolean intersects(Vertex[] quad, List<Vertex[]> quads) {
        final float precision = 0.00001f;
        boolean ret = false;
        for (Vertex[] p : quads) for (Vertex v1 : quad)
            if (Arrays.stream(p).anyMatch(v2 -> v1.pos().squaredDistanceTo(v2.pos()) < precision)) ret = true;
        return ret;
    }

    private static void drawQuad(DrawContext context, Vertex[] quad, int argb, int offset) {
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        for (Vertex vertex : quad) {
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z() + offset).color(argb).next();
        }
        context.draw();
    }
}
