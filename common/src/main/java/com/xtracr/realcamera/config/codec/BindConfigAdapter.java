package com.xtracr.realcamera.config.codec;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.xtracr.realcamera.config.BindTarget.BindConfig;

import java.io.IOException;

public final class BindConfigAdapter extends TypeAdapter<BindConfig> {
    @Override
    public void write(JsonWriter out, BindConfig value) throws IOException {
        out.beginObject();
        out.name("bindX").value(value.bindX());
        out.name("bindY").value(value.bindY());
        out.name("bindZ").value(value.bindZ());
        out.name("bindRotation").value(value.bindRotation());
        out.name("bindPitch").value(value.bindPitch());
        out.name("bindYaw").value(value.bindYaw());
        out.name("bindRoll").value(value.bindRoll());
        out.endObject();
    }

    @Override
    public BindConfig read(JsonReader in) throws IOException {
        boolean bindX = false, bindY = false, bindZ = false, legacyRotation = false;
        Boolean bindPitch = null, bindYaw = null, bindRoll = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "bindX" -> bindX = in.nextBoolean();
                case "bindY" -> bindY = in.nextBoolean();
                case "bindZ" -> bindZ = in.nextBoolean();
                case "bindRotation" -> legacyRotation = in.nextBoolean();
                case "bindPitch" -> bindPitch = in.nextBoolean();
                case "bindYaw" -> bindYaw = in.nextBoolean();
                case "bindRoll" -> bindRoll = in.nextBoolean();
                default -> in.skipValue();
            }
        }
        in.endObject();

        return new BindConfig(
                bindX,
                bindY,
                bindZ,
                bindPitch == null ? legacyRotation : bindPitch,
                bindYaw == null ? legacyRotation : bindYaw,
                bindRoll == null ? legacyRotation : bindRoll
        );
    }
}
