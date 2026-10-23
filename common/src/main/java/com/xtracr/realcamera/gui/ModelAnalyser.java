package com.xtracr.realcamera.gui;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.config.binding.TargetConfig;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.renderer.gui.GuiCulledModelsRenderer;
import com.xtracr.realcamera.renderer.state.ModelSnapshot;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.util.LookupContext;
import com.xtracr.realcamera.util.ReferenceFrame;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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
    private @Nullable LookupContext targetLookup;
    private @Nullable ReferenceFrame cameraFrame;
    @Nullable
    private ModelSnapshot focusedRecord;

    public static void applyDisableConfigs(List<ModelSnapshot> modelRecords, RenderConfig renderConfig, String textureId) {
        for (int i = 0; i < modelRecords.size(); i++) {
            ModelSnapshot record = modelRecords.get(i);
            List<Primitive> primitives = new ArrayList<>();
            if (record.containsTextureId(textureId)) {
                RenderConfig.DisableCache disables = renderConfig.disableCache(record.textureId());
                if (!disables.disableAll()) for (var primitive : record.primitives()) {
                    for (var vertex : primitive) {
                        if (disables.disable(vertex.u(), vertex.v())) continue;
                        primitives.add(primitive);
                        break;
                    }
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
        for (var record : textureRecords) {
            for (var primitive : record.primitives()) {
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
        for (var record : modelRecords) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            Iterable<Primitive> primitives = record.primitives();
            for (var primitive : primitives) {
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
        for (var record : modelRecords) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) continue;
            Iterable<Primitive> primitives = record.primitives();
            primitiveFor:
            for (var primitive : primitives) {
                float maxZ = primitive.vertex(0).z();
                for (var vertex : primitive) {
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
                for (var vertex : primitive) {
                    for (var focused : focusedPolyhedron)
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
        for (var vertex : focused) vertexSet.add(quantizedKey(vertex));
        boolean[] visited = new boolean[primitives.length];
        visited[focusedIndex] = true;
        boolean added;
        do {
            added = false;
            for (int i = 0; i < primitives.length; i++) {
                if (visited[i]) continue;
                for (var vertex : primitives[i]) {
                    if (!vertexSet.contains(quantizedKey(vertex))) continue;
                    visited[i] = true;
                    for (var vertex2 : primitives[i]) vertexSet.add(quantizedKey(vertex2));
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
        if (cameraFrame == null) return;
        Vec3 start = cameraFrame.origin();
        GUIHelper.vector(graphics, start, cameraFrame.forward().scale(modelScale / 3), Z_LINE, FORWARD_ARGB);
        GUIHelper.vector(graphics, start, cameraFrame.upward().scale(modelScale / 6), Z_LINE, UPWARD_ARGB);
        // Reflection belongs to the viewport; the frame itself remains right-handed.
        GUIHelper.vector(graphics, start, cameraFrame.left().scale(-modelScale / 6), Z_LINE, LEFT_ARGB);
    }

    public void drawBindTarget(GuiGraphicsExtractor graphics, @Nullable TargetConfig config, double modelScale) {
        if (config == null || targetLookup == null) return;
        Primitive position = targetLookup.positionPrimitive(), forward = targetLookup.forwardPrimitive(), upward = targetLookup.upwardPrimitive();
        if (position != null)
            GUIHelper.triangleOrQuad(graphics, position, Z_QUAD, PLANE_ARGB);
        if (forward != null)
            GUIHelper.vector(graphics, forward.positionAtUV(config.forwardU(), config.forwardV()), forward.faceNormal().scale(-modelScale / 2), Z_LINE, FORWARD_ARGB);
        if (upward != null)
            GUIHelper.vector(graphics, upward.positionAtUV(config.upwardU(), config.upwardV()), upward.faceNormal().scale(-modelScale / 2), Z_LINE, UPWARD_ARGB);
    }

    public void drawFocusedInModelArea(GuiGraphicsExtractor graphics) {
        if (focusedPolyhedron.isEmpty() || focusedRecord == null) return;
        Primitive focused = focusedPolyhedron.getFirst();
        GUIHelper.triangleOrQuad(graphics, focused, Z_QUAD, FOCUSED_ARGB, true);
        for (var primitive : focusedPolyhedron)
            GUIHelper.triangleOrQuad(graphics, primitive, Z_QUAD, FOCUSED_ARGB);
    }

    public void drawFocusedInTextureArea(GuiGraphicsExtractor graphics, Matrix4fc texturePose) {
        Vector3f position = new Vector3f();
        for (var primitive : focusedPolyhedron) {
            Primitive transformed = primitive.mapVertices(vertex -> {
                texturePose.transformPosition(vertex.u(), vertex.v(), 0, position);
                return VertexData.immutable(position.x, position.y, 0, vertex.argb(), vertex.u(), vertex.v(), vertex.overlay(), vertex.light(), 0, 0, 1);
            });
            GUIHelper.triangleOrQuad(graphics, transformed, 0, FOCUSED_ARGB);
            GUIHelper.triangleOrQuad(graphics, transformed, 0, FOCUSED_ARGB, true);
        }
    }

    public List<ModelSnapshot> captureModel(Minecraft client, Entity entity, float partialTicks, PoseStack poseStack, @Nullable TargetConfig config, OffsetConfig offsets) {
        cameraFrame = null;
        targetLookup = null;
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        client.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        dispatcher.submit(dispatcher.extractEntity(entity, partialTicks), new CameraRenderState(), 0, 0, 0, poseStack, vertexCatcher.initCollector());
        List<ModelSnapshot> records = new ArrayList<>();
        for (var buffer : vertexCatcher.collectBuffers()) {
            records.add(buffer.snapshot());
            if (config == null || !buffer.containsTextureId(config.textureId())) continue;
            if (targetLookup == null) targetLookup = new LookupContext(config);
            // Preserve the first partial match so its found primitives remain visible in the editor.
            if (!targetLookup.isEmpty()) continue;
            targetLookup.find(buffer, LookupContext.LookupMode.SCAN_ONLY);
            ReferenceFrame frame = targetLookup.tryBuild(true);
            if (frame != null) cameraFrame = offsets.applyTo(frame, true);
        }
        return records;
    }

    private record ZEntry(ModelSnapshot record, Primitive primitive, float z) {
    }
}
