package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.util.VertexDataCatcher;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Pair;
import org.joml.Vector3f;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModelAnalyser implements VertexConsumerProvider {
    private final VertexDataCatcher catcher = new VertexDataCatcher();
    private final List<List<Integer>> polygons = new ArrayList<>();

    public List<Integer> getPolygon(int index) {
        List<Integer> ret = new ArrayList<>();
        for (List<Integer> polygon : polygons) {
            if (!polygon.contains(index)) continue;
            ret = polygon;
            break;
        }
        return ret;
    }

    public List<Integer> getFocusedPolygon(int mouseX, int mouseY, int layers) {
        final float precision = 0.00001f;
        polygons.add(new ArrayList<>(List.of(0)));
        for (int i = 1; i < catcher.normalRecorder.size(); i++) {
            double dotProduct = catcher.normalRecorder.get(i - 1).dotProduct(catcher.normalRecorder.get(i));
            if (dotProduct >=  1 - precision) polygons.get(polygons.size() - 1).add(i);
            else polygons.add(new ArrayList<>(List.of(i)));
        }
        List<Pair<List<Integer>, Float>> sortByDepth = new ArrayList<>();
        for (List<Integer> vertices : polygons) {
            Polygon polygon = new Polygon();
            for (int index : vertices) {
                Vector3f pos = catcher.posRecorder.get(index).toVector3f();
                polygon.addPoint((int) pos.x(), (int) pos.y());
            }
            Vector3f normal = catcher.normalRecorder.get(vertices.get(0)).toVector3f();
            Vector3f intersection = catcher.posRecorder.get(vertices.get(0)).toVector3f();
            float deltaZ = 0;
            if (normal.z() != 0) deltaZ = (normal.x() * (mouseX - intersection.x()) + normal.y() * (mouseY - intersection.y())) / normal.z();
            if (polygon.contains(mouseX, mouseY)) sortByDepth.add(new Pair<>(vertices, intersection.z() + deltaZ));
        }
        if (sortByDepth.isEmpty()) return null;
        sortByDepth.sort(Comparator.comparingDouble(pair -> -pair.getRight()));
        return sortByDepth.get(Math.min(sortByDepth.size() - 1, layers)).getLeft();
    }

    public void drawPolygon(DrawContext context, int index, int argb) {
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        getPolygon(index).stream().map(i -> catcher.posRecorder.get(i).toVector3f()).forEach(pos -> vertexConsumer.vertex(pos.x(), pos.y(), pos.z() + 1000f).color(argb).next());
        context.draw();
    }

    public void drawNormal(DrawContext context, int index, int size, int argb) {
        if (index >= catcher.posRecorder.size()) return;
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getLineStrip());
        Vector3f normal = catcher.normalRecorder.get(index).toVector3f();
        Vector3f start = new Vector3f();
        List<Integer> polygon = getPolygon(index);
        polygon.forEach(i -> start.add(catcher.posRecorder.get(i).toVector3f()));
        start.mul(1 / (float) polygon.size());
        Vector3f end = new Vector3f(normal).mul(size).add(start);
        vertexConsumer.vertex(start.x(), start.y(), start.z() + 1200f).color(argb).normal(normal.x(), normal.y(), normal.z()).next();
        vertexConsumer.vertex(end.x(), end.y(), end.z() + 1200f).color(argb).normal(normal.x(), normal.y(), normal.z()).next();
        context.draw();
    }
    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return catcher;
    }
}
