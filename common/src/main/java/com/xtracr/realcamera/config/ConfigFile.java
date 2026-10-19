package com.xtracr.realcamera.config;

import com.google.common.base.Suppliers;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.codec.ConfigFileMigrator;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class ConfigFile {
    private static final String FILE_NAME = RealCamera.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Identifier.class, new IdentifierTypeAdapter().nullSafe())
            .create();
    private final static Supplier<Path> path = Suppliers.memoize(ConfigFile::getPath);
    @Nullable
    private static ModConfig config;

    public static ModConfig config() {
        if (config == null) load();
        return config;
    }

    public static void load() {
        try (BufferedReader reader = Files.newBufferedReader(path.get())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ConfigFileMigrator.migrateLegacyBinding(json);
            config = GSON.fromJson(json, ModConfig.class);
            config.clamp();
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to load " + FILE_NAME, exception);
            config = new ModConfig();
            save();
        }
    }

    public static void save() {
        if (config == null) load();
        try (BufferedWriter writer = Files.newBufferedWriter(path.get())) {
            config.clamp();
            GSON.toJson(config, writer);
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to save " + FILE_NAME, exception);
            reset();
        }
    }

    public static void reset() {
        try (BufferedWriter writer = Files.newBufferedWriter(path.get())) {
            config = new ModConfig();
            GSON.toJson(config, writer);
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to reset " + FILE_NAME, exception);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static Path getPath() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!configDir.exists()) configDir.mkdirs();
        return configDir.toPath().resolve(FILE_NAME);
    }

    private static class IdentifierTypeAdapter extends TypeAdapter<Identifier> {
        @Override
        public void write(JsonWriter out, Identifier value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public Identifier read(JsonReader in) throws IOException {
            return Identifier.parse(in.nextString());
        }
    }
}
