package com.tot.craftedsurvivors.platform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configFile;
    public boolean enableRevival = true;  // Example default
    public String item = "minecraft:nether_star";
    public int amount = 1;
    public boolean lastLifeKillGainEnabled = true;
    public static void init(Path path) {
        configFile = path;
        load();
    }
    public static void load() {
        try (FileReader reader = new FileReader(configFile.toFile())) {
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded != null) {
                instance.enableRevival = loaded.enableRevival;
                instance.item = loaded.item;
                instance.amount = loaded.amount;
                instance.lastLifeKillGainEnabled = loaded.lastLifeKillGainEnabled;
            }
        } catch (IOException e) {
            save();
        }
    }
    public static void save() {
        try (FileWriter writer = new FileWriter(configFile.toFile())) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
    public static Config instance = new Config();
}
