package com.xtracr.realcamera.config;

import com.google.gson.annotations.JsonAdapter;
import com.xtracr.realcamera.config.codec.DisableConfigAdapter;
import com.xtracr.realcamera.renderer.state.VertexData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.List;

@JsonAdapter(DisableConfigAdapter.class)
public final class DisableConfig {
    private final LongOpenHashSet disabledUVs = new LongOpenHashSet(), enabledUVs = new LongOpenHashSet();
    private final String name;
    private final String textureId;
    private final boolean disableAll;
    private final List<UVRectangle> rectangles;

    public DisableConfig(String name, String textureId, boolean disableAll, List<UVRectangle> rectangles) {
        this.name = name;
        this.textureId = textureId;
        this.disableAll = disableAll;
        this.rectangles = rectangles;
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
