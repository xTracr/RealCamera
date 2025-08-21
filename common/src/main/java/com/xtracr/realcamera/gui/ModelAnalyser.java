package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.BindingTarget.*;
import com.xtracr.realcamera.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.*;
import java.util.List;


public class ModelAnalyser extends VertexRecorder {
    private static final Set<RenderType> UNFOCUSABLE_RENDER_TYPES = Set.of(RenderType.armorEntityGlint(), RenderType.glintTranslucent(), RenderType.glint(), RenderType.entityGlint(), RenderType.entityGlintDirect());
    private static final int primitiveArgb = 0x6F3333CC, forwardArgb = 0xFF00CC00, upwardArgb = 0xFFCC0000, leftArgb = 0xFF0000CC;
    private static final int focusedArgb = 0x7FFFFFFF, sideArgb = 0x3FFFFFFF;
    private BindingContext bindingContext = BindingContext.EMPTY;
    private BindingTarget target = BindingTarget.EMPTY;
    @Nullable
    private BuiltRecord focusedRecord, currentRecord;
    private int focusedIndex = -1, entityScale, offsetZ;

    private static boolean intersects(VertexData[] p1, List<VertexData[]> primitives) {
        final float precision = 1.0E-05f;
        for (VertexData[] p2 : primitives) for (VertexData v1 : p1) for (VertexData v2 : p2) if (v1.pos().distanceToSqr(v2.pos()) < precision) return true;
        return false;
    }

    public void setup(BindingTarget target, int entityScale, int offsetZ) {
        this.target = target;
        bindingContext = BindingContext.EMPTY;
        focusedRecord = currentRecord = null;
        focusedIndex = -1;
        this.entityScale = entityScale;
        this.offsetZ = offsetZ / 2;
    }

    public String getFocusedTextureId() {
        if (focusedRecord == null) return null;
        return focusedRecord.textureId();
    }

    public Vec2 getFocusedUV() {
        if (focusedIndex == -1 || focusedRecord == null) return null;
        float u = 0, v = 0;
        VertexData[] primitive = focusedRecord.primitives()[focusedIndex];
        for (VertexData vertex : primitive) {
            u += vertex.u();
            v += vertex.v();
        }
        return new Vec2(u / primitive.length, v / primitive.length);
    }

    public void previewEffect(GuiGraphics graphics, boolean canSelect) {
        if (canSelect) drawFocused(graphics);
        Vec3 start = bindingContext.getPosition();
        Matrix3f normal = bindingContext.normal;
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        drawNormal(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()), entityScale / 3, forwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()), entityScale / 6, upwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()), entityScale / 6, leftArgb);
    }

    public void drawSelected(GuiGraphics graphics) {
        drawFocusedPolyhedron(graphics);
        drawFocused(graphics);
        if (currentRecord == null) return;
        TargetConfig config = target.targetConfig();
        currentRecord.findPrimitive(config.posU(), config.posV()).ifPresent(primitive -> drawPrimitive(graphics, primitive, primitiveArgb));
        currentRecord.findPrimitive(config.forwardU(), config.forwardV()).ifPresent(primitive -> drawNormal(graphics, getPosition(primitive, config.forwardU(), config.forwardV()), primitive[0].normal(), entityScale / 2, forwardArgb));
        currentRecord.findPrimitive(config.upwardU(), config.upwardV()).ifPresent(primitive -> drawNormal(graphics, getPosition(primitive, config.upwardU(), config.upwardV()), primitive[0].normal(), entityScale / 2, upwardArgb));
    }

    public void analyse(int mouseX, int mouseY, int layers) {
        target.offsets().setScale(target.offsets().getScale() * entityScale);
        genContext();
        List<Triple> sortByDepth = new ArrayList<>();
        records.stream().filter(record -> !UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())).forEach(record -> {
            VertexData[][] primitives = record.primitives();
            for (int i = 0, primitiveCount = primitives.length; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                int[] xs = new int[primitive.length], ys = new int[primitive.length];
                for (int j = 0; j < primitive.length; j++) {
                    xs[j] = (int) primitive[j].x();
                    ys[j] = (int) primitive[j].y();
                }
                if (!new Polygon(xs, ys, primitive.length).contains(mouseX, mouseY)) continue;
                VertexData point = primitive[0];
                double deltaZ = point.normalZ() == 0 ? 0 : (point.normalX() * (mouseX - point.x()) + point.normalY() * (mouseY - point.y())) / point.normalZ();
                sortByDepth.add(new Triple(point.z() - deltaZ, record, i));
            }
        });
        if (!sortByDepth.isEmpty()) {
            sortByDepth.sort(Comparator.comparingDouble(triple -> -triple.depth));
            Triple result = sortByDepth.get(Math.min(sortByDepth.size() - 1, layers));
            focusedRecord = result.record;
            focusedIndex = result.index;
        }
    }

    public void drawModel(GuiGraphics graphics, Map<String, Set<String>> hiddenNameMap, int guiScale) {
        Set<String> hiddenNames = hiddenNameMap.getOrDefault(target.name(), Set.of());
        float scaledOffsetZ = guiScale * 2 * offsetZ;
        Matrix4f positionMatrix = new Matrix4f().translate(0, 0, scaledOffsetZ);
        Matrix3f normalMatrix = new Matrix3f();
        records().forEach(record -> {
            DisableConfig[] disableConfigs = target.filteredDisableConfigs(config -> record.textureId().contains(config.textureId()) && hiddenNames.contains(config.name()));
            for (DisableConfig disableConfig : disableConfigs) {
                if (disableConfig.disableAll()) return;
            }
            VertexConsumer buffer = graphics.bufferSource().getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                VertexData.renderVertices(record.vertices(), buffer);
                return;
            }
            for (VertexData[] primitive : record.primitives()) {
                outer:
                for (VertexData vertex : primitive) {
                    for (DisableConfig config : disableConfigs) {
                        if (config.test(vertex)) continue outer;
                    }
                    VertexData.renderVertices(primitive, buffer, positionMatrix, normalMatrix);
                    break;
                }
            }
        });
        graphics.flush();
    }

    @Override
    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        Lighting.setupForEntityInInventory();
        MultiVertexCatcher catcher = new SimpleMultiVertexCatcher();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0, 0, 0, 0, deltaTick, poseStack, catcher, 0xF000f0);
        dispatcher.setRenderShadow(true);
        catcher.sendVertices(this);
        Lighting.setupFor3DItems();
    }

    @Override
    public BindingContext genContext() {
        for (BuiltRecord record : records) {
            BindingContext context = new BindingContext(target, true);
            record.setupContext(context);
            if (context.weakAvailable()) currentRecord = record;
            if (!context.available()) continue;
            bindingContext = context;
            currentRecord = record;
            bindingContext.init();
            return context;
        }
        return BindingContext.EMPTY;
    }

    private void drawPrimitive(GuiGraphics graphics, VertexData[] primitive, int argb) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (VertexData vertex : primitive) buffer.addVertex(vertex.x(), vertex.y(), vertex.z() + offsetZ).setColor(argb);
        if (primitive.length == 3) buffer.addVertex(primitive[2].x(), primitive[2].y(), primitive[2].z() + offsetZ).setColor(argb);
        graphics.flush();
    }

    private void drawNormal(GuiGraphics graphics, Vec3 start, Vec3 normal, int length, int argb) {
        Vec3 end = normal.scale(length).add(start);
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.lineStrip());
        buffer.addVertex((float) start.x(), (float) start.y(), (float) (start.z() + offsetZ)).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        buffer.addVertex((float) end.x(), (float) end.y(), (float) (end.z() + offsetZ)).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        graphics.flush();
    }

    private void drawFocused(GuiGraphics graphics) {
        if (focusedIndex == -1 || focusedRecord == null) return;
        VertexData[] focused = focusedRecord.primitives()[focusedIndex];
        drawPrimitive(graphics, focused, focusedArgb);
        int length = focused.length;
        VertexData[] reversed = new VertexData[length];
        for (int i = 0; i < length; i++) reversed[i] = focused[length - 1 - i];
        drawPrimitive(graphics, reversed, focusedArgb);
    }

    private void drawFocusedPolyhedron(GuiGraphics graphics) {
        if (focusedIndex == -1 || focusedRecord == null) return;
        List<VertexData[]> polyhedron = new ArrayList<>();
        polyhedron.add(focusedRecord.primitives()[focusedIndex]);
        List<Integer> indexes = new ArrayList<>(List.of(focusedIndex));
        VertexData[][] primitives = focusedRecord.primitives();
        final int primitiveCount = primitives.length;
        boolean added;
        do {
            added = false;
            for (int i = 0; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                if (indexes.contains(i) | !intersects(primitive, polyhedron)) continue;
                polyhedron.add(primitive);
                indexes.add(i);
                added = true;
            }
        } while (added);
        List<Integer> resultIndexes = new ArrayList<>(List.of(focusedIndex));
        for (int i = focusedIndex + 1; i < primitiveCount; i++) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        for (int i = focusedIndex - 1; i >= 0; i--) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        resultIndexes.forEach(i -> drawPrimitive(graphics, primitives[i], sideArgb));
    }

    record Triple(double depth, BuiltRecord record, int index) { }
}
