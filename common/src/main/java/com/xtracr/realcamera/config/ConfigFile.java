package com.xtracr.realcamera.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import com.xtracr.realcamera.RealCamera;

import net.minecraft.client.MinecraftClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigFile {

    private static final String FILE_NAME = RealCamera.MODID + ".json";
    private static final Path PATH;
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public static final ModConfig modConfig = new ModConfig();

    static {
        @SuppressWarnings("resource")
        final File configDir = new File(MinecraftClient.getInstance().runDirectory, "config");
        if (!configDir.exists()) configDir.mkdirs();
        PATH = configDir.toPath().resolve(FILE_NAME);
    }

    public static void load() {
        try (BufferedReader reader = Files.newBufferedReader(PATH)) {
            modConfig.set(GSON.fromJson(reader, ModConfig.class));
            modConfig.clamp();
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to load " + FILE_NAME);
            save();
        }
    }

    public static void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(modConfig, writer);
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to save " + FILE_NAME, exception);
            reset();
        }
    }

    public static void reset() {
        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            modConfig.set(new ModConfig());
            GSON.toJson(modConfig, writer);
        } catch (Exception exception) {
            RealCamera.LOGGER.warn("Failed to reset " + FILE_NAME, exception);
        }
    }

}
