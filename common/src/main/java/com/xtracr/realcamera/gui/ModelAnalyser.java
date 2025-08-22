package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ExcludedRegion;
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

import java.awt.*;
import java.util.*;
import java.util.List;

public class ModelAnalyser extends VertexRecorder {
    private static final Set<RenderType> UNFOCUSABLE_RENDER_TYPES = Set.of(RenderType.armorEntityGlint(), RenderType.glintTranslucent(), RenderType.glint(), RenderType.entityGlint(), RenderType.entityGlintDirect());
    private static final int primitiveArgb = 0x6F3333CC, forwardArgb = 0xFF00CC00, upwardArgb = 0xFFCC0000, leftArgb = 0xFF0000CC;
    private static final int focusedArgb = 0x7FFFFFFF, sideArgb = 0x3FFFFFFF;
    private BindingContext bindingContext = BindingContext.EMPTY;
    private BindingTarget target = new BindingTarget();
    @Nullable
    private BuiltRecord focusedRecord, currentRecord;
    private int focusedIndex = -1;
    private List<ExcludedRegion> selectedExclusions = new ArrayList<>();
    
    // Store sorted primitives for brush mode multi-layer deletion
    private List<Triple> lastSortByDepth = new ArrayList<>();
    
    // Cache for performance optimization
    private static class RecordCache {
        Map<Integer, List<Integer>> adjacencyMap;
        Map<Integer, Vec2> uvCenterMap;
        // REMOVED: normalMap - no longer needed for rotation-invariant matching
        Map<Integer, Float> neighborhoodHashMap;
        
        RecordCache() {
            this.adjacencyMap = new HashMap<>();
            this.uvCenterMap = new HashMap<>();
            // REMOVED: normalMap initialization
            this.neighborhoodHashMap = new HashMap<>();
        }
    }
    
    private final Map<BuiltRecord, RecordCache> cacheMap = new HashMap<>();
    private boolean cacheValid = false;

    private static boolean intersects(VertexData[] p1, List<VertexData[]> primitives) {
        final float precision = 1.0E-05f;
        for (VertexData[] p2 : primitives) for (VertexData v1 : p1) for (VertexData v2 : p2) if (v1.pos().distanceToSqr(v2.pos()) < precision) return true;
        return false;
    }

    public void setup(BindingTarget target) {
        this.target = target;
        bindingContext = BindingContext.EMPTY;
        focusedRecord = currentRecord = null;
        focusedIndex = -1;
    }
    
    public void setSelectedExclusions(List<ExcludedRegion> exclusions) {
        this.selectedExclusions = exclusions != null ? exclusions : new ArrayList<>();
    }

    public String focusedTextureId() {
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

    public void previewEffect(GuiGraphics graphics, int entitySize, boolean canSelect) {
        if (canSelect) drawFocused(graphics);
        Vec3 start = bindingContext.getPosition();
        Matrix3f normal = bindingContext.normal;
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        drawNormal(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()), entitySize / 3, forwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()), entitySize / 6, upwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()), entitySize / 6, leftArgb);
    }

    public void drawSelected(GuiGraphics graphics, int entitySize) {
        drawFocusedPolyhedron(graphics);
        drawFocused(graphics);
        if (currentRecord == null) return;
        currentRecord.findPrimitive(target.getPosU(), target.getPosV()).ifPresent(primitive -> drawPrimitive(graphics, primitive, primitiveArgb, 1000));
        currentRecord.findPrimitive(target.getForwardU(), target.getForwardV()).ifPresent(primitive ->
                drawNormal(graphics, getPosition(primitive, target.getForwardU(), target.getForwardV()), primitive[0].normal(), entitySize / 2, forwardArgb));
        currentRecord.findPrimitive(target.getUpwardU(), target.getUpwardV()).ifPresent(primitive ->
                drawNormal(graphics, getPosition(primitive, target.getUpwardU(), target.getUpwardV()), primitive[0].normal(), entitySize / 2, upwardArgb));
    }

    public void analyse(int entitySize, int mouseX, int mouseY, int layers, boolean hideDisabled, String idInField) {
        target.setScale(target.getScale() * entitySize);
        genContext();
        records.removeIf(record -> {
            boolean isIdInField = !idInField.isBlank() && record.textureId().contains(idInField);
            return (hideDisabled && isIdInField) || (!isIdInField && target.getDisabledTextureIds().stream().anyMatch(record.textureId()::contains));
        });
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
        // Store the sorted list for brush mode
        lastSortByDepth = new ArrayList<>(sortByDepth);
        
        if (!sortByDepth.isEmpty()) {
            sortByDepth.sort(Comparator.comparingDouble(triple -> -triple.depth));
            Triple result = sortByDepth.get(Math.min(sortByDepth.size() - 1, layers));
            focusedRecord = result.record;
            focusedIndex = result.index;
        }
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
        
        // Build cache after model update
        buildCache();
    }
    
    private void buildCache() {
        // Clear old cache entries that are no longer valid
        cacheMap.keySet().removeIf(record -> !records.contains(record));
        
        // Build cache for new records
        for (BuiltRecord record : records) {
            if (!cacheMap.containsKey(record)) {
                RecordCache cache = new RecordCache();
                
                // Build adjacency map using unified algorithm
                cache.adjacencyMap = PrimitiveUtils.buildAdjacencyMap(record.primitives());
                
                // Pre-calculate UV centers (normals removed for rotation invariance)
                for (int i = 0; i < record.primitives().length; i++) {
                    VertexData[] primitive = record.primitives()[i];
                    cache.uvCenterMap.put(i, PrimitiveUtils.getUVCenter(primitive));
                    // REMOVED: Normal calculation - no longer needed for rotation-invariant matching
                    // cache.normalMap.put(i, PrimitiveUtils.getAverageNormal(primitive));
                    
                    // Calculate neighborhood hash for each primitive
                    List<Vec2> neighborOffsets = new ArrayList<>();
                    List<Integer> adjacentIndices = cache.adjacencyMap.get(i);
                    if (adjacentIndices != null) {
                        Vec2 center = cache.uvCenterMap.get(i);
                        for (int adjIndex : adjacentIndices) {
                            Vec2 adjCenter = PrimitiveUtils.getUVCenter(record.primitives()[adjIndex]);
                            neighborOffsets.add(new Vec2(adjCenter.x - center.x, adjCenter.y - center.y));
                        }
                    }
                    cache.neighborhoodHashMap.put(i, PrimitiveUtils.calculateNeighborhoodHash(neighborOffsets));
                }
                
                cacheMap.put(record, cache);
            }
        }
        
        cacheValid = true;
    }

    @Override
    public BindingContext genContext() {
        for (BuiltRecord record : records) {
            BindingContext context = new BindingContext(target, true);
            record.setupContext(context);
            if (!context.available()) continue;
            bindingContext = context;
            currentRecord = record;
            bindingContext.init();
            return context;
        }
        return BindingContext.EMPTY;
    }

    private void drawPrimitive(GuiGraphics graphics, VertexData[] primitive, int argb, int offset) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (VertexData vertex : primitive) buffer.addVertex(vertex.x(), vertex.y(), vertex.z() + offset).setColor(argb);
        if (primitive.length == 3) buffer.addVertex(primitive[2].x(), primitive[2].y(), primitive[2].z() + offset).setColor(argb);
        graphics.flush();
    }

    private void drawNormal(GuiGraphics graphics, Vec3 start, Vec3 normal, int length, int argb) {
        Vec3 end = normal.scale(length).add(start);
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.lineStrip());
        buffer.addVertex((float) start.x(), (float) start.y(), (float) (start.z() + 1200f)).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        buffer.addVertex((float) end.x(), (float) end.y(), (float) (end.z() + 1200f)).setColor(argb).setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
        graphics.flush();
    }

    private void drawFocused(GuiGraphics graphics) {
        if (focusedIndex == -1 || focusedRecord == null) return;
        VertexData[] focused = focusedRecord.primitives()[focusedIndex];
        drawPrimitive(graphics, focused, focusedArgb, 1100);
        int length = focused.length;
        VertexData[] reversed = new VertexData[length];
        for (int i = 0; i < length; i++) reversed[i] = focused[length - 1 - i];
        drawPrimitive(graphics, reversed, focusedArgb, 1100);
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
        resultIndexes.forEach(i -> drawPrimitive(graphics, primitives[i], sideArgb, 1000));
    }

    record Triple(double depth, BuiltRecord record, int index) {}
    
    public ExcludedRegion generateExclusionSignature() {
        if (focusedIndex == -1 || focusedRecord == null) return null;
        
        RecordCache cache = cacheMap.get(focusedRecord);
        if (cache == null) return null;
        
        VertexData[] targetPrimitive = focusedRecord.primitives()[focusedIndex];
        
        // Get cached UV center (normal no longer needed for rotation invariance)
        Vec2 uvCenter = cache.uvCenterMap.get(focusedIndex);
        
        if (uvCenter == null) return null;
        
        // Create the excluded region (passing Vec3.ZERO for normal - it's ignored in matching)
        ExcludedRegion region = new ExcludedRegion(focusedRecord.textureId(), uvCenter, Vec3.ZERO);
        
        // Use cached adjacency information
        List<Integer> adjacentIndices = cache.adjacencyMap.get(focusedIndex);
        if (adjacentIndices != null) {
            for (int adjIndex : adjacentIndices) {
                Vec2 adjacentCenter = cache.uvCenterMap.get(adjIndex);
                if (adjacentCenter != null) {
                    region.addNeighborOffset(adjacentCenter.x - uvCenter.x, adjacentCenter.y - uvCenter.y);
                }
            }
        }
        
        // Calculate UV bounds and area
        float minU = Float.MAX_VALUE, maxU = Float.MIN_VALUE;
        float minV = Float.MAX_VALUE, maxV = Float.MIN_VALUE;
        for (VertexData vertex : targetPrimitive) {
            minU = Math.min(minU, vertex.u());
            maxU = Math.max(maxU, vertex.u());
            minV = Math.min(minV, vertex.v());
            maxV = Math.max(maxV, vertex.v());
        }
        region.setBounds(minU, maxU, minV, maxV);
        
        // Set vertex count for better matching
        region.setVertexCount(targetPrimitive.length);
        
        // Calculate UV area using shoelace formula for better uniqueness
        float area = 0;
        for (int i = 0; i < targetPrimitive.length; i++) {
            int j = (i + 1) % targetPrimitive.length;
            area += targetPrimitive[i].u() * targetPrimitive[j].v();
            area -= targetPrimitive[j].u() * targetPrimitive[i].v();
        }
        region.setUVArea(Math.abs(area) / 2.0f);
        
        return region;
    }
    
    // Generate exclusion regions for all layers under the mouse
    public List<ExcludedRegion> generateAllLayersExclusions() {
        List<ExcludedRegion> regions = new ArrayList<>();
        
        if (lastSortByDepth == null || lastSortByDepth.isEmpty()) {
            return regions;
        }
        
        // Save current focused state
        BuiltRecord savedFocusedRecord = focusedRecord;
        int savedFocusedIndex = focusedIndex;
        
        // Generate exclusion for each layer
        for (Triple triple : lastSortByDepth) {
            focusedRecord = triple.record;
            focusedIndex = triple.index;
            
            ExcludedRegion region = generateExclusionSignature();
            if (region != null) {
                regions.add(region);
            }
        }
        
        // Restore original focused state
        focusedRecord = savedFocusedRecord;
        focusedIndex = savedFocusedIndex;
        
        return regions;
    }
    
    // Get the count of available layers under the mouse
    public int getAvailableLayerCount() {
        return lastSortByDepth != null ? lastSortByDepth.size() : 0;
    }
    
    // Collect exclusion regions for all primitives in brush area using current frame data
    public List<ExcludedRegion> collectExclusionsInBrushArea(int centerX, int centerY, int brushRadius) {
        List<ExcludedRegion> regions = new ArrayList<>();
        
        // Use current records which already have the correct screen coordinates
        // These were updated in the last updateModel call and are in sync with what's rendered
        for (BuiltRecord record : records) {
            if (UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())) {
                continue;
            }
            
            VertexData[][] primitives = record.primitives();
            for (int i = 0; i < primitives.length; i++) {
                VertexData[] primitive = primitives[i];
                
                // Check if this primitive intersects with the brush area
                // Using the current screen coordinates from the rendered model
                if (primitiveIntersectsBrush(primitive, centerX, centerY, brushRadius)) {
                    // Save current state
                    BuiltRecord savedFocusedRecord = focusedRecord;
                    int savedFocusedIndex = focusedIndex;
                    
                    // Temporarily set focus to this primitive to generate its exclusion
                    focusedRecord = record;
                    focusedIndex = i;
                    
                    ExcludedRegion region = generateExclusionSignature();
                    if (region != null) {
                        regions.add(region);
                    }
                    
                    // Restore state
                    focusedRecord = savedFocusedRecord;
                    focusedIndex = savedFocusedIndex;
                }
            }
        }
        
        return regions;
    }
    
    // Analyze with brush area detection for better coverage
    public void analyseWithBrush(int entitySize, int centerX, int centerY, int brushRadius, int layers, boolean hideDisabled, String idInField) {
        target.setScale(target.getScale() * entitySize);
        genContext();
        records.removeIf(record -> {
            boolean isIdInField = !idInField.isBlank() && record.textureId().contains(idInField);
            return (hideDisabled && isIdInField) || (!isIdInField && target.getDisabledTextureIds().stream().anyMatch(record.textureId()::contains));
        });
        
        List<Triple> sortByDepth = new ArrayList<>();
        records.stream().filter(record -> !UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())).forEach(record -> {
            VertexData[][] primitives = record.primitives();
            for (int i = 0, primitiveCount = primitives.length; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                
                // Check if any part of the primitive intersects with the brush circle
                if (primitiveIntersectsBrush(primitive, centerX, centerY, brushRadius)) {
                    VertexData point = primitive[0];
                    // Calculate approximate depth at center point
                    double deltaZ = point.normalZ() == 0 ? 0 : 
                        (point.normalX() * (centerX - point.x()) + point.normalY() * (centerY - point.y())) / point.normalZ();
                    sortByDepth.add(new Triple(point.z() - deltaZ, record, i));
                }
            }
        });
        
        // Store the sorted list for brush mode
        lastSortByDepth = new ArrayList<>(sortByDepth);
        
        if (!sortByDepth.isEmpty()) {
            sortByDepth.sort(Comparator.comparingDouble(triple -> -triple.depth));
            if (layers < sortByDepth.size()) {
                Triple result = sortByDepth.get(layers);
                focusedRecord = result.record;
                focusedIndex = result.index;
            }
        }
    }
    
    // Check if a primitive intersects with the brush circle area
    private boolean primitiveIntersectsBrush(VertexData[] primitive, int centerX, int centerY, int radius) {
        // First check: if any vertex is within the brush radius
        for (VertexData vertex : primitive) {
            double dx = vertex.x() - centerX;
            double dy = vertex.y() - centerY;
            if (dx * dx + dy * dy <= radius * radius) {
                return true;
            }
        }
        
        // Second check: if the center point is inside the polygon (using float coordinates)
        double[] xs = new double[primitive.length];
        double[] ys = new double[primitive.length];
        for (int i = 0; i < primitive.length; i++) {
            xs[i] = primitive[i].x();
            ys[i] = primitive[i].y();
        }
        if (pointInPolygon(centerX, centerY, xs, ys)) {
            return true;
        }
        
        // Third check: if any edge intersects with the brush circle
        for (int i = 0; i < primitive.length; i++) {
            int j = (i + 1) % primitive.length;
            if (lineIntersectsCircle(primitive[i].x(), primitive[i].y(), 
                                    primitive[j].x(), primitive[j].y(), 
                                    centerX, centerY, radius)) {
                return true;
            }
        }
        
        return false;
    }
    
    // More accurate point-in-polygon test using floating point
    private boolean pointInPolygon(double x, double y, double[] xs, double[] ys) {
        int n = xs.length;
        boolean inside = false;
        
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((ys[i] > y) != (ys[j] > y) &&
                x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i]) {
                inside = !inside;
            }
        }
        
        return inside;
    }
    
    // Check if a line segment intersects with a circle
    private boolean lineIntersectsCircle(double x1, double y1, double x2, double y2, 
                                        double cx, double cy, double radius) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double fx = x1 - cx;
        double fy = y1 - cy;
        
        double a = dx * dx + dy * dy;
        double b = 2 * (fx * dx + fy * dy);
        double c = (fx * fx + fy * fy) - radius * radius;
        
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return false;
        }
        
        discriminant = Math.sqrt(discriminant);
        double t1 = (-b - discriminant) / (2 * a);
        double t2 = (-b + discriminant) / (2 * a);
        
        return (t1 >= 0 && t1 <= 1) || (t2 >= 0 && t2 <= 1) || (t1 < 0 && t2 > 1);
    }
    
    private List<VertexData[]> findAdjacentPrimitives(int targetIndex, VertexData[][] allPrimitives) {
        List<VertexData[]> adjacents = new ArrayList<>();
        VertexData[] target = allPrimitives[targetIndex];
        
        for (int i = 0; i < allPrimitives.length; i++) {
            if (i == targetIndex) continue;
            VertexData[] primitive = allPrimitives[i];
            if (intersects(primitive, List.<VertexData[]>of(target))) {
                adjacents.add(primitive);
            }
        }
        
        return adjacents;
    }
    
    private Vec2 calculateUVCenter(VertexData[] primitive) {
        float u = 0, v = 0;
        for (VertexData vertex : primitive) {
            u += vertex.u();
            v += vertex.v();
        }
        return new Vec2(u / primitive.length, v / primitive.length);
    }
    
    // Render model with excluded parts hidden
    public void renderWithExclusions(GuiGraphics graphics, List<ExcludedRegion> exclusions) {
        if (exclusions == null || exclusions.isEmpty()) {
            // If no exclusions, render normally
            records.forEach(record -> VertexData.renderVertices(record.vertices(), graphics.bufferSource().getBuffer(record.renderType())));
            return;
        }
        
        // Render each record with exclusions applied
        for (BuiltRecord record : records) {
            RecordCache cache = cacheMap.get(record);
            
            // Pre-filter exclusions by texture ID
            List<ExcludedRegion> relevantExclusions = new ArrayList<>();
            for (ExcludedRegion excluded : exclusions) {
                if (record.textureId().contains(excluded.getTextureId())) {
                    relevantExclusions.add(excluded);
                }
            }
            
            if (relevantExclusions.isEmpty()) {
                // No exclusions for this record, render all
                VertexData.renderVertices(record.vertices(), graphics.bufferSource().getBuffer(record.renderType()));
                continue;
            }
            
            // Mark which primitives to exclude
            boolean[] excludeMask = new boolean[record.primitives().length];
            
            if (cache != null) {
                for (int i = 0; i < record.primitives().length; i++) {
                    VertexData[] primitive = record.primitives()[i];
                    Vec2 uvCenter = cache.uvCenterMap.get(i);
                    // Normal no longer needed - rotation invariant matching
                    Float hash = cache.neighborhoodHashMap.get(i);
                    
                    if (uvCenter == null) continue;
                    
                    // Calculate area
                    float area = 0;
                    for (int vi = 0; vi < primitive.length; vi++) {
                        int vj = (vi + 1) % primitive.length;
                        area += primitive[vi].u() * primitive[vj].v();
                        area -= primitive[vj].u() * primitive[vi].v();
                    }
                    area = Math.abs(area) / 2.0f;
                    
                    // Check if this primitive should be excluded
                    for (ExcludedRegion excluded : relevantExclusions) {
                        // Pass Vec3.ZERO for normal - it's ignored in rotation-invariant matching
                        if (excluded.matchesUV(uvCenter, Vec3.ZERO, hash != null ? hash : 0, primitive.length, area)) {
                            excludeMask[i] = true;
                            break;
                        }
                    }
                }
            }
            
            // Render only non-excluded primitives
            List<VertexData> filteredVertices = new ArrayList<>();
            for (int i = 0; i < record.primitives().length; i++) {
                if (!excludeMask[i]) {
                    for (VertexData vertex : record.primitives()[i]) {
                        filteredVertices.add(vertex);
                    }
                }
            }
            
            if (!filteredVertices.isEmpty()) {
                VertexData.renderVertices(filteredVertices.toArray(new VertexData[0]), 
                    graphics.bufferSource().getBuffer(record.renderType()));
            }
        }
    }
    
    public void drawExcludedRegions(GuiGraphics graphics, int entitySize) {
        // In exclusion mode, only highlight the currently focused primitive
        // Since excluded parts are already hidden, we don't need to draw them in red
        
        // Highlight the currently focused primitive for selection feedback
        if (focusedIndex != -1 && focusedRecord != null) {
            VertexData[] focused = focusedRecord.primitives()[focusedIndex];
            
            // Check if this primitive is already excluded
            boolean isExcluded = false;
            RecordCache cache = cacheMap.get(focusedRecord);
            if (cache != null) {
                Vec2 uvCenter = cache.uvCenterMap.get(focusedIndex);
                // Normal no longer needed - rotation invariant matching
                Float hash = cache.neighborhoodHashMap.get(focusedIndex);
                
                if (uvCenter != null) {
                    // Calculate area
                    float area = 0;
                    for (int vi = 0; vi < focused.length; vi++) {
                        int vj = (vi + 1) % focused.length;
                        area += focused[vi].u() * focused[vj].v();
                        area -= focused[vj].u() * focused[vi].v();
                    }
                    area = Math.abs(area) / 2.0f;
                    
                    // Check if excluded
                    for (ExcludedRegion excluded : selectedExclusions) {
                        if (focusedRecord.textureId().contains(excluded.getTextureId()) &&
                            // Pass Vec3.ZERO for normal - it's ignored in rotation-invariant matching
                            excluded.matchesUV(uvCenter, Vec3.ZERO, hash != null ? hash : 0, focused.length, area)) {
                            isExcluded = true;
                            break;
                        }
                    }
                }
            }
            
            // Draw with different colors: green if not excluded, red if excluded
            int highlightColor = isExcluded ? 0x7FFF0000 : 0x7F00FF00; // Red or Green
            drawPrimitive(graphics, focused, highlightColor, 1100);
        }
    }
}
