package com.xtracr.realcamera.config.serialization;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.xtracr.realcamera.config.DisableConfig;
import com.xtracr.realcamera.config.UVRectangle;

import java.io.IOException;
import java.util.ArrayList;

public final class DisableConfigAdapter extends TypeAdapter<DisableConfig> {
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
