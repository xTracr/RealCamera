package com.xtracr.realcamera.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.xtracr.realcamera.RealCamera;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigFile{
    
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("realcamera.json");
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public static final ModConfig modConfig = new ModConfig();
    
    public static void setup() {
        load();
    }
    
    public static void load() {
        try (BufferedReader reader = Files.newBufferedReader(PATH)){
            modConfig.set(gson.fromJson(reader, ModConfig.class));
            modConfig.clamp();
            reader.close();
        } catch (Exception exception) {
            save();
        }
    }

    public static void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            gson.toJson(modConfig, writer);
            writer.close();
        } catch (IOException exception) {
            RealCamera.LOGGER.warn("Failed to save config", exception);
        }
    }

    public static void reset() {
        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            modConfig.set(new ModConfig());
            gson.toJson(modConfig, writer);
            writer.close();
        } catch (IOException exception) {
            RealCamera.LOGGER.warn("Failed to reset config", exception);
        }
    }
    
}
