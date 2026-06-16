package com.xtracr.realcamera.config;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class ConfigFile {
    private static final String FILE_NAME = RealCamera.MODID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final static Supplier<Path> path = Suppliers.memoize(ConfigFile::getPath);
    @Nullable
    private static ModConfig config;

    public static ModConfig config() {
        if (config == null) load();
        return config;
    }

    public static void load() {
        try (BufferedReader reader = Files.newBufferedReader(path.get())) {
            config = GSON.fromJson(reader, ModConfig.class);
            config.clamp();
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to load " + FILE_NAME, exception);
            config = new ModConfig();
            save();
        }
    }

    public static void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(path.get())) {
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
}
