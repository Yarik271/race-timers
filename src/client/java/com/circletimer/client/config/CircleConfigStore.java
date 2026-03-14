package com.circletimer.client.config;

import com.circletimer.client.CircleTimerClient;
import com.circletimer.client.state.ConfigData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CircleConfigStore {
    private static final String DATA_FILE = "circle_timer_data.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataPath;

    public CircleConfigStore() {
        this.dataPath = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(CircleTimerClient.MOD_ID)
            .resolve(DATA_FILE);
        try {
            Files.createDirectories(dataPath.getParent());
        } catch (IOException e) {
            CircleTimerClient.LOGGER.error("Failed to create config directory", e);
        }
    }

    public synchronized ConfigData load() {
        if (!Files.exists(dataPath)) {
            return new ConfigData();
        }
        try {
            String json = Files.readString(dataPath);
            ConfigData loaded = gson.fromJson(json, ConfigData.class);
            return loaded != null ? loaded : new ConfigData();
        } catch (IOException e) {
            CircleTimerClient.LOGGER.error("Failed to load config data", e);
            return new ConfigData();
        }
    }

    public synchronized void save(ConfigData data) {
        try {
            Files.writeString(dataPath, gson.toJson(data));
        } catch (IOException e) {
            CircleTimerClient.LOGGER.error("Failed to save config data", e);
        }
    }
}
