package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ModelAnalyser extends VertexRecorder {
    private static final Set<RenderLayer> unfocusableLayers = Set.of(RenderLayer.getArmorGlint(), RenderLayer.getArmorEntityGlint(), RenderLayer.getGlintTranslucent(), RenderLayer.getGlint(), RenderLayer.getDirectGlint(), RenderLayer.getEntityGlint(), RenderLayer.getDirectEntityGlint());
    private static final int quadArgb = 0x6F3333CC, forwardArgb = 0xFF00CC00, upwardArgb = 0xFFCC0000, leftArgb = 0xFF0000CC;
    private static final int focusedArgb = 0x7FFFFFFF, sideArgb = 0x3FFFFFFF;
    private final BindingTarget target;
    private final Matrix3f normal = new Matrix3f();
    private final Vector3f position = new Vector3f();
    @Nullable
    private BuiltRecord focusedRecord, currentRecord;
    private int focusedIndex = -1;

    public ModelAnalyser(BindingTarget target) {
        this.target = target;
    }

    private static boolean intersects(Vertex[] quad, List<Vertex[]> quads) {
        final float precision = 1.0E-05f;
        for (Vertex[] q : quads) for (Vertex v1 : quad) for (Vertex v2 : q) if (v1.pos().squaredDistanceTo(v2.pos()) < precision) return true;
        return false;
    }

    private static void drawQuad(DrawContext context, Vertex[] quad, int argb, int offset) {
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        for (Vertex vertex : quad) vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z() + offset).color(argb).next();
        if (quad.length == 3) vertexConsumer.vertex(quad[2].x(), quad[2].y(), quad[2].z() + offset).color(argb).next();
        context.draw();
    }

    private static void drawNormal(DrawContext context, Vec3d start, Vec3d normal, int length, int argb) {
        Vec3d end = normal.multiply(length).add(start);
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getLineStrip());
        vertexConsumer.vertex(start.getX(), start.getY(), start.getZ() + 1200f).color(argb).normal((float) normal.getX(), (float) normal.getY(), (float) normal.getZ()).next();
        vertexConsumer.vertex(end.getX(), end.getY(), end.getZ() + 1200f).color(argb).normal((float) normal.getX(), (float) normal.getY(), (float) normal.getZ()).next();
        context.draw();
    }

    public void initialize(int entitySize, int mouseX, int mouseY, int layers, boolean hideDisabled, String idInField) {
        buildRecords();
        List<BuiltRecord> removedRecords = records.stream().filter(record -> {
            boolean isIdInField = !idInField.isBlank() && record.textureId().contains(idInField);
            return (hideDisabled && isIdInField) || (!isIdInField && target.disabledTextureIds.stream().anyMatch(record.textureId()::contains));
        }).toList();
        records.removeAll(removedRecords);
        List<Triple> sortByDepth = new ArrayList<>();
        records.stream().filter(record -> !unfocusableLayers.contains(record.renderLayer())).forEach(record -> {
            Vertex[][] vertices = record.vertices();
            for (int i = 0, size = vertices.length; i < size; i++) {
                Polygon polygon = new Polygon();
                Vertex[] quad = vertices[i];
                for (Vertex vertex : quad) polygon.addPoint((int) vertex.x(), (int) vertex.y());
                if (!polygon.contains(mouseX, mouseY)) continue;
                Vertex point = quad[0];
                double deltaZ = 0;
                if (point.normalZ() != 0) deltaZ = (point.normalX() * (mouseX - point.x()) + point.normalY() * (mouseY - point.y())) / point.normalZ();
                sortByDepth.add(new Triple(point.z() + deltaZ, record, i));
            }
        });
        if (!sortByDepth.isEmpty()) {
            sortByDepth.sort(Comparator.comparingDouble(triple -> -triple.depth));
            Triple result = sortByDepth.get(Math.min(sortByDepth.size() - 1, layers));
            focusedRecord = result.record;
            focusedIndex = result.index;
        }
        target.scale *= entitySize;
        records.addAll(removedRecords);
        currentRecord = getTargetPosAndRot(target, normal, position);
        records.removeAll(removedRecords);
        normal.rotateLocal((float) Math.toRadians(target.getYaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal((float) Math.toRadians(target.getPitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal((float) Math.toRadians(target.getRoll()), normal.m20, normal.m21, normal.m22);
    }

    public String focusedTextureId() {
        if (focusedRecord == null) return null;
        return focusedRecord.textureId();
    }

    public Vec2f getFocusedUV() {
        if (focusedIndex == -1 || focusedRecord == null) return null;
        float u = 0, v = 0;
        Vertex[] quad = focusedRecord.vertices()[focusedIndex];
        for (Vertex vertex : quad) {
            u += vertex.u();
            v += vertex.v();
        }
        return new Vec2f(u / quad.length, v / quad.length);
    }

    public void previewEffect(DrawContext context, int entitySize, boolean canSelect) {
        drawByAnother(context.getVertexConsumers(), BuiltRecord::vertices);
        context.draw();
        if (canSelect) drawFocused(context);
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        Vec3d start = new Vec3d(position);
        drawNormal(context, start, new Vec3d(normal.m20(), normal.m21(), -normal.m22()), entitySize / 3, forwardArgb);
        drawNormal(context, start, new Vec3d(normal.m10(), normal.m11(), -normal.m12()), entitySize / 6, upwardArgb);
        drawNormal(context, start, new Vec3d(normal.m00(), normal.m01(), -normal.m02()), entitySize / 6, leftArgb);
    }

    public void drawModelWithNormals(DrawContext context, int entitySize) {
        drawByAnother(context.getVertexConsumers(), BuiltRecord::vertices);
        context.draw();
        drawPolyhedron(context);
        drawFocused(context);
        if (currentRecord == null) return;
        Vertex[] quad;
        if ((quad = getQuad(currentRecord, target.posU, target.posV)) != null) drawQuad(context, quad, quadArgb, 1000);
        if ((quad = getQuad(currentRecord, target.forwardU, target.forwardV)) != null) drawNormal(context, getPos(quad, target.forwardU, target.forwardV), quad[0].normal(), entitySize / 2, forwardArgb);
        if ((quad = getQuad(currentRecord, target.upwardU, target.upwardV)) != null) drawNormal(context, getPos(quad, target.upwardU, target.upwardV), quad[0].normal(), entitySize / 2, upwardArgb);
    }

    private void drawFocused(DrawContext context) {
        if (focusedIndex == -1 || focusedRecord == null) return;
        Vertex[] focused = focusedRecord.vertices()[focusedIndex];
        drawQuad(context, focused, focusedArgb, 1100);
        int size = focused.length;
        Vertex[] reversed = new Vertex[size];
        for (int i = 0; i < size; i++) reversed[i] = focused[size - 1 - i];
        drawQuad(context, reversed, focusedArgb, 1100);
    }

    private void drawPolyhedron(DrawContext context) {
        if (focusedIndex == -1 || focusedRecord == null) return;
        List<Vertex[]> polyhedron = new ArrayList<>();
        polyhedron.add(focusedRecord.vertices()[focusedIndex]);
        List<Integer> indexes = new ArrayList<>(List.of(focusedIndex));
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
        List<Integer> resultIndexes = new ArrayList<>(List.of(focusedIndex));
        for (int i = focusedIndex + 1; i < size; i++) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        for (int i = focusedIndex - 1; i >= 0; i--) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        resultIndexes.forEach(i -> drawQuad(context, vertices[i], sideArgb, 1000));
    }

    record Triple(double depth, BuiltRecord record, int index) {}
}
