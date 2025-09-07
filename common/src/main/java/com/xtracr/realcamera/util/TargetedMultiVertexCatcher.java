package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures only vertex streams whose texture id matches any of the accepted patterns.
 * Significantly reduces allocations versus capturing entire model each frame.
 */
public class TargetedMultiVertexCatcher implements MultiVertexCatcher {
    private static final Pattern TEXTURE_ID_PATTERN = Pattern.compile("texture\\[Optional\\[(.*?)]");

    private final List<SimpleMultiVertexCatcher.RecordBuilder> captured = new ArrayList<>();
    private final Set<String> accepted; // non-empty texture substrings to match

    private static final VertexConsumer SINK = new VertexConsumer() {
        @Override public @NotNull VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public @NotNull VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public @NotNull VertexConsumer setUv(float u, float v) { return this; }
        @Override public @NotNull VertexConsumer setUv1(int u, int v) { return this; }
        @Override public @NotNull VertexConsumer setUv2(int u, int v) { return this; }
        @Override public @NotNull VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public void addVertex(float x, float y, float z, int argb, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) { }
        @Override public @NotNull VertexConsumer setColor(int argb) { return this; }
        @Override public @NotNull VertexConsumer setOverlay(int overlay) { return this; }
        @Override public @NotNull VertexConsumer setLight(int light) { return this; }
    };

    public TargetedMultiVertexCatcher(Collection<String> acceptedTextureSubstrings) {
        this.accepted = new HashSet<>();
        for (String s : acceptedTextureSubstrings) {
            if (s != null && !s.isEmpty()) this.accepted.add(s);
        }
    }

    @Override
    public void sendVertices(VertexRecorder recorder) {
        recorder.records().clear();
        for (SimpleMultiVertexCatcher.RecordBuilder rb : captured) {
            recorder.records().add(VertexRecorder.buildVertices(rb.renderType(), rb.collectVertices()));
        }
    }

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
        if (shouldCapture(renderType)) {
            SimpleMultiVertexCatcher.RecordBuilder rb = new SimpleMultiVertexCatcher.RecordBuilder(renderType);
            captured.add(rb);
            return rb;
        }
        return SINK;
    }

    private boolean shouldCapture(RenderType renderType) {
        if (accepted.isEmpty()) return false;
        String id = extractTextureId(renderType);
        if (id.isEmpty()) return false;
        for (String s : accepted) {
            // Special handling aligning with VertexRecorder.BuiltRecord matching intent
            if ("minecraft:skins/".equals(s)) {
                if (id.contains("minecraft:skins/") || id.contains("/skin/") || id.contains("_skin")) return true;
            } else if ("minecraft:textures/entity/player/".equals(s)) {
                if (id.contains("minecraft:textures/entity/player/") && !id.contains("minecraft:skins/")) return true;
            } else if (id.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private static String extractTextureId(RenderType renderType) {
        String name = renderType.toString();
        Matcher m = TEXTURE_ID_PATTERN.matcher(name);
        return m.find() ? m.group(1) : name;
    }

    public List<VertexRecorder.BuiltRecord> buildRecords() {
        List<VertexRecorder.BuiltRecord> list = new ArrayList<>(captured.size());
        for (SimpleMultiVertexCatcher.RecordBuilder rb : captured) {
            list.add(VertexRecorder.buildVertices(rb.renderType(), rb.collectVertices()));
        }
        return list;
    }
}

