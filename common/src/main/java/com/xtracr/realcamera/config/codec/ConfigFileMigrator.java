package com.xtracr.realcamera.config.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public class ConfigFileMigrator {
    public static void migrateLegacyBinding(JsonObject root) {
        if (!root.has("binding") || !root.get("binding").isJsonObject()) return;
        JsonObject binding = root.getAsJsonObject("binding");
        if (binding.has("targetConfigs") || !binding.has("targetList")) return;
        JsonObject targets = new JsonObject(), offsets = new JsonObject(), renders = new JsonObject();
        for (String list : java.util.List.of("targetList", "fixedTargetList")) {
            if (!binding.has(list) || !binding.get(list).isJsonArray()) continue;
            for (JsonElement element : binding.getAsJsonArray(list)) {
                if (!element.isJsonObject()) continue;
                JsonObject old = element.getAsJsonObject();
                if (!old.has("name") || old.get("name").isJsonNull()) continue;
                Identifier id = ConfigDecoder703.legacyId(old.get("name").getAsString());
                String key = id.toString();
                if (old.has("targetConfig") && old.get("targetConfig").isJsonObject()
                        && old.has("textureId") && !old.get("textureId").isJsonNull()
                        && !id.getNamespace().equals("legacy")) {
                    JsonObject target = old.getAsJsonObject("targetConfig").deepCopy();
                    target.add("textureId", old.get("textureId"));
                    targets.add(key, target);
                }
                JsonObject offset = old.has("offsets") && old.get("offsets").isJsonObject()
                        ? old.getAsJsonObject("offsets").deepCopy() : new JsonObject();
                if (old.has("bindConfig") && old.get("bindConfig").isJsonObject()) {
                    old.getAsJsonObject("bindConfig").entrySet().forEach(entry -> offset.add(entry.getKey(), entry.getValue()));
                }
                offsets.add(key, offset);
                JsonObject render = new JsonObject();
                if (old.has("disablingDepth")) render.add("disablingDepth", old.get("disablingDepth"));
                if (old.has("disableConfigs")) render.add("disableConfigs", old.get("disableConfigs"));
                renders.add(key, render);
            }
        }
        binding.add("targetConfigs", targets);
        binding.add("offsetConfigs", offsets);
        binding.add("renderConfigs", renders);
    }
}
