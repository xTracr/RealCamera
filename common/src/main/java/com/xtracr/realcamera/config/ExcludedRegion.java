package com.xtracr.realcamera.config;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a region of a model that should be excluded from rendering.
 * Uses UV neighborhood signatures to precisely identify regions even when
 * multiple parts share the same texture.
 */
public class ExcludedRegion {
    private String textureId;
    private float centerU;
    private float centerV;
    private float tolerance = 0.001f; // 进一步收紧容差到0.001，提高精度
    
    // UV neighborhood signature - simplified for performance
    private List<UVOffset> neighborhoodPattern = new ArrayList<>();
    private float neighborhoodHash = 0; // Pre-computed hash for fast matching
    
    // DEPRECATED: Normal no longer used due to rotation dependency
    // Kept for backward compatibility but not used in matching
    @Deprecated
    private float normalX;
    @Deprecated
    private float normalY;
    @Deprecated
    private float normalZ;
    
    // Optional: UV bounding box for quick rejection
    private float minU;
    private float maxU;
    private float minV;
    private float maxV;
    
    // Enhanced features for better uniqueness
    private int vertexCount = 3;    // Number of vertices (3 for triangle, 4 for quad)
    private float uvArea = 0;       // Area in UV space for additional verification
    private float aspectRatio = 1;  // Width/height ratio in UV space
    
    // For Gson serialization
    public ExcludedRegion() {}
    
    public ExcludedRegion(String textureId, Vec2 center, Vec3 normal) {
        this.textureId = textureId;
        this.centerU = center.x;
        this.centerV = center.y;
        this.normalX = (float) normal.x;
        this.normalY = (float) normal.y;
        this.normalZ = (float) normal.z;
    }
    
    // Enhanced pattern matching WITHOUT normal (rotation-invariant)
    public boolean matchesUV(Vec2 uv, Vec3 normal, float targetHash, int targetVertexCount, float targetArea) {
        // NOTE: normal parameter is kept for API compatibility but IGNORED to ensure rotation invariance
        
        // First check vertex count - must match exactly
        if (targetVertexCount > 0 && vertexCount > 0 && targetVertexCount != vertexCount) {
            return false;
        }
        
        // Check UV area with very tight tolerance (1% for high precision)
        if (uvArea > 0 && targetArea > 0) {
            float areaDiff = Math.abs(uvArea - targetArea);
            if (areaDiff > uvArea * 0.01f) { // Ultra-tight 1% tolerance
                return false;
            }
        }
        
        // Ultra-precise UV distance check using Euclidean distance
        float uvDistSq = (uv.x - centerU) * (uv.x - centerU) + (uv.y - centerV) * (uv.y - centerV);
        if (uvDistSq > tolerance * tolerance) { // tolerance is now 0.001
            return false;
        }
        
        // REMOVED: Normal matching - causes rotation dependency issues
        // Normal is completely ignored to ensure rotation invariance
        
        // Very strict hash comparison for maximum uniqueness
        if (neighborhoodHash > 0 && targetHash > 0) {
            float hashDiff = Math.abs(neighborhoodHash - targetHash);
            if (hashDiff > 20) { // Further tightened from 30 to 20
                return false;
            }
        }
        
        // Additional check: if bounding box is set, verify UV is within bounds
        if (minU != maxU && minV != maxV) { // Bounding box is set
            if (uv.x < minU - tolerance || uv.x > maxU + tolerance ||
                uv.y < minV - tolerance || uv.y > maxV + tolerance) {
                return false;
            }
        }
        
        return true;
    }
    
    // Simplified version for backward compatibility
    public boolean matchesUV(Vec2 uv, Vec3 normal, float targetHash) {
        return matchesUV(uv, normal, targetHash, 0, 0);
    }
    
    // Original method for backward compatibility
    public boolean matchesUV(Vec2 uv, Vec3 normal, List<Vec2> adjacentUVs) {
        // Convert adjacentUVs to hash for comparison
        float targetHash = 0;
        if (adjacentUVs != null && !adjacentUVs.isEmpty()) {
            targetHash = com.xtracr.realcamera.util.PrimitiveUtils.calculateNeighborhoodHash(adjacentUVs);
        }
        return matchesUV(uv, normal, targetHash);
    }
    
    private boolean matchesNeighborhoodPattern(Vec2 center, List<Vec2> adjacentUVs) {
        if (adjacentUVs.size() != neighborhoodPattern.size()) {
            return false;
        }
        
        // Calculate offsets from center
        List<UVOffset> currentOffsets = new ArrayList<>();
        for (Vec2 adjacent : adjacentUVs) {
            float du = adjacent.x - center.x;
            float dv = adjacent.y - center.y;
            currentOffsets.add(new UVOffset(du, dv));
        }
        
        // Try to match patterns (with rotation tolerance)
        return patternsMatch(currentOffsets, neighborhoodPattern, tolerance);
    }
    
    private boolean patternsMatch(List<UVOffset> pattern1, List<UVOffset> pattern2, float tol) {
        if (pattern1.size() != pattern2.size()) {
            return false;
        }
        
        // Simple distance-based matching
        // Could be improved with rotation-invariant matching
        for (int i = 0; i < pattern1.size(); i++) {
            UVOffset off1 = pattern1.get(i);
            UVOffset off2 = pattern2.get(i);
            float dist = (off1.du - off2.du) * (off1.du - off2.du) + 
                        (off1.dv - off2.dv) * (off1.dv - off2.dv);
            if (dist > tol * tol) {
                return false;
            }
        }
        return true;
    }
    
    // Quick bounds check
    public boolean isWithinBounds(float u, float v) {
        return u >= minU && u <= maxU && v >= minV && v <= maxV;
    }
    
    public void addNeighborOffset(float du, float dv) {
        neighborhoodPattern.add(new UVOffset(du, dv));
        // Recalculate hash when pattern changes
        recalculateHash();
    }
    
    private void recalculateHash() {
        if (neighborhoodPattern.isEmpty()) {
            neighborhoodHash = 0;
            return;
        }
        
        List<Vec2> offsets = new ArrayList<>();
        for (UVOffset offset : neighborhoodPattern) {
            offsets.add(new Vec2(offset.du, offset.dv));
        }
        neighborhoodHash = com.xtracr.realcamera.util.PrimitiveUtils.calculateNeighborhoodHash(offsets);
    }
    
    public void setBounds(float minU, float maxU, float minV, float maxV) {
        this.minU = minU;
        this.maxU = maxU;
        this.minV = minV;
        this.maxV = maxV;
        
        // Calculate aspect ratio
        float width = maxU - minU;
        float height = maxV - minV;
        if (height > 0) {
            this.aspectRatio = width / height;
        }
    }
    
    public void setVertexCount(int count) {
        this.vertexCount = count;
    }
    
    public void setUVArea(float area) {
        this.uvArea = area;
    }
    
    // Inner class for UV offsets
    public static class UVOffset {
        public float du;
        public float dv;
        
        public UVOffset() {}
        
        public UVOffset(float du, float dv) {
            this.du = du;
            this.dv = dv;
        }
    }
    
    // Getters and setters
    public String getTextureId() {
        return textureId;
    }
    
    public void setTextureId(String textureId) {
        this.textureId = textureId;
    }
    
    public Vec2 getCenter() {
        return new Vec2(centerU, centerV);
    }
    
    public Vec3 getNormal() {
        return new Vec3(normalX, normalY, normalZ);
    }
    
    public float getTolerance() {
        return tolerance;
    }
    
    public void setTolerance(float tolerance) {
        this.tolerance = tolerance;
    }
    
    public List<UVOffset> getNeighborhoodPattern() {
        return neighborhoodPattern;
    }
    
    public float getNeighborhoodHash() {
        return neighborhoodHash;
    }
    
    public int getVertexCount() {
        return vertexCount;
    }
    
    public float getUVArea() {
        return uvArea;
    }
    
    public float getAspectRatio() {
        return aspectRatio;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExcludedRegion that = (ExcludedRegion) o;
        return Float.compare(that.centerU, centerU) == 0 &&
               Float.compare(that.centerV, centerV) == 0 &&
               Objects.equals(textureId, that.textureId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(textureId, centerU, centerV);
    }
}