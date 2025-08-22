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
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ModelAnalyser extends VertexRecorder {
    private static final Set<RenderType> UNFOCUSABLE_RENDER_TYPES = Set.of(RenderType.armorEntityGlint(), RenderType.glintTranslucent(), RenderType.glint(), RenderType.entityGlint(), RenderType.entityGlintDirect());
    private static final int primitiveArgb = 0x6F3333CC, forwardArgb = 0xFF00CC00, upwardArgb = 0xFFCC0000, leftArgb = 0xFF0000CC;
    private static final int focusedArgb = 0x7FFFFFFF, sideArgb = 0x3FFFFFFF;
    private static final int zOffset1 = 256, zOffset2 = 2 * zOffset1;
    public final PoseStack modelPose = new PoseStack(), texturePose = new PoseStack();
    private BindingContext bindingContext = BindingContext.EMPTY;
    private BindingTarget target = BindingTarget.EMPTY;
    @Nullable
    private BuiltRecord focusedRecord, currentRecord;
    private int focusedIndex = -1, modelScale;

    private static boolean intersects(VertexData[] p1, List<VertexData[]> primitives) {
        final float precision = 1.0E-05f;
        for (VertexData[] p2 : primitives) for (VertexData v1 : p1) for (VertexData v2 : p2) if (v1.pos().distanceToSqr(v2.pos()) < precision) return true;
        return false;
    }

    public void setup(BindingTarget target, int modelScale) {
        this.target = target;
        modelPose.setIdentity();
        texturePose.setIdentity();
        bindingContext = BindingContext.EMPTY;
        focusedRecord = currentRecord = null;
        focusedIndex = -1;
        this.modelScale = modelScale;
    }

    public String getFocusedTextureId() {
        return focusedRecord == null ? null : focusedRecord.textureId();
    }

    public VertexData[] getFocusedPrimitive() {
        return focusedIndex == -1 || focusedRecord == null ? new VertexData[0] : focusedRecord.primitives()[focusedIndex];
    }

    public void computeFocusedInEntity(int mouseX, int mouseY, int layers) {
        if (focusedRecord != null && focusedIndex > -1 || mouseX == -1) return;
        List<Triple> sortByDepth = new ArrayList<>();
        records.stream().filter(record -> !UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())).forEach(record -> {
            VertexData[][] primitives = record.primitives();
            for (int i = 0, primitiveCount = primitives.length; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                int length = primitive.length;
                int[] xs = new int[length], ys = new int[length];
                for (int j = 0; j < length; j++) {
                    xs[j] = (int) primitive[j].x();
                    ys[j] = (int) primitive[j].y();
                }
                if (!new Polygon(xs, ys, length).contains(mouseX, mouseY)) continue;
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

    public void drawLayers(GuiGraphics graphics, ScreenRectangle modelViewArea, @Nullable ScreenRectangle textureViewArea, boolean drawSelected, boolean drawTarget) {
        if (drawSelected) drawFocusedPolyhedron(graphics, modelViewArea, textureViewArea);
        graphics.enableScissor(modelViewArea.left(), modelViewArea.top(), modelViewArea.right(), modelViewArea.bottom());
        if (drawTarget) drawBindingTarget(graphics);
        else drawCameraDirections(graphics);
        graphics.disableScissor();
    }

    public void drawModel(GuiGraphics graphics, Map<String, Set<String>> hiddenNameMap, PoseStack poseStack) {
        Set<String> hiddenNames = hiddenNameMap.getOrDefault(target.name(), Set.of());
        poseStack.pushPose();
        poseStack.mulPose(modelPose.last().pose().invert(new Matrix4f()));
        Matrix4f positionMatrix = new Matrix4f().scale(1, 1, 0.1f).mul(poseStack.last().pose());
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
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
        poseStack.popPose();
        graphics.flush();
    }

    public void drawTextureAndComputeFocused(GuiGraphics graphics, String textureId, int mouseX, int mouseY, PoseStack poseStack) {
        Matrix4f positionMatrix1 = texturePose.last().pose();
        poseStack.pushPose();
        poseStack.mulPose(positionMatrix1.invert(new Matrix4f()));
        Matrix4f positionMatrix2 = new Matrix4f().mul(poseStack.last().pose());
        records.forEach(record -> {
            if (textureId.isBlank() || !record.textureId().contains(textureId)) return;
            VertexConsumer buffer = graphics.bufferSource().getBuffer(record.renderType());
            VertexData[][] primitives = record.primitives();
            for (int i = 0, primitiveCount = primitives.length; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                int length = primitive.length;
                int[] xs = new int[length], ys = new int[length];
                for (int j = 0; j < length; j++) {
                    VertexData vertex = primitive[j];
                    Vector3f position = new Vector3f(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix1);
                    xs[j] = (int) position.x();
                    ys[j] = (int) position.y();
                    position.mulPosition(positionMatrix2);
                    buffer.addVertex(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
                }
                if (mouseX != -1 && new Polygon(xs, ys, length).contains(mouseX, mouseY)) {
                    focusedRecord = record;
                    focusedIndex = i;
                }
            }
        });
        poseStack.popPose();
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
        records.clear();
        catcher.sendVertices(this);
        Lighting.setupFor3DItems();
    }

    @Override
    public BindingContext genContext() {
        target.offsets().setScale(target.offsets().getScale() * modelScale);
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

    private void drawPrimitive(GuiGraphics graphics, VertexData[] primitive, int z, int argb) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (VertexData vertex : primitive) buffer.addVertex(vertex.x(), vertex.y(), z).setColor(argb);
        if (primitive.length == 3) buffer.addVertex(primitive[2].x(), primitive[2].y(), z).setColor(argb);
        graphics.flush();
    }

    private void drawNormal(GuiGraphics graphics, Vec3 start, Vec3 normal, int length, int argb) {
        Vec3 end = normal.scale(length).add(start);
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.lineStrip());
        buffer.addVertex((float) start.x(), (float) start.y(), zOffset2).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        buffer.addVertex((float) end.x(), (float) end.y(), zOffset2).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        graphics.flush();
    }

    private void drawCameraDirections(GuiGraphics graphics) {
        Vec3 start = bindingContext.getPosition();
        Matrix3f normal = bindingContext.normal;
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        drawNormal(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()), modelScale / 3, forwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()), modelScale / 6, upwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()), modelScale / 6, leftArgb);
    }

    private void drawBindingTarget(GuiGraphics graphics) {
        if (currentRecord == null) return;
        TargetConfig config = target.targetConfig();
        currentRecord.findPrimitive(config.posU(), config.posV()).ifPresent(primitive -> drawPrimitive(graphics, primitive, zOffset2, primitiveArgb));
        currentRecord.findPrimitive(config.forwardU(), config.forwardV()).ifPresent(primitive -> drawNormal(graphics, getPosition(primitive, config.forwardU(), config.forwardV()), primitive[0].normal(), modelScale / 2, forwardArgb));
        currentRecord.findPrimitive(config.upwardU(), config.upwardV()).ifPresent(primitive -> drawNormal(graphics, getPosition(primitive, config.upwardU(), config.upwardV()), primitive[0].normal(), modelScale / 2, upwardArgb));
    }

    private void drawFocusedPolyhedron(GuiGraphics graphics, ScreenRectangle modelViewArea, @Nullable ScreenRectangle textureViewArea) {
        if (focusedIndex == -1 || focusedRecord == null) return;
        VertexData[][] primitives = focusedRecord.primitives();
        VertexData[] focused = primitives[focusedIndex];
        VertexData[] reversedFocus = new VertexData[focused.length];
        for (int i = 0; i < focused.length; i++) reversedFocus[i] = focused[focused.length - 1 - i];
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
        graphics.enableScissor(modelViewArea.left(), modelViewArea.top(), modelViewArea.right(), modelViewArea.bottom());
        drawPrimitive(graphics, focused, zOffset2, focusedArgb);
        drawPrimitive(graphics, reversedFocus, zOffset2, focusedArgb);
        resultIndexes.forEach(i -> drawPrimitive(graphics, primitives[i], zOffset1, sideArgb));
        graphics.disableScissor();
        if (textureViewArea == null) return;
        Matrix4f positionMatrix = texturePose.last().pose();
        for (int i = 0; i < focused.length; i++) {
            VertexData vertex = focused[i];
            Vector3f position = new Vector3f(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
            focused[i] = reversedFocus[focused.length - 1 - i] = new VertexData(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
        }
        graphics.enableScissor(textureViewArea.left(), textureViewArea.top(), textureViewArea.right(), textureViewArea.bottom());
        drawPrimitive(graphics, focused, 0, focusedArgb);
        drawPrimitive(graphics, reversedFocus, 0, focusedArgb);
        resultIndexes.forEach(i -> {
            VertexData[] primitive = primitives[i], reversed = new VertexData[primitive.length];
            for (int j = 0; j < primitive.length; j++) {
                VertexData vertex = primitive[j];
                Vector3f position = new Vector3f(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                primitive[j] = reversed[primitive.length - 1 - j] = new VertexData(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            }
            drawPrimitive(graphics, primitive, 0, sideArgb);
            drawPrimitive(graphics, reversed, 0, sideArgb);
        });
        graphics.disableScissor();
    }

    record Triple(double depth, BuiltRecord record, int index) {}
}
