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
    private static final Supplier<Path> configDirectory = Suppliers.memoize(ConfigFile::getConfigDirectory);
    private static final Supplier<BindTargetStore> bindTargetStore = Suppliers.memoize(
            () -> new BindTargetStore(GSON, configDirectory.get().resolve(RealCamera.MOD_ID)));
    @Nullable
    private static ModConfig config;

    public static ModConfig config() {
        if (config == null) load();
        return config;
    }

    public static BindTargetStore bindTargets() {
        return bindTargetStore.get();
    }

    public static void load() {
        JsonObject json = null;
        ModConfig loaded = null;
        try (BufferedReader reader = Files.newBufferedReader(path())) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
            loaded = GSON.fromJson(json, ModConfig.class);
            loaded.clamp();
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to load " + FILE_NAME, exception);
        }
        // A broken main file must not discard the targets, which are stored on their own.
        config = loaded == null ? new ModConfig() : loaded;
        bindTargets().load();
        ConfigFileMigrator.migrateLegacyTargets(json, GSON, bindTargets());
        bindTargets().ensureDefaults();
        if (loaded == null) save();
    }

    public static void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(path())) {
            GSON.toJson(config, writer);
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to save " + FILE_NAME, exception);
            reset();
        }
        bindTargets().save();
    }

    public static void reset() {
        try (BufferedWriter writer = Files.newBufferedWriter(path())) {
            config = new ModConfig();
            GSON.toJson(config, writer);
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to reset " + FILE_NAME, exception);
        }
    }

    private static Path path() {
        return configDirectory.get().resolve(FILE_NAME);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static Path getConfigDirectory() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!configDir.exists()) configDir.mkdirs();
        return configDir.toPath();
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
