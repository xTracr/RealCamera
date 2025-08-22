package com.xtracr.realcamera.util;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for primitive operations.
 * Provides unified algorithms for both UI and runtime to ensure consistency.
 */
public class PrimitiveUtils {
    // Unified epsilon for adjacency checks
    private static final float ADJACENCY_EPSILON = 1.0E-04f;
    
    /**
     * Check if two primitives are adjacent (share at least one vertex).
     * This is the single source of truth for adjacency checking.
     */
    public static boolean areAdjacent(VertexData[] p1, VertexData[] p2) {
        for (VertexData v1 : p1) {
            for (VertexData v2 : p2) {
                float distSq = (v1.x() - v2.x()) * (v1.x() - v2.x()) +
                               (v1.y() - v2.y()) * (v1.y() - v2.y()) +
                               (v1.z() - v2.z()) * (v1.z() - v2.z());
                if (distSq < ADJACENCY_EPSILON) {
                    return true; // Found shared vertex
                }
            }
        }
        return false;
    }
    
    /**
     * Calculate the UV center of a primitive.
     * Unified implementation for consistency.
     */
    public static Vec2 getUVCenter(VertexData[] primitive) {
        if (primitive == null || primitive.length == 0) {
            return new Vec2(0, 0);
        }
        
        float u = 0, v = 0;
        for (VertexData vertex : primitive) {
            u += vertex.u();
            v += vertex.v();
        }
        return new Vec2(u / primitive.length, v / primitive.length);
    }
    
    /**
     * Get the average normal of a primitive.
     * Some primitives might have varying normals, so we average them.
     */
    public static Vec3 getAverageNormal(VertexData[] primitive) {
        if (primitive == null || primitive.length == 0) {
            return Vec3.ZERO;
        }
        
        double x = 0, y = 0, z = 0;
        for (VertexData vertex : primitive) {
            Vec3 normal = vertex.normal();
            x += normal.x;
            y += normal.y;
            z += normal.z;
        }
        
        return new Vec3(x / primitive.length, y / primitive.length, z / primitive.length).normalize();
    }
    
    /**
     * Build adjacency map for all primitives in an array.
     * Returns a map where key is primitive index and value is list of adjacent primitive indices.
     */
    public static Map<Integer, List<Integer>> buildAdjacencyMap(VertexData[][] primitives) {
        Map<Integer, List<Integer>> adjacencyMap = new HashMap<>();
        
        for (int i = 0; i < primitives.length; i++) {
            adjacencyMap.put(i, new ArrayList<>());
        }
        
        // Check all pairs (only once)
        for (int i = 0; i < primitives.length; i++) {
            for (int j = i + 1; j < primitives.length; j++) {
                if (areAdjacent(primitives[i], primitives[j])) {
                    adjacencyMap.get(i).add(j);
                    adjacencyMap.get(j).add(i);
                }
            }
        }
        
        return adjacencyMap;
    }
    
    /**
     * Calculate a simple hash for neighborhood pattern.
     * This hash is rotation-invariant and used for quick matching.
     */
    public static float calculateNeighborhoodHash(List<Vec2> neighborOffsets) {
        if (neighborOffsets == null || neighborOffsets.isEmpty()) {
            return 0;
        }
        
        int count = neighborOffsets.size();
        float totalDist = 0;
        float minDist = Float.MAX_VALUE;
        float maxDist = 0;
        
        for (Vec2 offset : neighborOffsets) {
            float dist = offset.length();
            totalDist += dist;
            minDist = Math.min(minDist, dist);
            maxDist = Math.max(maxDist, dist);
        }
        
        // Create a hash that captures the neighborhood pattern
        // but is rotation-invariant
        float avgDist = totalDist / count;
        return count * 1000 + avgDist * 100 + (maxDist - minDist) * 10;
    }
    
    /**
     * Check if two neighborhood hashes match within tolerance.
     */
    public static boolean hashesMatch(float hash1, float hash2, float tolerance) {
        return Math.abs(hash1 - hash2) < tolerance;
    }
    
    /**
     * Calculate the area of a primitive in UV space using shoelace formula.
     */
    public static float calculateUVArea(VertexData[] primitive) {
        if (primitive == null || primitive.length < 3) {
            return 0;
        }
        
        float area = 0;
        for (int i = 0; i < primitive.length; i++) {
            int j = (i + 1) % primitive.length;
            area += primitive[i].u() * primitive[j].v();
            area -= primitive[j].u() * primitive[i].v();
        }
        return Math.abs(area) / 2.0f;
    }
}