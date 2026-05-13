package com.xtracr.realcamera.gui;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.DisableConfig;
import com.xtracr.realcamera.config.BindTarget.TargetConfig;
import com.xtracr.realcamera.renderer.BuiltIterableBuffer;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.renderer.state.BuiltModelRecord;
import com.xtracr.realcamera.renderer.state.VertexData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ModelAnalyser {
    private static final MultiVertexCatcher vertexCatcher = MultiVertexCatcher.create();
    private static final Set<RenderType> UNFOCUSABLE_RENDER_TYPES = ImmutableSet.of(RenderTypes.armorEntityGlint(), RenderTypes.glintTranslucent(), RenderTypes.glint(), RenderTypes.entityGlint());
    private static final int planeArgb = 0x6F3333CC, forwardArgb = 0xFF00CC00, upwardArgb = 0xFFCC0000, leftArgb = 0xFF0000CC, focusedArgb = 0x4FFFFFFF;
    private static final int z1 = 210, z2 = z1 + 10;
    private final List<VertexData[]> focusedPolyhedron = new ArrayList<>();
    final List<BuiltModelRecord> modelRecords = new ArrayList<>(), textureRecords = new ArrayList<>();
    private VertexData[][] targetPrimitives = new VertexData[3][];
    private BindResult bindResult = BindResult.EMPTY;
    private BindTarget target = BindTarget.EMPTY;
    @Nullable
    private BuiltModelRecord focusedRecord, currentRecord;

    private static boolean haveCommonVertex(VertexData[] p1, List<VertexData[]> primitives) {
        final float precision = 1e-5f;
        for (VertexData[] p2 : primitives)
            for (VertexData v1 : p1)
                for (VertexData v2 : p2)
                    if (Math.abs(v1.x() - v2.x()) < precision && Math.abs(v1.y() - v2.y()) < precision && Math.abs(v1.z() - v2.z()) < precision)
                        return true;
        return false;
    }

    public void initialize(BindTarget target, int modelScale) {
        this.target = target;
        modelRecords.clear();
        textureRecords.clear();
        focusedPolyhedron.clear();
        targetPrimitives[0] = targetPrimitives[1] = targetPrimitives[2] = null;
        bindResult = BindResult.EMPTY;
        focusedRecord = currentRecord = null;
        target.offsets().setScale(target.offsets().getScale() * modelScale);
    }

    public String getFocusedTextureId() {
        return focusedRecord == null ? null : focusedRecord.textureId();
    }

    public VertexData[][] getFocusedPolyhedron() {
        return focusedPolyhedron.toArray(new VertexData[0][]);
    }

    public void applyDisableConfigs(String textureId, Set<String> hiddenNames) {
        for (BuiltModelRecord record : modelRecords) {
            if (record.containsTextureId(textureId)) textureRecords.add(record);
        }
        for (int i = 0; i < modelRecords.size(); i++) {
            BuiltModelRecord record = modelRecords.get(i);
            List<VertexData[]> primitives = new ArrayList<>();
            DisableConfig[] disableConfigs = target.filteredDisableConfigs(config -> record.containsTextureId(config.textureId()) && hiddenNames.contains(config.name()));
            boolean disableAll = !record.containsTextureId(textureId);
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
            modelRecords.set(i, new BuiltModelRecord(record.renderType(), record.textureId(), record.vertices(), primitives.toArray(new VertexData[0][])));
        }
    }

    public void computeFocusedOnTexture(float mouseU, float mouseV) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        for (BuiltModelRecord record : textureRecords) {
            for (VertexData[] primitive : record.primitives()) {
                if (VertexData.containsUV(primitive, mouseU, mouseV)) {
                    focusedRecord = record;
                    focusedPolyhedron.add(primitive);
                    break;
                }
            }
        }
    }

    public void computeFocusedOnModel(int mouseX, int mouseY, int layers) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        List<ZEntry> sortByZ = new ArrayList<>();
        for (BuiltModelRecord record : modelRecords) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            VertexData[][] primitives = record.primitives();
            for (VertexData[] primitive : primitives) {
                if (!VertexData.containsXY(primitive, mouseX, mouseY)) continue;
                VertexData vertex = primitive[0];
                float deltaZ = vertex.normalZ() == 0 ? 0 : (vertex.normalX() * (mouseX - vertex.x()) + vertex.normalY() * (mouseY - vertex.y())) / vertex.normalZ();
                sortByZ.add(new ZEntry(record, primitive, vertex.z() - deltaZ));
            }
        }
        if (sortByZ.isEmpty()) return;
        sortByZ.sort(Comparator.comparingDouble(ZEntry::z).reversed());
        ZEntry result = sortByZ.get(Math.min(sortByZ.size() - 1, layers));
        focusedRecord = result.record();
        focusedPolyhedron.add(result.primitive());
    }

    public void computeFocusedOnModel(int minX, int minY, int maxX, int maxY) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        List<ZEntry> sortByZ = new ArrayList<>();
        for (BuiltModelRecord record : modelRecords) {
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
                sortByZ.add(new ZEntry(record, primitive, maxZ));
            }
        }
        if (sortByZ.isEmpty()) return;
        sortByZ.sort(Comparator.comparingDouble((ZEntry entry) -> -entry.z()));
        focusedRecord = sortByZ.getFirst().record();
        while (!sortByZ.isEmpty()) {
            VertexData[] first = sortByZ.getFirst().primitive();
            focusedPolyhedron.add(first);
            sortByZ.removeFirst();
            sortByZ.removeIf(entry -> {
                if (entry.record() != focusedRecord) return true;
                VertexData[] primitive = entry.primitive();
                primitiveFor:
                for (VertexData vertex : primitive) {
                    for (VertexData[] focused : focusedPolyhedron)
                        if (VertexData.containsXY(focused, vertex.x(), vertex.y())) continue primitiveFor;
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
        boolean[] visited = new boolean[primitives.length];
        visited[focusedIndex] = true;
        boolean added;
        do {
            added = false;
            for (int i = 0; i < primitives.length; i++) {
                VertexData[] primitive = primitives[i];
                if (visited[i] | !haveCommonVertex(primitive, polyhedron)) continue;
                polyhedron.add(primitive);
                visited[i] = true;
                added = true;
            }
        } while (added);
        for (int i = focusedIndex + 1; i < primitives.length && visited[i]; i++)
            focusedPolyhedron.add(primitives[i]);
        for (int i = focusedIndex - 1; i >= 0 && visited[i]; i--)
            focusedPolyhedron.add(primitives[i]);
        focusedPolyhedron.add(focused);
    }

    public void drawCameraDirections(GuiGraphicsExtractor graphics, double modelScale) {
        if (targetPrimitives[0] == null || targetPrimitives[1] == null || targetPrimitives[2] == null) return;
        Vec3 start = bindResult.getPosition();
        Matrix3f normal = bindResult.getRotation();
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        GUIHelper.vector(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()).scale(modelScale / 3), z2, forwardArgb);
        GUIHelper.vector(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()).scale(modelScale / 6), z2, upwardArgb);
        GUIHelper.vector(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()).scale(modelScale / 6), z2, leftArgb);
    }

    public void drawBindTarget(GuiGraphicsExtractor graphics, double modelScale) {
        if (currentRecord == null) return;
        TargetConfig config = target.targetConfig();
        if (targetPrimitives[0] != null) GUIHelper.triangleOrQuad(graphics, targetPrimitives[0], z1, planeArgb);
        if (targetPrimitives[1] != null) GUIHelper.vector(graphics, VertexData.position(targetPrimitives[1], config.forwardU(), config.forwardV()), VertexData.normal(targetPrimitives[1]).scale(-modelScale / 2), z2, forwardArgb);
        if (targetPrimitives[2] != null) GUIHelper.vector(graphics, VertexData.position(targetPrimitives[2], config.upwardU(), config.upwardV()), VertexData.normal(targetPrimitives[2]).scale(-modelScale / 2), z2, upwardArgb);
    }

    public void drawFocusedInModelArea(GuiGraphicsExtractor graphics) {
        if (focusedPolyhedron.isEmpty() || focusedRecord == null) return;
        VertexData[] focused = focusedPolyhedron.getFirst();
        VertexData[] reversed = new VertexData[focused.length];
        for (int i = 0; i < focused.length; i++) reversed[i] = focused[focused.length - 1 - i];
        GUIHelper.triangleOrQuad(graphics, reversed, z1, focusedArgb);
        focusedPolyhedron.forEach(primitive -> GUIHelper.triangleOrQuad(graphics, primitive, z1, focusedArgb));
    }

    public void drawFocusedInTextureArea(GuiGraphicsExtractor graphics, Matrix4f texturePose) {
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
                position.set(vertex.u(), vertex.v(), 0).mulPosition(texturePose);
                transformed[i] = reversed[length - 1 - i] = VertexData.immutable(position.x(), position.y(), 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            }
            GUIHelper.triangleOrQuad(graphics, transformed, 0, focusedArgb);
            GUIHelper.triangleOrQuad(graphics, reversed, 0, focusedArgb);
        }
    }

    public void updateModel(Minecraft client, Entity entity, float partialTicks, PoseStack poseStack) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        client.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        dispatcher.submit(dispatcher.extractEntity(entity, partialTicks), new CameraRenderState(), 0, 0, 0, poseStack, vertexCatcher.initCollector());
        vertexCatcher.forEachBuffer(this::computeBindResult);
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
        BuiltModelRecord record = new BuiltModelRecord(builtBuffer.renderType(), builtBuffer.textureId(), vertices, primitives);
        modelRecords.add(record);
        if (!builtBuffer.textureId().contains(target.textureId()) || currentRecord != null) return;
        BindResult result = new BindResult(target);
        BindTarget.TargetConfig config = target.targetConfig();
        VertexData.UV[] uvs = {new VertexData.UV(config.posU(), config.posV()), new VertexData.UV(config.forwardU(), config.forwardV()), new VertexData.UV(config.upwardU(), config.upwardV())};
        targetPrimitives = builtBuffer.findPrimitives(uvs);
        if (targetPrimitives[0] != null) result.setPosition(VertexData.position(targetPrimitives[0], config.posU(), config.posV()));
        if (targetPrimitives[1] != null) result.setForward(VertexData.normal(targetPrimitives[1]).scale(-1));
        if (targetPrimitives[2] != null) result.setUpward(VertexData.normal(targetPrimitives[2]).scale(-1));
        if (result.weakAvailable()) {
            currentRecord = record;
            bindResult = result.computeCamera(true);
        }
    }

    private record ZEntry(BuiltModelRecord record, VertexData[] primitive, float z) {
    }
}
