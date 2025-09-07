package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexRecorder {
    protected static final Pattern textureIdPattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
    protected final List<BuiltRecord> records = new ArrayList<>();

    protected static Vec3 getPosition(VertexData[] primitive, float u, float v) {
        if (primitive.length < 3) return primitive[0].pos();
        float u0 = primitive[0].u(), v0 = primitive[0].v();
        float u1 = primitive[1].u(), v1 = primitive[1].v();
        float u2 = primitive[2].u(), v2 = primitive[2].v();
        float denom = (u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2);
        if (denom == 0.0f) denom = 1.0e-12f;
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / denom;
        float denom2 = (u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0);
        if (denom2 == 0.0f) denom2 = 1.0e-12f;
        float beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / denom2;
        float gamma = 1 - alpha - beta;
        double x = primitive[0].x() * alpha + primitive[1].x() * beta + primitive[2].x() * gamma;
        double y = primitive[0].y() * alpha + primitive[1].y() * beta + primitive[2].y() * gamma;
        double z = primitive[0].z() * alpha + primitive[1].z() * beta + primitive[2].z() * gamma;
        return new Vec3(x, y, z);
    }

    public static BuiltRecord buildVertices(RenderType renderType, VertexData[] vertices) {
        String renderTypeName = renderType.toString();
        Matcher matcher = textureIdPattern.matcher(renderTypeName);
        String textureId = matcher.find() ? matcher.group(1) : renderTypeName;
        VertexFormat.Mode drawMode = renderType.mode();
        final int primitiveLength = drawMode.primitiveLength, primitiveStride = drawMode.primitiveStride;
        final int primitiveCount = (vertices.length - primitiveLength) / primitiveStride + 1;
        final boolean startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
        VertexData[][] primitives = new VertexData[primitiveCount][primitiveLength];
        for (int i = 0, k = 0; i < primitiveCount; i++, k += primitiveStride) {
            VertexData[] prim = primitives[i];
            prim[0] = vertices[startWithFirst ? 0 : k];
            // Inline copy to avoid System.arraycopy call overhead in hot path
            for (int j = 1; j < primitiveLength; j++) {
                prim[j] = vertices[k + j];
            }
        }
        return new BuiltRecord(renderType, textureId, vertices, primitives);
    }

    public List<BuiltRecord> records() {
        return records;
    }

    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        MultiVertexCatcher catcher = new SimpleMultiVertexCatcher();
        dispatcher.render(entity, 0, 0, 0, Mth.lerp(deltaTick, entity.yRotO, entity.getYRot()), deltaTick, poseStack, catcher, dispatcher.getPackedLightCoords(entity, deltaTick));
        catcher.sendVertices(this);
    }

    public BindingContext genContext() {
        // Single-pass best selection to avoid building/sorting lists per frame
        BindingContext best = BindingContext.EMPTY;
        boolean bestHasExcluded = false;
        int bestPriority = Integer.MIN_VALUE;

        for (BindingTarget target : ConfigFile.config().getTargetList()) {
            final boolean targetHasExcluded = target.getExcludedRegions() != null && !target.getExcludedRegions().isEmpty();
            final int targetPriority = target.getPriority();

            for (BuiltRecord record : records) {
                BindingContext context = new BindingContext(target, false);
                record.setupContext(context);
                if (!context.available()) continue;

                // Better if it has excluded regions when current best doesn't, or higher priority when tie
                if ((targetHasExcluded && !bestHasExcluded) ||
                    (targetHasExcluded == bestHasExcluded && targetPriority > bestPriority)) {
                    best = context;
                    bestHasExcluded = targetHasExcluded;
                    bestPriority = targetPriority;
                }
            }
        }

        return best;
    }

    public record BuiltRecord(RenderType renderType, String textureId, VertexData[] vertices, VertexData[][] primitives) {
        public Optional<VertexData[]> findPrimitive(float u, float v) {
            for (VertexData[] primitive : primitives) {
                if (pointInPolygon(u, v, primitive)) {
                    return Optional.of(primitive);
                }
            }
            return Optional.empty();
        }

        // Ray casting point-in-polygon test in UV space (allocation-free)
        private boolean pointInPolygon(float u, float v, VertexData[] primitive) {
            boolean inside = false;
            int n = primitive.length;
            if (n == 0) return false;
            int j = n - 1;
            for (int i = 0; i < n; j = i++) {
                float ui = primitive[i].u();
                float vi = primitive[i].v();
                float uj = primitive[j].u();
                float vj = primitive[j].v();
                boolean intersect = ((vi > v) != (vj > v));
                if (intersect) {
                    float denom = (vj - vi);
                    if (denom == 0.0f) denom = 1.0e-12f;
                    float t = (v - vi) / denom;
                    float x = ui + (uj - ui) * t;
                    if (u < x) inside = !inside;
                }
            }
            return inside;
        }

        public void setupContext(BindingContext context) {
            BindingTarget target = context.target;
            // Improved matching: More specific texture ID matching
            if (!matchesTextureId(textureId, target.textureId)) return;
            findPrimitive(target.getPosU(), target.getPosV()).ifPresent(primitive -> context.setPosition(getPosition(primitive, target.getPosU(), target.getPosV())));
            findPrimitive(target.getForwardU(), target.getForwardV()).ifPresent(primitive -> context.setForward(primitive[0].normal()));
            findPrimitive(target.getUpwardU(), target.getUpwardV()).ifPresent(primitive -> context.setUpward(primitive[0].normal()));
        }

        public void setupContext(BindingContext context, Matrix4f positionMatrix, Matrix3f normalMatrix) {
            BindingTarget target = context.target;
            // Improved matching: More specific texture ID matching
            if (!matchesTextureId(textureId, target.textureId)) return;
            findPrimitive(target.getPosU(), target.getPosV()).ifPresent(primitive ->
                    context.setPosition(new Vec3(getPosition(primitive, target.getPosU(), target.getPosV()).toVector3f().mulPosition(positionMatrix))));
            findPrimitive(target.getForwardU(), target.getForwardV()).ifPresent(primitive ->
                    context.setForward(new Vec3(primitive[0].normal().toVector3f().mul(normalMatrix))));
            findPrimitive(target.getUpwardU(), target.getUpwardV()).ifPresent(primitive ->
                    context.setUpward(new Vec3(primitive[0].normal().toVector3f().mul(normalMatrix))));
        }
        
        private boolean matchesTextureId(String recordTexture, String targetTexture) {
            // If target texture is empty, it matches any texture
            if (targetTexture == null || targetTexture.isEmpty()) {
                return true;
            }
            
            // Prioritize exact match
            if (recordTexture.equals(targetTexture)) {
                return true;
            }
            
            // For player skins, use more specific matching
            if (targetTexture.equals("minecraft:skins/")) {
                // Only match skins, not regular player textures
                return recordTexture.contains("minecraft:skins/") || 
                       recordTexture.contains("/skin/") ||
                       recordTexture.contains("_skin");
            }
            
            if (targetTexture.equals("minecraft:textures/entity/player/")) {
                // Only match player entity textures, not skins
                return recordTexture.contains("minecraft:textures/entity/player/") &&
                       !recordTexture.contains("minecraft:skins/");
            }
            
            // Default: use contains for backward compatibility
            return recordTexture.contains(targetTexture);
        }
    }
}
