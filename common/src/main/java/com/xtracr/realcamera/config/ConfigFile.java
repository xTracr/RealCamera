package com.xtracr.realcamera.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.xtracr.realcamera.RealCamera;

import net.minecraft.client.MinecraftClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

@SuppressWarnings("resource")
public class ConfigFile {
    
    private static final Path PATH;
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public static final ModConfig modConfig = new ModConfig();
    
    static {
        final File configDir = new File(MinecraftClient.getInstance().runDirectory, "config"); 
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        PATH = configDir.toPath().resolve(RealCamera.MODID+".json");
    }

    public static void load() {
        try (BufferedReader reader = Files.newBufferedReader(PATH)){
            modConfig.set(GSON.fromJson(reader, ModConfig.class));
            modConfig.clamp();
            reader.close();
        } catch (IOException | JsonSyntaxException | JsonIOException exception) {
            save();
        }
    }

    public static void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(modConfig, writer);
            writer.close();
        } catch (IOException | JsonIOException exception) {
            RealCamera.LOGGER.warn("Failed to save config", exception);
            reset();
        }
    }

    public static void reset() {
        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            modConfig.set(new ModConfig());
            GSON.toJson(modConfig, writer);
            writer.close();
        } catch (IOException | JsonIOException exception) {
            RealCamera.LOGGER.warn("Failed to reset config", exception);
        }
    }
    
}
