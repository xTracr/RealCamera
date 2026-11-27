package com.xtracr.realcamera.config.codec;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTargetStore;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.config.binding.TargetConfig;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves the binding targets of released versions, which were stored as lists inside the main configuration file,
 * into their own target files.
 */
public final class ConfigFileMigrator {
    /**
     * Imports the legacy target lists into the store, if the main configuration still contains them. Existing
     * files are never replaced, so this can run on every load until the main file has been rewritten.
     */
    public static void migrateLegacyTargets(@Nullable JsonObject root, Gson gson, BindTargetStore store) {
        if (root == null) return;
        try {
            List<BindTarget> legacy = decodeLegacyTargets(root, gson);
            if (legacy.isEmpty()) return;
            legacy.forEach(store::putIfAbsent);
            store.save();
        } catch (RuntimeException exception) {
            RealCamera.LOGGER.warn("Failed to migrate the legacy binding targets", exception);
        }
    }

    private static List<BindTarget> decodeLegacyTargets(JsonObject root, Gson gson) {
        if (!root.has("binding") || !root.get("binding").isJsonObject()) return List.of();
        JsonObject binding = root.getAsJsonObject("binding");
        List<BindTarget> targets = new ArrayList<>();
        for (var list : List.of("targetList", "fixedTargetList")) {
            if (!binding.has(list) || !binding.get(list).isJsonArray()) continue;
            for (JsonElement element : binding.getAsJsonArray(list)) {
                if (!element.isJsonObject()) continue;
                JsonObject old = element.getAsJsonObject();
                if (!old.has("name") || old.get("name").isJsonNull()) continue;
                Identifier id = ConfigDecoder703.legacyId(old.get("name").getAsString());
                targets.add(new BindTarget(id, readTargetConfig(old, id, gson),
                        readRenderConfig(old, gson), readOffsets(old, gson), readPriority(old, id)));
            }
        }
        return targets;
    }

    private static TargetConfig readTargetConfig(JsonObject old, Identifier id, Gson gson) {
        if (id.getNamespace().equals("legacy")) return TargetConfig.EMPTY;
        if (!old.has("targetConfig") || !old.get("targetConfig").isJsonObject()) return TargetConfig.EMPTY;
        if (!old.has("textureId") || old.get("textureId").isJsonNull()) return TargetConfig.EMPTY;
        JsonObject target = old.getAsJsonObject("targetConfig").deepCopy();
        target.add("textureId", old.get("textureId"));
        TargetConfig config = gson.fromJson(target, TargetConfig.class);
        return config == null ? TargetConfig.EMPTY : config;
    }

    private static OffsetConfig readOffsets(JsonObject old, Gson gson) {
        JsonObject offsets = old.has("offsets") && old.get("offsets").isJsonObject()
                ? old.getAsJsonObject("offsets").deepCopy() : new JsonObject();
        // 0.7.x kept the bind flags in a separate object.
        if (old.has("bindConfig") && old.get("bindConfig").isJsonObject()) {
            old.getAsJsonObject("bindConfig").entrySet().forEach(entry -> offsets.add(entry.getKey(), entry.getValue()));
        }
        OffsetConfig config = gson.fromJson(offsets, OffsetConfig.class);
        return config == null ? new OffsetConfig() : config;
    }

    private static RenderConfig readRenderConfig(JsonObject old, Gson gson) {
        JsonObject render = new JsonObject();
        if (old.has("disablingDepth")) render.add("disablingDepth", old.get("disablingDepth"));
        if (old.has("disableConfigs")) render.add("disableConfigs", old.get("disableConfigs"));
        RenderConfig config = gson.fromJson(render, RenderConfig.class);
        return config == null ? new RenderConfig() : config;
    }

    private static int readPriority(JsonObject old, Identifier id) {
        return old.has("priority") && !old.get("priority").isJsonNull()
                ? old.get("priority").getAsInt() : BindTarget.defaultPriority(id);
    }
}
