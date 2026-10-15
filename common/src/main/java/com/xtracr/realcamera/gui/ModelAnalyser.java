package com.xtracr.realcamera.gui;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.TargetConfig;
import com.xtracr.realcamera.config.DisableConfig;
import com.xtracr.realcamera.renderer.IdentifiedVertexBuffer;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.renderer.gui.GuiCulledModelsRenderer;
import com.xtracr.realcamera.renderer.state.ModelSnapshot;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.util.PrimitiveLookup;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ModelAnalyser {
    private static final MultiVertexCatcher vertexCatcher = MultiVertexCatcher.create();
    private static final Set<RenderType> UNFOCUSABLE_RENDER_TYPES = ImmutableSet.of(RenderTypes.armorEntityGlint(), RenderTypes.glintTranslucent(), RenderTypes.glint(), RenderTypes.entityGlint());
    private static final int PLANE_ARGB = 0x6F3333CC, FORWARD_ARGB = 0xFF00CC00, UPWARD_ARGB = 0xFFCC0000, LEFT_ARGB = 0xFF0000CC, FOCUSED_ARGB = 0x4FFFFFFF;
    private static final int Z_QUAD = GuiCulledModelsRenderer.MAX_Z + 10, Z_LINE = Z_QUAD + 10;
    private final List<Primitive> focusedPolyhedron = new ArrayList<>();
    private Primitive[] targetPrimitives = new Primitive[3];
    private BindResult bindResult = BindResult.EMPTY;
    @Nullable
    private ModelSnapshot focusedRecord;

    public static void applyDisableConfigs(List<ModelSnapshot> modelRecords, BindTarget target, String textureId, Set<String> hiddenNames) {
        for (int i = 0; i < modelRecords.size(); i++) {
            ModelSnapshot record = modelRecords.get(i);
            List<Primitive> primitives = new ArrayList<>();
            DisableConfig[] disableConfigs = target.filteredDisableConfigs(config -> record.containsTextureId(config.textureId()) && hiddenNames.contains(config.name()));
            boolean disableAll = !record.containsTextureId(textureId);
            for (DisableConfig config : disableConfigs) {
                if (config.disableAll()) {
                    disableAll = true;
                    break;
                }
            }
            if (!disableAll) for (Primitive primitive : record.primitives()) {
                primitiveFor:
                for (VertexData vertex : primitive) {
                    for (DisableConfig config : disableConfigs) {
                        if (config.disable(vertex)) continue primitiveFor;
                    }
                    primitives.add(primitive);
                    break;
                }
            }
            modelRecords.set(i, new ModelSnapshot(record.renderType(), record.textureId(), record.vertexArray(), primitives.toArray(new Primitive[0])));
        }
    }

    private static long quantizedKey(VertexData vertex) {
        int x = Math.round(vertex.x() * 1e5f);
        int y = Math.round(vertex.y() * 1e5f);
        int z = Math.round(vertex.z() * 1e5f);
        return ((long) x & 0x1FFFFF) | (((long) y & 0x1FFFFF) << 21) | (((long) z & 0x1FFFFF) << 42);
    }

    public Primitive[] getFocusedPolyhedron() {
        return focusedPolyhedron.toArray(new Primitive[0]);
    }

    public String getFocusedTextureId() {
        return focusedRecord == null ? null : focusedRecord.textureId();
    }

    public void computeFocusedOnTexture(List<ModelSnapshot> textureRecords, float mouseU, float mouseV) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        for (ModelSnapshot record : textureRecords) {
            for (Primitive primitive : record.primitives()) {
                if (primitive.containsUV(mouseU, mouseV)) {
                    focusedRecord = record;
                    focusedPolyhedron.add(primitive);
                    break;
                }
            }
        }
    }

    public void computeFocusedOnModel(List<ModelSnapshot> modelRecords, int mouseX, int mouseY, int layers) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        List<ZEntry> sortByZ = new ArrayList<>();
        for (ModelSnapshot record : modelRecords) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            Iterable<Primitive> primitives = record.primitives();
            for (Primitive primitive : primitives) {
                if (!primitive.containsXY(mouseX, mouseY)) continue;
                VertexData vertex = primitive.vertex(0);
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

    public void computeFocusedOnModel(List<ModelSnapshot> modelRecords, int minX, int minY, int maxX, int maxY) {
        if (focusedRecord != null && !focusedPolyhedron.isEmpty()) return;
        List<ZEntry> sortByZ = new ArrayList<>();
        for (ModelSnapshot record : modelRecords) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            Iterable<Primitive> primitives = record.primitives();
            primitiveFor:
            for (Primitive primitive : primitives) {
                float maxZ = primitive.vertex(0).z();
                for (VertexData vertex : primitive) {
                    int x = (int) vertex.x(), y = (int) vertex.y();
                    if (x < minX || y < minY || x > maxX || y > maxY) continue primitiveFor;
                    if (vertex.z() > maxZ) maxZ = vertex.z();
                }
                sortByZ.add(new ZEntry(record, primitive, maxZ));
            }
        }
        if (sortByZ.isEmpty()) return;
        sortByZ.sort(Comparator.comparingDouble(ZEntry::z).reversed());
        focusedRecord = sortByZ.getFirst().record();
        while (!sortByZ.isEmpty()) {
            Primitive first = sortByZ.getFirst().primitive();
            focusedPolyhedron.add(first);
            sortByZ.removeFirst();
            sortByZ.removeIf(entry -> {
                if (entry.record() != focusedRecord) return true;
                Primitive primitive = entry.primitive();
                primitiveFor:
                for (VertexData vertex : primitive) {
                    for (Primitive focused : focusedPolyhedron)
                        if (focused.containsXY(vertex.x(), vertex.y())) continue primitiveFor;
                    return false;
                }
                return true;
            });
        }
    }

    public void computeFocusedPolyhedron() {
        if (focusedRecord == null || focusedPolyhedron.isEmpty()) return;
        Primitive[] primitives = focusedRecord.primitiveArray();
        Primitive focused = focusedPolyhedron.getFirst();
        int focusedIndex = -1;
        for (int i = 0; i < primitives.length; i++) {
            if (focused == primitives[i]) {
                focusedIndex = i;
                break;
            }
        }
        if (focusedIndex == -1) return;
        LongOpenHashSet vertexSet = new LongOpenHashSet();
        for (VertexData vertex : focused) vertexSet.add(quantizedKey(vertex));
        boolean[] visited = new boolean[primitives.length];
        visited[focusedIndex] = true;
        boolean added;
        do {
            added = false;
            for (int i = 0; i < primitives.length; i++) {
                if (visited[i]) continue;
                for (VertexData vertex : primitives[i]) {
                    if (!vertexSet.contains(quantizedKey(vertex))) continue;
                    visited[i] = true;
                    for (VertexData vertex2 : primitives[i]) vertexSet.add(quantizedKey(vertex2));
                    added = true;
                    break;
                }
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
        GUIHelper.vector(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()).scale(modelScale / 3), Z_LINE, FORWARD_ARGB);
        GUIHelper.vector(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()).scale(modelScale / 6), Z_LINE, UPWARD_ARGB);
        GUIHelper.vector(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()).scale(modelScale / 6), Z_LINE, LEFT_ARGB);
    }

    public void drawBindTarget(GuiGraphicsExtractor graphics, BindTarget target, double modelScale) {
        TargetConfig config = target.targetConfig();
        if (targetPrimitives[0] != null) GUIHelper.triangleOrQuad(graphics, targetPrimitives[0], Z_QUAD, PLANE_ARGB);
        if (targetPrimitives[1] != null) GUIHelper.vector(graphics, targetPrimitives[1].positionAtUV(config.forwardU(), config.forwardV()), targetPrimitives[1].faceNormal().scale(-modelScale / 2), Z_LINE, FORWARD_ARGB);
        if (targetPrimitives[2] != null) GUIHelper.vector(graphics, targetPrimitives[2].positionAtUV(config.upwardU(), config.upwardV()), targetPrimitives[2].faceNormal().scale(-modelScale / 2), Z_LINE, UPWARD_ARGB);
    }

    public void drawFocusedInModelArea(GuiGraphicsExtractor graphics) {
        if (focusedPolyhedron.isEmpty() || focusedRecord == null) return;
        Primitive focused = focusedPolyhedron.getFirst();
        GUIHelper.triangleOrQuad(graphics, focused, Z_QUAD, FOCUSED_ARGB, true);
        for (Primitive primitive : focusedPolyhedron) GUIHelper.triangleOrQuad(graphics, primitive, Z_QUAD, FOCUSED_ARGB);
    }

    public void drawFocusedInTextureArea(GuiGraphicsExtractor graphics, Matrix4fc texturePose) {
        Vector3f position = new Vector3f();
        for (Primitive primitive : focusedPolyhedron) {
            Primitive transformed = primitive.mapVertices(vertex -> {
                texturePose.transformPosition(vertex.u(), vertex.v(), 0, position);
                return VertexData.immutable(position.x, position.y, 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            });
            GUIHelper.triangleOrQuad(graphics, transformed, 0, FOCUSED_ARGB);
            GUIHelper.triangleOrQuad(graphics, transformed, 0, FOCUSED_ARGB, true);
        }
    }

    public List<ModelSnapshot> captureModel(Minecraft client, Entity entity, float partialTicks, PoseStack poseStack, BindTarget target) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        client.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        dispatcher.submit(dispatcher.extractEntity(entity, partialTicks), new CameraRenderState(), 0, 0, 0, poseStack, vertexCatcher.initCollector());
        List<ModelSnapshot> records = new ArrayList<>();
        vertexCatcher.forEachBuffer(buf -> computeRecord(buf, records, target));
        return records;
    }

    private void computeRecord(IdentifiedVertexBuffer builtBuffer, List<ModelSnapshot> records, BindTarget target) {
        ModelSnapshot record = builtBuffer.snapshot();
        records.add(record);
        if (!builtBuffer.textureId().contains(target.textureId()) || !bindResult.target.isEmpty()) return;
        BindResult result = new BindResult(target);
        BindTarget.TargetConfig config = target.targetConfig();
        VertexData.UV[] uvs = {new VertexData.UV(config.posU(), config.posV()), new VertexData.UV(config.forwardU(), config.forwardV()), new VertexData.UV(config.upwardU(), config.upwardV())};
        targetPrimitives = PrimitiveLookup.findPrimitives(builtBuffer, uvs, PrimitiveLookup.LookupMode.SCAN_ONLY);
        if (targetPrimitives[0] != null) result.setPosition(targetPrimitives[0].positionAtUV(config.posU(), config.posV()));
        if (targetPrimitives[1] != null) result.setForward(targetPrimitives[1].faceNormal().scale(-1));
        if (targetPrimitives[2] != null) result.setUpward(targetPrimitives[2].faceNormal().scale(-1));
        if (result.weakAvailable()) bindResult = result.computeCamera(true);
    }

    private record ZEntry(ModelSnapshot record, Primitive primitive, float z) {
    }
}
