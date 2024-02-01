package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.util.VertexDataCatcher;
import com.xtracr.realcamera.util.VertexDataCatcherProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Pair;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ModelAnalyser extends VertexDataCatcherProvider {
    private final float precision = 0.00001f;
    private final VertexDataCatcher catcher = new VertexDataCatcher();
    private final List<List<Integer>> quads = new ArrayList<>();

    public List<Integer> getQuad(int index) {
        List<Integer> ret = new ArrayList<>();
        for (List<Integer> quad : quads) {
            if (!quad.contains(index)) continue;
            ret = quad;
            break;
        }
        return ret;
    }

    public void analyse() {
        getUnion(catcher);
        int size = catcher.vertexCount();
        quads.add(new ArrayList<>(List.of(0)));
        for (int i = 1; i < size; i++) {
            double dotProduct = catcher.getNormal(i - 1).dotProduct(catcher.getNormal(i));
            if (dotProduct >=  1 - precision) quads.get(quads.size() - 1).add(i);
            else quads.add(new ArrayList<>(List.of(i)));
        }
    }

    public int getFocusedIndex(int mouseX, int mouseY, int layers) {
        List<Pair<Integer, Float>> sortByDepth = new ArrayList<>();
        for (List<Integer> vertices : quads) {
            Polygon quad = new Polygon();
            vertices.forEach(i -> quad.addPoint((int) catcher.getPos(i).getX(), (int) catcher.getPos(i).getY()));
            Vector3f normal = catcher.getNormal(vertices.get(0)).toVector3f();
            Vector3f point = catcher.getPos(vertices.get(0)).toVector3f();
            float deltaZ = 0;
            if (normal.z() != 0) deltaZ = (normal.x() * (mouseX - point.x()) + normal.y() * (mouseY - point.y())) / normal.z();
            if (quad.contains(mouseX, mouseY)) sortByDepth.add(new Pair<>(vertices.get(0), point.z() + deltaZ));
        }
        if (sortByDepth.isEmpty()) return -1;
        sortByDepth.sort(Comparator.comparingDouble(pair -> -pair.getRight()));
        return sortByDepth.get(Math.min(sortByDepth.size() - 1, layers)).getLeft();
    }

    public void drawQuad(DrawContext context, int vertex, int argb) {
        if (vertex >= catcher.vertexCount()) return;
        drawQuad(context, getQuad(vertex), argb, 1000);
    }

    public void drawPolyhedron(DrawContext context, int vertex, int argb) {
        if (vertex >= catcher.vertexCount()) return;
        List<Integer> highlight = getQuad(vertex);
        List<List<Integer>> polyhedron = new ArrayList<>(List.of(highlight));
        boolean added;
        do {
            added = false;
            for (List<Integer> quad : quads) {
                if (polyhedron.contains(quad) | !intersects(quad, polyhedron)) continue;
                polyhedron.add(quad);
                added = true;
            }
        } while (added);
        List<Integer> indexes = polyhedron.stream().map(quads::indexOf).sorted().toList();
        int index = quads.indexOf(highlight), size = quads.size();
        List<Integer> resultIndexes = new ArrayList<>(List.of(index));
        for (int i = index + 1; i < size; i++) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        for (int i = index - 1; i >= 0; i--) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        resultIndexes.forEach(i -> drawQuad(context, quads.get(i), argb, 1000));
        drawQuad(context, highlight, argb, 1100);
        Collections.reverse(highlight = new ArrayList<>(highlight));
        drawQuad(context, highlight, argb, 1100);
    }

    public void drawNormal(DrawContext context, int vertex, int length, int argb) {
        if (vertex >= catcher.vertexCount()) return;
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getLineStrip());
        Vector3f normal = catcher.getNormal(vertex).toVector3f();
        Vector3f start = new Vector3f();
        List<Integer> quad = getQuad(vertex);
        quad.forEach(i -> start.add(catcher.getPos(i).toVector3f()));
        start.mul(1 / (float) quad.size());
        Vector3f end = new Vector3f(normal).mul(length).add(start);
        vertexConsumer.vertex(start.x(), start.y(), start.z() + 1200f).color(argb).normal(normal.x(), normal.y(), normal.z()).next();
        vertexConsumer.vertex(end.x(), end.y(), end.z() + 1200f).color(argb).normal(normal.x(), normal.y(), normal.z()).next();
        context.draw();
    }

    private boolean intersects(List<Integer> quad, List<List<Integer>> quads) {
        boolean ret = false;
        for (List<Integer> p : quads) for (int v1 : quad) for (int v2 : p) {
            if (catcher.getPos(v1).squaredDistanceTo(catcher.getPos(v2)) < precision) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    private void drawQuad(DrawContext context, List<Integer> quad, int argb, int offset) {
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        quad.stream().map(catcher::getPos).forEach(pos -> vertexConsumer.vertex(pos.getX(), pos.getY(), pos.getZ() + offset).color(argb).next());
        context.draw();
    }
}
