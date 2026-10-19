package com.xtracr.realcamera.config.binding;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class RenderConfig {
    public float disablingDepth = 0.2f;
    private transient @Nullable Map<String, DisableCache> disableCaches;
    private List<DisableConfig> disableConfigs = List.of();

    public RenderConfig() {
    }

    public RenderConfig(float disablingDepth, List<DisableConfig> disableConfigs) {
        this.disablingDepth = disablingDepth;
        this.disableConfigs = List.copyOf(disableConfigs);
    }

    public List<DisableConfig> disableConfigs() {
        // Gson may assign a mutable list directly when loading the configuration.
        return Collections.unmodifiableList(disableConfigs);
    }

    public void setDisableConfigs(List<DisableConfig> disableConfigs) {
        if (this.disableConfigs.equals(disableConfigs)) return;
        this.disableConfigs = List.copyOf(disableConfigs);
        disableCaches = null;
    }

    public DisableCache disableCache(String textureId) {
        if (disableCaches == null) disableCaches = new HashMap<>();
        return disableCaches.computeIfAbsent(textureId, id -> new DisableCache(id, disableConfigs));
    }

    public record DisableConfig(String name, String textureId, boolean disableAll, List<UVRectangle> rectangles) {
        public DisableConfig {
            // Missing JSON fields retain the defaults previously supplied by the no-argument constructor.
            name = Objects.requireNonNullElse(name, "");
            textureId = Objects.requireNonNullElse(textureId, "");
            rectangles = rectangles == null ? List.of() : List.copyOf(rectangles);
        }
    }

    public static final class DisableCache {
        private final boolean disableAll;
        private final List<UVRectangle> rectangles = new ArrayList<>();
        // 0 = unknown, 1 = visible, 2 = hidden; a hit needs only one lookup.
        private final transient Long2ByteMap uvResults;

        private DisableCache(String textureId, List<DisableConfig> configs) {
            boolean all = false;
            for (DisableConfig config : configs) {
                if (!textureId.contains(config.textureId())) continue;
                if (config.disableAll()) {
                    all = true;
                    break;
                }
                rectangles.addAll(config.rectangles());
            }
            disableAll = all;
            uvResults = all || rectangles.isEmpty() ? Long2ByteMaps.EMPTY_MAP : new Long2ByteOpenHashMap();
        }

        public boolean disableAll() {
            return disableAll;
        }

        public boolean disable(float u, float v) {
            if (disableAll || rectangles.isEmpty()) return disableAll;
            long packed = (long) Float.floatToRawIntBits(u) << 32 | Integer.toUnsignedLong(Float.floatToRawIntBits(v));
            byte cached = uvResults.get(packed);
            if (cached != 0) return cached == 2;
            boolean disabled = false;
            for (UVRectangle rect : rectangles) {
                if (!rect.contains(u, v)) continue;
                disabled = true;
                break;
            }
            uvResults.put(packed, (byte) (disabled ? 2 : 1));
            return disabled;
        }
    }
}
