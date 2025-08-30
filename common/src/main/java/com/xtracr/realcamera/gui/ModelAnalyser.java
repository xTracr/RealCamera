package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.BindingTarget.DisableConfig;
import com.xtracr.realcamera.config.BindingTarget.TargetConfig;
import com.xtracr.realcamera.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ModelAnalyser extends VertexRecorder {
    private static final Set<RenderType> UNFOCUSABLE_RENDER_TYPES = Set.of(RenderType.armorEntityGlint(), RenderType.glintTranslucent(), RenderType.glint(), RenderType.entityGlint(), RenderType.entityGlintDirect());
    private static final int planeArgb = 0x6F3333CC, forwardArgb = 0xFF00CC00, upwardArgb = 0xFFCC0000, leftArgb = 0xFF0000CC, focusedArgb = 0x4FFFFFFF;
    private static final int z1 = 210, z2 = z1 + 10;
    public final List<VertexData[]> focusedPolyhedron = new ArrayList<>();
    public final PoseStack modelPose = new PoseStack(), texturePose = new PoseStack();
    private final List<BuiltRecord> textureRecords = new ArrayList<>();
    private BindingContext bindingContext = BindingContext.EMPTY;
    private BindingTarget target = BindingTarget.EMPTY;
    @Nullable
    private BuiltRecord focusedRecord, currentRecord;
    private int focusedIndex = -1, modelScale;
    private float minEntityZ, maxEntityZ;

    private static boolean intersects(VertexData[] p1, List<VertexData[]> primitives) {
        final float precision = 1.0E-05f;
        for (VertexData[] p2 : primitives) for (VertexData v1 : p1) for (VertexData v2 : p2) if (v1.pos().distanceToSqr(v2.pos()) < precision) return true;
        return false;
    }

    public void setup(BindingTarget target, int modelScale) {
        this.target = target;
        this.modelScale = modelScale;
        textureRecords.clear();
        focusedPolyhedron.clear();
        modelPose.setIdentity();
        texturePose.setIdentity();
        bindingContext = BindingContext.EMPTY;
        focusedRecord = currentRecord = null;
        focusedIndex = -1;
    }

    public String getFocusedTextureId() {
        return focusedRecord == null ? null : focusedRecord.textureId();
    }

    public void applyDisableConfigs(String textureId, Set<String> hiddenNames) {
        records.forEach(record -> {
            if (!record.textureId().contains(textureId)) return;
            textureRecords.add(record);
        });
        for (int i = 0; i < records.size(); i++) {
            BuiltRecord record = records.get(i);
            List<VertexData[]> primitives = new ArrayList<>();
            DisableConfig[] disableConfigs = target.filteredDisableConfigs(config -> record.textureId().contains(config.textureId()) && hiddenNames.contains(config.name()));
            boolean disableAll = !record.textureId().contains(textureId);
            for (DisableConfig config : disableConfigs) {
                if (config.disableAll()) {
                    disableAll = true;
                    break;
                }
            }
            if (!disableAll) for (VertexData[] primitive : record.primitives()) {
                outer:
                for (VertexData vertex : primitive) {
                    for (DisableConfig config : disableConfigs) {
                        if (config.test(vertex)) continue outer;
                    }
                    primitives.add(primitive);
                    break;
                }
            }
            records.set(i, new BuiltRecord(record.renderType(), record.textureId(), record.vertices(), primitives.toArray(VertexData[][]::new)));
        }
    }

    public void computeFocusedOnTexture(int mouseX, int mouseY) {
        Matrix4f positionMatrix = texturePose.last().pose();
        textureRecords.forEach(record -> {
            VertexData[][] primitives = record.primitives();
            for (int i = 0, primitiveCount = primitives.length; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                int length = primitive.length;
                int[] xs = new int[length], ys = new int[length];
                for (int j = 0; j < length; j++) {
                    VertexData vertex = primitive[j];
                    Vector3f position = new Vector3f(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                    xs[j] = (int) position.x();
                    ys[j] = (int) position.y();
                }
                if (mouseX >= 0 && mouseY >= 0 && new Polygon(xs, ys, length).contains(mouseX, mouseY)) {
                    focusedRecord = record;
                    focusedIndex = i;
                    return;
                }
            }
        });
    }

    public void computeFocusedOnModel(int mouseX, int mouseY, int layers) {
        List<Triple> sortByDepth = new ArrayList<>();
        minEntityZ = 0f;
        maxEntityZ = 200f;
        for (BuiltRecord record : records) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            VertexData[][] primitives = record.primitives();
            for (int i = 0, primitiveCount = primitives.length; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                int length = primitive.length;
                int[] xs = new int[length], ys = new int[length];
                for (int j = 0; j < length; j++) {
                    VertexData vertex = primitive[j];
                    xs[j] = (int) vertex.x();
                    ys[j] = (int) vertex.y();
                    if (vertex.z() < minEntityZ) minEntityZ = vertex.z();
                    if (vertex.z() > maxEntityZ) maxEntityZ = vertex.z();
                }
                if (!new Polygon(xs, ys, length).contains(mouseX, mouseY)) continue;
                VertexData point = primitive[0];
                double deltaZ = point.normalZ() == 0 ? 0 : (point.normalX() * (mouseX - point.x()) + point.normalY() * (mouseY - point.y())) / point.normalZ();
                sortByDepth.add(new Triple(point.z() - deltaZ, record, i));
            }
        }
        if (mouseX < 0 || mouseY < 0) return;
        if ((focusedRecord != null && focusedIndex > -1) || sortByDepth.isEmpty()) return;
        sortByDepth.sort(Comparator.comparingDouble(triple -> -triple.depth));
        Triple result = sortByDepth.get(Math.min(sortByDepth.size() - 1, layers));
        focusedRecord = result.record;
        focusedIndex = result.index;
    }

    public void computeFocusedPolyhedron() {
        if (focusedIndex == -1 || focusedRecord == null) return;
        VertexData[][] primitives = focusedRecord.primitives();
        VertexData[] focused = primitives[focusedIndex];
        List<VertexData[]> polyhedron = new ArrayList<>();
        polyhedron.add(focused);
        List<Integer> indexes = new ArrayList<>(List.of(focusedIndex));
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
        focusedPolyhedron.add(focused);
        resultIndexes.forEach(i -> focusedPolyhedron.add(primitives[i]));
    }

    public void drawCameraDirections(GuiGraphics graphics) {
        Vec3 start = bindingContext.getPosition();
        Matrix3f normal = bindingContext.rotation;
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        drawNormal(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()), modelScale / 3, forwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()), modelScale / 6, upwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()), modelScale / 6, leftArgb);
    }

    public void drawBindingTarget(GuiGraphics graphics) {
        if (currentRecord == null) return;
        TargetConfig config = target.targetConfig();
        currentRecord.findPrimitive(config.posU(), config.posV()).ifPresent(primitive -> drawPrimitive(graphics, primitive, z1, planeArgb));
        currentRecord.findPrimitive(config.forwardU(), config.forwardV()).ifPresent(primitive -> drawNormal(graphics, getPosition(primitive, config.forwardU(), config.forwardV()), VertexData.normal(primitive), -modelScale / 2, forwardArgb));
        currentRecord.findPrimitive(config.upwardU(), config.upwardV()).ifPresent(primitive -> drawNormal(graphics, getPosition(primitive, config.upwardU(), config.upwardV()), VertexData.normal(primitive), -modelScale / 2, upwardArgb));
    }

    public void drawFocusedInModelArea(GuiGraphics graphics) {
        if (focusedPolyhedron.isEmpty() || focusedRecord == null) return;
        VertexData[] focused = focusedPolyhedron.getFirst();
        VertexData[] reversed = new VertexData[focused.length];
        for (int i = 0; i < focused.length; i++) reversed[i] = focused[focused.length - 1 - i];
        drawPrimitive(graphics, reversed, z1, focusedArgb);
        focusedPolyhedron.forEach(primitive -> drawPrimitive(graphics, primitive, z1, focusedArgb));
    }

    public void drawFocusedInTextureArea(GuiGraphics graphics) {
        Matrix4f positionMatrix = texturePose.last().pose();
        for (VertexData[] primitive : focusedPolyhedron) {
            VertexData[] transformed = new VertexData[primitive.length], reversed = new VertexData[primitive.length];
            for (int i = 0; i < primitive.length; i++) {
                VertexData vertex = primitive[i];
                Vector3f position = new Vector3f(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                transformed[i] = reversed[primitive.length - 1 - i] = new VertexData(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            }
            drawPrimitive(graphics, transformed, 0, focusedArgb);
            drawPrimitive(graphics, reversed, 0, focusedArgb);
        }
    }

    public void drawModel(GuiGraphics graphics, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(modelPose.last().pose().invert(new Matrix4f()));
        Matrix4f positionMatrix = new Matrix4f().mul(poseStack.last().pose()).scale(1, 1, 200 / (maxEntityZ - minEntityZ)).translate(0, 0, -minEntityZ);
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
        records.forEach(record -> {
            VertexConsumer buffer = graphics.bufferSource().getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                VertexData.renderVertices(record.vertices(), buffer);
                return;
            }
            for (VertexData[] primitive : record.primitives()) {
                VertexData.renderVertices(primitive, buffer, positionMatrix, normalMatrix);
            }
        });
        poseStack.popPose();
        graphics.flush();
    }

    public void drawTexture(GuiGraphics graphics, PoseStack poseStack) {
        Matrix4f positionMatrix = poseStack.last().pose();
        textureRecords.forEach(record -> {
            VertexConsumer buffer = graphics.bufferSource().getBuffer(record.renderType());
            for (VertexData vertex : record.vertices()) {
                Vector3f position = new Vector3f(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                buffer.addVertex(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            }
        });
        graphics.flush();
    }

    @Override
    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        Lighting.setupForEntityInInventory();
        MultiVertexCatcher catcher = MultiVertexCatcher.defaultImpl();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0, 0, 0, 0, deltaTick, poseStack, catcher, 0xF000f0);
        dispatcher.setRenderShadow(true);
        records.clear();
        catcher.sendVertices(this);
        Lighting.setupFor3DItems();
    }

    @Override
    public BindingContext genContext() {
        Matrix4f matrix4f = new Matrix4f();
        Matrix3f matrix3f = new Matrix3f().scale(-1);
        target.offsets().setScale(target.offsets().getScale() * modelScale);
        for (BuiltRecord record : records) {
            BindingContext context = new BindingContext(target, true);
            record.setupContext(context, matrix4f, matrix3f);
            if (context.weakAvailable()) currentRecord = record;
            if (!context.available()) continue;
            bindingContext = context;
            currentRecord = record;
            bindingContext.init();
            return context;
        }
        return BindingContext.EMPTY;
    }

    private void drawPrimitive(GuiGraphics graphics, VertexData[] primitive, int z, int argb) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (VertexData vertex : primitive) buffer.addVertex(vertex.x(), vertex.y(), z).setColor(argb);
        if (primitive.length == 3) buffer.addVertex(primitive[2].x(), primitive[2].y(), z).setColor(argb);
        graphics.flush();
    }

    private void drawNormal(GuiGraphics graphics, Vec3 start, Vec3 normal, int length, int argb) {
        Vec3 end = normal.scale(length).add(start);
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.lineStrip());
        buffer.addVertex((float) start.x(), (float) start.y(), z2).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        buffer.addVertex((float) end.x(), (float) end.y(), z2).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        graphics.flush();
    }

    record Triple(double depth, BuiltRecord record, int index) {}
}
