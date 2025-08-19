package com.tot.craftedsurvivors.platform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerDataManager {
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path dataFile;
    public static void init(Path configDir) {
        try {
            Files.createDirectories(configDir);
            dataFile = configDir.resolve("playerdata.json");
            loadData();
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
    }
    public static void saveData() {
        Path tempFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            GSON.toJson(Main.playerData, writer);
        } catch (IOException e) {
            System.err.println("Failed to save player data: " + e.getMessage());
            return;
        }
        try {
            Files.move(tempFile, dataFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to replace old save file: " + e.getMessage());
        }
    }
    public static void loadData() {
        if (!Files.exists(dataFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(dataFile)) {
            var type = new TypeToken<Map<UUID, Main.PlayerLifeData>>(){}.getType();
            Map<UUID, Main.PlayerLifeData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                Main.playerData.clear();
                Main.playerData.putAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Failed to load player data: " + e.getMessage());
        }
    }
}