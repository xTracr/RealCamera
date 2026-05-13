package com.xtracr.realcamera.config;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.xtracr.realcamera.renderer.state.VertexData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;
import java.util.ArrayList;

@JsonAdapter(DisableConfig.Adapter.class)
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

    public static DisableConfig read(FriendlyByteBuf byteBuf) {
        String name = byteBuf.readUtf();
        String textureId = byteBuf.readUtf();
        boolean disableAll = byteBuf.readBoolean();
        UVRectangle[] rectangles = new UVRectangle[byteBuf.readVarInt()];
        for (int i = 0; i < rectangles.length; i++) rectangles[i] = UVRectangle.read(byteBuf);
        return new DisableConfig(name, textureId, disableAll, rectangles);
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

    public static final class Adapter extends TypeAdapter<DisableConfig> {
        @Override
        public void write(JsonWriter out, DisableConfig value) throws IOException {
            out.beginObject();
            out.name("name").value(value.name());
            out.name("textureId").value(value.textureId());
            out.name("disableAll").value(value.disableAll());
            out.name("rectangles");
            out.beginArray();
            for (UVRectangle rect : value.rectangles()) {
                out.beginObject();
                out.name("uMin").value(rect.uMin());
                out.name("vMin").value(rect.vMin());
                out.name("uMax").value(rect.uMax());
                out.name("vMax").value(rect.vMax());
                out.endObject();
            }
            out.endArray();
            out.endObject();
        }

        @Override
        public DisableConfig read(JsonReader in) throws IOException {
            in.beginObject();
            in.nextName();
            String name = in.nextString();
            in.nextName();
            String textureId = in.nextString();
            in.nextName();
            boolean disableAll = in.nextBoolean();
            in.nextName();
            in.beginArray();
            ArrayList<UVRectangle> rectangles = new ArrayList<>();
            while (in.hasNext()) {
                in.beginObject();
                in.nextName();
                float uMin = (float) in.nextDouble();
                in.nextName();
                float vMin = (float) in.nextDouble();
                in.nextName();
                float uMax = (float) in.nextDouble();
                in.nextName();
                float vMax = (float) in.nextDouble();
                in.endObject();
                rectangles.add(new UVRectangle(uMin, vMin, uMax, vMax));
            }
            in.endArray();
            in.endObject();
            return new DisableConfig(name, textureId, disableAll, rectangles.toArray(UVRectangle[]::new));
        }
    }
}
