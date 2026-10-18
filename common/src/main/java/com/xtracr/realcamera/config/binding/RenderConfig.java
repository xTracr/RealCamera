package com.xtracr.realcamera.config.binding;

import com.xtracr.realcamera.config.UVRectangle;
import com.xtracr.realcamera.renderer.state.VertexData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayList;
import java.util.List;

public final class RenderConfig {
    public float disablingDepth = 0.2f;
    public List<DisableConfig> disableConfigs = new ArrayList<>();

    public RenderConfig() {
    }

    public RenderConfig(float disablingDepth, List<DisableConfig> disableConfigs) {
        this.disablingDepth = disablingDepth;
        this.disableConfigs = new ArrayList<>(disableConfigs);
    }

    public static final class DisableConfig {
        private final transient LongOpenHashSet disabledUVs = new LongOpenHashSet(), enabledUVs = new LongOpenHashSet();
        private final String name;
        private final String textureId;
        private final boolean disableAll;
        private final List<UVRectangle> rectangles;

        /** Gson invokes this constructor so the transient caches are initialized on load. */
        @SuppressWarnings("unused")
        private DisableConfig() {
            this("", "", false, List.of());
        }

        public DisableConfig(String name, String textureId, boolean disableAll, List<UVRectangle> rectangles) {
            this.name = name;
            this.textureId = textureId;
            this.disableAll = disableAll;
            this.rectangles = List.copyOf(rectangles);
        }

        public String name() {
            return name;
        }

        public String textureId() {
            return textureId;
        }

        public boolean disableAll() {
            return disableAll;
        }

        public List<UVRectangle> rectangles() {
            return rectangles;
        }

        public boolean disable(VertexData vertex) {
            final float u = vertex.u(), v = vertex.v();
            final long packed = (long) Float.floatToIntBits(u) << 32 | Float.floatToIntBits(v);
            if (enabledUVs.contains(packed)) return false;
            if (disabledUVs.contains(packed)) return true;
            for (UVRectangle rect : rectangles) {
                if (!rect.contains(u, v)) continue;
                disabledUVs.add(packed);
                return true;
            }
            enabledUVs.add(packed);
            return false;
        }
    }

}
