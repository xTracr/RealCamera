package com.xtracr.realcamera.config;

import com.google.gson.annotations.JsonAdapter;
import com.xtracr.realcamera.config.serialization.DisableConfigAdapter;
import com.xtracr.realcamera.renderer.state.VertexData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.network.FriendlyByteBuf;

@JsonAdapter(DisableConfigAdapter.class)
public final class DisableConfig {
    private final LongOpenHashSet disabledUVs = new LongOpenHashSet(), enabledUVs = new LongOpenHashSet();
    private final String name;
    private final String textureId;
    private final boolean disableAll;
    private final UVRectangle[] rectangles;

    public DisableConfig(String name, String textureId, boolean disableAll, UVRectangle[] rectangles) {
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

    public UVRectangle[] rectangles() {
        return rectangles;
    }

    public void write(FriendlyByteBuf byteBuf) {
        byteBuf.writeUtf(name);
        byteBuf.writeUtf(textureId);
        byteBuf.writeBoolean(disableAll);
        byteBuf.writeVarInt(rectangles.length);
        for (UVRectangle rect : rectangles) rect.write(byteBuf);
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
