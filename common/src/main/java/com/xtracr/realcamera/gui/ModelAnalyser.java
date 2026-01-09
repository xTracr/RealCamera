package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.DisableConfig;
import com.xtracr.realcamera.config.BindTarget.TargetConfig;
import com.xtracr.realcamera.util.BuiltIterableBuffer;
import com.xtracr.realcamera.util.MultiVertexCatcher;
import com.xtracr.realcamera.util.VertexData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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

public class ModelAnalyser {
    private static final Set<RenderType> UNFOCUSABLE_RENDER_TYPES = Set.of(RenderTypes.armorEntityGlint(), RenderTypes.glintTranslucent(), RenderTypes.glint(), RenderTypes.entityGlint());
    private static final int planeArgb = 0x6F3333CC, forwardArgb = 0xFF00CC00, upwardArgb = 0xFFCC0000, leftArgb = 0xFF0000CC, focusedArgb = 0x4FFFFFFF;
    private static final int z1 = 210, z2 = z1 + 10;
    public final List<VertexData[]> focusedPolyhedron = new ArrayList<>();
    public final PoseStack modelPose = new PoseStack(), texturePose = new PoseStack();
    private final List<BuiltRecord> modelRecords = new ArrayList<>(), textureRecords = new ArrayList<>();
    private VertexData[][] targetPrimitives = new VertexData[3][];
    private BindResult bindResult = BindResult.EMPTY;
    private BindTarget target = BindTarget.EMPTY;
    @Nullable
    private BuiltRecord focusedRecord, currentRecord;
    private double modelScale;

    private static boolean haveCommonVertex(VertexData[] p1, List<VertexData[]> primitives) {
        final float precision = 1e-5f;
        for (VertexData[] p2 : primitives) for (VertexData v1 : p1) for (VertexData v2 : p2)
            if (Math.abs(v1.x() - v2.x()) < precision && Math.abs(v1.y() - v2.y()) < precision && Math.abs(v1.z() - v2.z()) < precision) return true;
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
        modelRecords.clear();
        textureRecords.clear();
        focusedPolyhedron.clear();
        modelPose.setIdentity();
        texturePose.setIdentity();
        targetPrimitives[0] = targetPrimitives[1] = targetPrimitives[2] = null;
        bindResult = BindResult.EMPTY;
        focusedRecord = currentRecord = null;
        target.offsets().setScale(target.offsets().getScale() * modelScale);
    }

    public String getFocusedTextureId() {
        return focusedRecord == null ? null : focusedRecord.textureId();
    }

    public void applyDisableConfigs(String textureId, Set<String> hiddenNames) {
        for (BuiltRecord record : modelRecords) {
            if (!record.textureId().contains(textureId)) continue;
            textureRecords.add(record);
        }
        for (int i = 0; i < modelRecords.size(); i++) {
            BuiltRecord record = modelRecords.get(i);
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
                primitiveFor:
                for (VertexData vertex : primitive) {
                    for (DisableConfig config : disableConfigs) {
                        if (config.disable(vertex)) continue primitiveFor;
                    }
                    primitives.add(primitive);
                    break;
                }
            }
            modelRecords.set(i, new BuiltRecord(record.renderType(), record.textureId(), record.vertices(), primitives.toArray(new VertexData[0][])));
        }
    }

    public void computeFocusedOnTexture(int mouseX, int mouseY) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        Matrix4f positionMatrix = texturePose.last().pose();
        Vector3f position = new Vector3f();
        for (BuiltRecord record : textureRecords) {
            int length = record.renderType().mode().primitiveLength;
            int[] xs = new int[length], ys = new int[length];
            for (VertexData[] primitive : record.primitives()) {
                for (int j = 0; j < length; j++) {
                    VertexData vertex = primitive[j];
                    position.set(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
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
        for (BuiltRecord record : modelRecords) {
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
        for (BuiltRecord record : modelRecords) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            VertexData[][] primitives = record.primitives();
            primitiveFor:
            for (VertexData[] primitive : primitives) {
                float maxZ = primitive[0].z();
                for (VertexData vertex : primitive) {
                    int x = (int) vertex.x(), y = (int) vertex.y();
                    if (x < minX || y < minY || x > maxX || y > maxY) continue primitiveFor;
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
                primitiveFor:
                for (VertexData vertex : primitive) {
                    for (Polygon polygon : polygons) if (polygon.contains(vertex.x(), vertex.y())) continue primitiveFor;
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
        if (targetPrimitives[0] == null || targetPrimitives[1] == null || targetPrimitives[2] == null) return;
        Vec3 start = bindResult.getPosition();
        Matrix3f normal = bindResult.getRotation();
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        GUIHelper.renderVector(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()).scale(modelScale / 3), z2, forwardArgb);
        GUIHelper.renderVector(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()).scale(modelScale / 6), z2, upwardArgb);
        GUIHelper.renderVector(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()).scale(modelScale / 6), z2, leftArgb);
    }

    public void drawBindTarget(GuiGraphics graphics) {
        if (currentRecord == null) return;
        TargetConfig config = target.targetConfig();
        if (targetPrimitives[0] != null) GUIHelper.renderPolygon(graphics, targetPrimitives[0], z1, planeArgb);
        if (targetPrimitives[1] != null) GUIHelper.renderVector(graphics, VertexData.position(targetPrimitives[1], config.forwardU(), config.forwardV()), VertexData.normal(targetPrimitives[1]).scale(-modelScale / 2), z2, forwardArgb);
        if (targetPrimitives[2] != null) GUIHelper.renderVector(graphics, VertexData.position(targetPrimitives[2], config.upwardU(), config.upwardV()), VertexData.normal(targetPrimitives[2]).scale(-modelScale / 2), z2, upwardArgb);
    }

    public void drawFocusedInModelArea(GuiGraphics graphics) {
        if (focusedPolyhedron.isEmpty() || focusedRecord == null) return;
        VertexData[] focused = focusedPolyhedron.getFirst();
        VertexData[] reversed = new VertexData[focused.length];
        for (int i = 0; i < focused.length; i++) reversed[i] = focused[focused.length - 1 - i];
        GUIHelper.renderPolygon(graphics, reversed, z1, focusedArgb);
        focusedPolyhedron.forEach(primitive -> GUIHelper.renderPolygon(graphics, primitive, z1, focusedArgb));
    }

    public void drawFocusedInTextureArea(GuiGraphics graphics) {
        Matrix4f positionMatrix = texturePose.last().pose();
        int length = 0;
        VertexData[] transformed = new VertexData[0], reversed = new VertexData[0];
        Vector3f position = new Vector3f();
        for (VertexData[] primitive : focusedPolyhedron) {
            if (length != primitive.length) {
                length = primitive.length;
                transformed = new VertexData[length];
                reversed = new VertexData[length];
            }
            for (int i = 0; i < length; i++) {
                VertexData vertex = primitive[i];
                position.set(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                transformed[i] = reversed[length - 1 - i] = VertexData.immutable(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            }
            GUIHelper.renderPolygon(graphics, transformed, 0, focusedArgb);
            GUIHelper.renderPolygon(graphics, reversed, 0, focusedArgb);
        }
    }

    public void drawModel(MultiBufferSource bufferSource, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(modelPose.last().pose().invert(new Matrix4f()));
        float minEntityZ = 0f, maxEntityZ = 200f;
        for (BuiltRecord record : modelRecords) {
            for (VertexData vertex : record.vertices()) {
                if (vertex.z() < minEntityZ) minEntityZ = vertex.z();
                if (vertex.z() > maxEntityZ) maxEntityZ = vertex.z();
            }
        }
        Matrix4f positionMatrix = new Matrix4f().mul(poseStack.last().pose()).scale(1, 1, 200 / (maxEntityZ - minEntityZ)).translate(0, 0, -minEntityZ);
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
        modelRecords.forEach(record -> {
            VertexConsumer buffer = bufferSource.getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                for (VertexData vertex : record.vertices()) vertex.render(buffer, positionMatrix, normalMatrix);
                return;
            }
            for (VertexData[] primitive : record.primitives()) {
                for (VertexData vertex : primitive) vertex.render(buffer, positionMatrix, normalMatrix);
            }
        });
        poseStack.popPose();
    }

    public void drawTexture(MultiBufferSource bufferSource, PoseStack poseStack) {
        Matrix4f positionMatrix = poseStack.last().pose();
        textureRecords.forEach(record -> {
            VertexConsumer buffer = bufferSource.getBuffer(record.renderType());
            Vector3f position = new Vector3f();
            for (VertexData vertex : record.vertices()) {
                position.set(vertex.u(), vertex.v(), 0).mulPosition(positionMatrix);
                buffer.addVertex(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            }
        });
    }

    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        client.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        dispatcher.setRenderShadow(false);
        MultiVertexCatcher catcher = MultiVertexCatcher.defaultImpl();
        dispatcher.render(entity, 0, 0, 0, deltaTick, poseStack, catcher, 0xF000f0);
        catcher.endCatching(this::computeBindResult);
        dispatcher.setRenderShadow(true);
    }

    public void computeBindResult(BuiltIterableBuffer builtBuffer) {
        VertexData[] vertices = builtBuffer.vertexBuffer().stream().map(VertexData::asImmutable).toArray(VertexData[]::new);
        VertexFormat.Mode drawMode = builtBuffer.renderType().mode();
        final int primitiveLength = drawMode.primitiveLength, primitiveStride = drawMode.primitiveStride;
        final int primitiveCount = (vertices.length - primitiveLength) / primitiveStride + 1;
        final boolean startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
        VertexData[][] primitives = new VertexData[primitiveCount][primitiveLength];
        for (int i = 0, k = 0; i < primitiveCount; i++, k += primitiveStride) {
            primitives[i][0] = vertices[startWithFirst ? 0 : k];
            System.arraycopy(vertices, k + 1, primitives[i], 1, primitiveLength - 1);
        }
        BuiltRecord record = new BuiltRecord(builtBuffer.renderType(), builtBuffer.textureId(), vertices, primitives);
        modelRecords.add(record);
        if (!builtBuffer.textureId().contains(target.textureId()) || currentRecord != null) return;
        BindResult result = new BindResult(target, true);
        BindTarget.TargetConfig config = target.targetConfig();
        VertexData.UV[] uvs = {new VertexData.UV(config.posU(), config.posV()), new VertexData.UV(config.forwardU(), config.forwardV()), new VertexData.UV(config.upwardU(), config.upwardV())};
        targetPrimitives = builtBuffer.findPrimitivesInCache(uvs);
        if (builtBuffer.anyNotCached(uvs)) {
            for (int i = 0; i < targetPrimitives.length; i++) {
                if (targetPrimitives[i] != null) uvs[i] = null;
            }
            VertexData[][] newPrimitives = builtBuffer.findPrimitives(uvs);
            for (int i = 0; i < targetPrimitives.length; i++) {
                if (newPrimitives[i] != null) targetPrimitives[i] = newPrimitives[i];
            }
        }
        if (targetPrimitives[0] != null) result.setPosition(VertexData.position(targetPrimitives[0], config.posU(), config.posV()));
        if (targetPrimitives[1] != null) result.setForward(VertexData.normal(targetPrimitives[1]).scale(-1));
        if (targetPrimitives[2] != null) result.setUpward(VertexData.normal(targetPrimitives[2]).scale(-1));
        if (result.weakAvailable()) {
            currentRecord = record;
            bindResult = result.computeCamera();
        }
    }

    private record BuiltRecord(RenderType renderType, String textureId, VertexData[] vertices, VertexData[][] primitives) { }
}
