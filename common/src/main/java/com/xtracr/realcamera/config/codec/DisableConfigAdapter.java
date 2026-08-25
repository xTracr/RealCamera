package com.xtracr.realcamera.config.codec;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.xtracr.realcamera.config.DisableConfig;
import com.xtracr.realcamera.config.UVRectangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class DisableConfigAdapter extends TypeAdapter<DisableConfig> {
    @Override
    public void write(JsonWriter out, DisableConfig value) throws IOException {
        out.beginObject();
        out.name("name").value(value.name());
        out.name("textureId").value(value.textureId());
        out.name("active").value(value.active());
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
        String name = "";
        String textureId = "";
        boolean active = false;
        boolean disableAll = false;
        List<UVRectangle> rectangles = new ArrayList<>();

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "name" -> name = in.nextString();
                case "textureId" -> textureId = in.nextString();
                case "active" -> active = in.nextBoolean();
                case "disableAll" -> disableAll = in.nextBoolean();
                case "rectangles" -> {
                    in.beginArray();
                    while (in.hasNext()) rectangles.add(readRectangle(in));
                    in.endArray();
                }
                default -> in.skipValue();
            }
        }
        in.endObject();
        return new DisableConfig(name, textureId, active, disableAll, rectangles);
    }

    private UVRectangle readRectangle(JsonReader in) throws IOException {
        float uMin = 0, vMin = 0, uMax = 0, vMax = 0;
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "uMin" -> uMin = (float) in.nextDouble();
                case "vMin" -> vMin = (float) in.nextDouble();
                case "uMax" -> uMax = (float) in.nextDouble();
                case "vMax" -> vMax = (float) in.nextDouble();
                default -> in.skipValue();
            }
        }
        in.endObject();
        return new UVRectangle(uMin, vMin, uMax, vMax);
    }
}
