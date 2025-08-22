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
        float u0 = primitive[0].u(), v0 = primitive[0].v(), u1 = primitive[1].u(), v1 = primitive[1].v(), u2 = primitive[2].u(), v2 = primitive[2].v();
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return primitive[0].pos().scale(alpha).add(primitive[1].pos().scale(beta)).add(primitive[2].pos().scale(1 - alpha - beta));
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
            primitives[i][0] = vertices[startWithFirst ? 0 : k];
            System.arraycopy(vertices, k + 1, primitives[i], 1, primitiveLength - 1);
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
        // Collect all available contexts with their targets
        List<BindingContext> availableContexts = new ArrayList<>();
        
        for (BindingTarget target : ConfigFile.config().getTargetList()) {
            for (BuiltRecord record : records) {
                BindingContext context = new BindingContext(target, false);
                record.setupContext(context);
                if (context.available()) {
                    availableContexts.add(context);
                }
            }
        }
        
        if (availableContexts.isEmpty()) {
            return BindingContext.EMPTY;
        }
        
        // Sort contexts: 
        // 1. Prefer targets with excludedRegions (non-empty)
        // 2. Then by priority (higher first)
        availableContexts.sort((c1, c2) -> {
            boolean has1 = c1.target != null && c1.target.getExcludedRegions() != null && !c1.target.getExcludedRegions().isEmpty();
            boolean has2 = c2.target != null && c2.target.getExcludedRegions() != null && !c2.target.getExcludedRegions().isEmpty();
            
            // If one has excludedRegions and the other doesn't, prefer the one with excludedRegions
            if (has1 && !has2) return -1;
            if (!has1 && has2) return 1;
            
            // Otherwise, sort by priority
            int p1 = c1.target != null ? c1.target.getPriority() : 0;
            int p2 = c2.target != null ? c2.target.getPriority() : 0;
            return Integer.compare(p2, p1); // Higher priority first
        });
        
        // Log the selected target for debugging
        BindingContext selectedContext = availableContexts.get(0);
        if (selectedContext.target != null) {
            boolean hasExclusions = selectedContext.target.getExcludedRegions() != null && 
                                   !selectedContext.target.getExcludedRegions().isEmpty();
            System.out.println("[RealCamera] Selected target: " + selectedContext.target.name + 
                             " (priority=" + selectedContext.target.getPriority() + 
                             ", excludedRegions=" + (hasExclusions ? selectedContext.target.getExcludedRegions().size() : 0) + ")");
        }
        
        // Return the best matching context
        return selectedContext;
    }

    public record BuiltRecord(RenderType renderType, String textureId, VertexData[] vertices, VertexData[][] primitives) {
        public Optional<VertexData[]> findPrimitive(float u, float v) {
            final int resolution = 1000000;
            for (VertexData[] primitive : primitives) {
                int[] us = new int[primitive.length], vs = new int[primitive.length];
                for (int i = 0; i < primitive.length; i++) {
                    us[i] = (int) (resolution * primitive[i].u());
                    vs[i] = (int) (resolution * primitive[i].v());
                }
                if (new Polygon(us, vs, primitive.length).contains(resolution * u, resolution * v)) return Optional.of(primitive);
            }
            return Optional.empty();
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
