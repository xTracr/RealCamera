package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.DisableConfig;
import com.xtracr.realcamera.config.BindTarget.TargetConfig;
import com.xtracr.realcamera.util.BindResult;
import com.xtracr.realcamera.util.MultiVertexCatcher;
import com.xtracr.realcamera.util.VertexData;
import com.xtracr.realcamera.util.VertexRecorder;
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
    private BindResult bindResult = BindResult.EMPTY;
    private BindTarget target = BindTarget.EMPTY;
    @Nullable
    private BuiltRecord focusedRecord, currentRecord;
    private int modelScale;

    private static boolean haveCommonVertex(VertexData[] p1, List<VertexData[]> primitives) {
        final float precision = 1.0E-05f;
        for (VertexData[] p2 : primitives) for (VertexData v1 : p1) for (VertexData v2 : p2) if (v1.pos().distanceToSqr(v2.pos()) < precision) return true;
        return false;
    }

    private static Polygon getPolygon(VertexData[] primitive) {
        int length = primitive.length;
        int[] xs = new int[length], ys = new int[length];
        for (int j = 0; j < length; j++) {
            VertexData vertex = primitive[j];
            xs[j] = (int) vertex.x();
            ys[j] = (int) vertex.y();
        }
        return new Polygon(xs, ys, length);
    }

    public void initialize(BindTarget target, int modelScale) {
        this.target = target;
        this.modelScale = modelScale;
        textureRecords.clear();
        focusedPolyhedron.clear();
        modelPose.setIdentity();
        texturePose.setIdentity();
        bindResult = BindResult.EMPTY;
        focusedRecord = currentRecord = null;
        if (catcher == null) setCatcher(MultiVertexCatcher.defaultImpl());
    }

    public String getFocusedTextureId() {
        return focusedRecord == null ? null : focusedRecord.textureId();
    }

    public void applyDisableConfigs(String textureId, Set<String> hiddenNames) {
        for (BuiltRecord record : records) {
            if (!record.textureId().contains(textureId)) continue;
            textureRecords.add(record);
        }
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
            records.set(i, new BuiltRecord(record.renderType(), record.textureId(), record.vertices(), primitives.toArray(new VertexData[0][])));
        }
    }

    public void computeFocusedOnTexture(int mouseX, int mouseY) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        Matrix4f positionMatrix = texturePose.last().pose();
        for (BuiltRecord record : textureRecords) {
            VertexData[][] primitives = record.primitives();
            for (VertexData[] primitive : primitives) {
                int length = primitive.length;
                int[] xs = new int[length], ys = new int[length];
                for (int j = 0; j < length; j++) {
                    VertexData vertex = primitive[j];
                    Vector3f position = new Vector3f(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                    xs[j] = (int) position.x();
                    ys[j] = (int) position.y();
                }
                if (new Polygon(xs, ys, length).contains(mouseX, mouseY)) {
                    focusedRecord = record;
                    focusedPolyhedron.add(primitive);
                    break;
                }
            }
        }
    }

    public void computeFocusedOnModel(int mouseX, int mouseY, int layers) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        List<Object[]> sortByZ = new ArrayList<>();
        for (BuiltRecord record : records) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            VertexData[][] primitives = record.primitives();
            for (VertexData[] primitive : primitives) {
                if (!getPolygon(primitive).contains(mouseX, mouseY)) continue;
                VertexData vertex = primitive[0];
                float deltaZ = vertex.normalZ() == 0 ? 0 : (vertex.normalX() * (mouseX - vertex.x()) + vertex.normalY() * (mouseY - vertex.y())) / vertex.normalZ();
                sortByZ.add(new Object[]{record, primitive, vertex.z() - deltaZ});
            }
        }
        if (sortByZ.isEmpty()) return;
        sortByZ.sort(Comparator.comparingDouble(array -> -(float) array[2]));
        Object[] result = sortByZ.get(Math.min(sortByZ.size() - 1, layers));
        focusedRecord = (BuiltRecord) result[0];
        focusedPolyhedron.add((VertexData[]) result[1]);
    }

    public void computeFocusedOnModel(int minX, int minY, int maxX, int maxY) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        List<Object[]> sortByZ = new ArrayList<>();
        for (BuiltRecord record : records) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            VertexData[][] primitives = record.primitives();
            outer:
            for (VertexData[] primitive : primitives) {
                float maxZ = primitive[0].z();
                for (VertexData vertex : primitive) {
                    int x = (int) vertex.x(), y = (int) vertex.y();
                    if (x < minX || y < minY || x > maxX || y > maxY) continue outer;
                    if (vertex.z() > maxZ) maxZ = vertex.z();
                }
                sortByZ.add(new Object[]{record, primitive, maxZ});
            }
        }
        if (sortByZ.isEmpty()) return;
        sortByZ.sort(Comparator.comparingDouble(array -> -(float) array[2]));
        List<Polygon> polygons = new ArrayList<>();
        focusedRecord = (BuiltRecord) sortByZ.getFirst()[0];
        while (!sortByZ.isEmpty()) {
            VertexData[] first = (VertexData[]) sortByZ.getFirst()[1];
            focusedPolyhedron.add(first);
            polygons.add(getPolygon(first));
            sortByZ.removeFirst();
            sortByZ.removeIf(array -> {
                if (array[0] != focusedRecord) return true;
                VertexData[] primitive = (VertexData[]) array[1];
                outer:
                for (VertexData vertex : primitive) {
                    float x = vertex.x(), y = vertex.y();
                    for (Polygon polygon : polygons) {
                        if (polygon.contains(x, y)) continue outer;
                    }
                    return false;
                }
                return true;
            });
        }
    }

    public void computeFocusedPolyhedron() {
        if (focusedRecord == null || focusedPolyhedron.isEmpty()) return;
        VertexData[][] primitives = focusedRecord.primitives();
        VertexData[] focused = focusedPolyhedron.getFirst();
        int focusedIndex = -1;
        for (int i = 0; i < primitives.length; i++) {
            if (focused == primitives[i]) {
                focusedIndex = i;
                break;
            }
        }
        if (focusedIndex == -1) return;
        List<VertexData[]> polyhedron = new ArrayList<>();
        polyhedron.add(focused);
        List<Integer> indexes = new ArrayList<>(List.of(focusedIndex));
        final int primitiveCount = primitives.length;
        boolean added;
        do {
            added = false;
            for (int i = 0; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                if (indexes.contains(i) | !haveCommonVertex(primitive, polyhedron)) continue;
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
        resultIndexes.forEach(i -> focusedPolyhedron.add(primitives[i]));
    }

    public void drawCameraDirections(GuiGraphics graphics) {
        Vec3 start = bindResult.getPosition();
        Matrix3f normal = bindResult.getRotation();
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        drawNormal(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()), modelScale / 3, forwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()), modelScale / 6, upwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()), modelScale / 6, leftArgb);
    }

    public void drawBindTarget(GuiGraphics graphics) {
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
        float minEntityZ = 0f, maxEntityZ = 200f;
        for (BuiltRecord record : records) {
            for (VertexData vertex : record.vertices()) {
                if (vertex.z() < minEntityZ) minEntityZ = vertex.z();
                if (vertex.z() > maxEntityZ) maxEntityZ = vertex.z();
            }
        }
        Matrix4f positionMatrix = new Matrix4f().mul(poseStack.last().pose()).scale(1, 1, 200 / (maxEntityZ - minEntityZ)).translate(0, 0, -minEntityZ);
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
        records.forEach(record -> {
            VertexConsumer buffer = graphics.bufferSource().getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                VertexData.renderVertices(record.vertices(), buffer, positionMatrix, normalMatrix);
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
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0, 0, 0, 0, deltaTick, poseStack, catcher, 0xF000f0);
        dispatcher.setRenderShadow(true);
        records.clear();
        catcher.sendVertices(this);
        Lighting.setupFor3DItems();
    }

    @Override
    public BindResult computeBindResult() {
        Matrix4f matrix4f = new Matrix4f();
        Matrix3f matrix3f = new Matrix3f().scale(-1);
        target.offsets().setScale(target.offsets().getScale() * modelScale);
        for (BuiltRecord record : records) {
            BindResult result = new BindResult(target, true);
            record.exportToBindResult(result, matrix4f, matrix3f);
            if (result.weakAvailable()) currentRecord = record;
            if (!result.available()) continue;
            bindResult = result.computeCamera();
            currentRecord = record;
            return result;
        }
        return BindResult.EMPTY;
    }

    private void drawPrimitive(GuiGraphics graphics, VertexData[] primitive, int z, int argb) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (VertexData vertex : primitive) buffer.addVertex(vertex.x(), vertex.y(), z).setColor(argb);
        if (primitive.length == 3) buffer.addVertex(primitive[2].x(), primitive[2].y(), z).setColor(argb);
        graphics.flush();
    }

    private void drawNormal(GuiGraphics graphics, Vec3 start, Vec3 normal, int length, int argb) {
        Vec3 end = normal.scale(length).add(start);
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.lines());
        buffer.addVertex((float) start.x(), (float) start.y(), z2).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        buffer.addVertex((float) end.x(), (float) end.y(), z2).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        graphics.flush();
    }
}
