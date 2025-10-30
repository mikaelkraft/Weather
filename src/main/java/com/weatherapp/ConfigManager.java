package com.weatherapp;

import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple config manager that persists a small JSON config under ~/.weatherapp/config.json
 */
public class ConfigManager {
    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".weatherapp");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ConfigManager() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
        } catch (Exception ex) {
            // ignore creation errors; will try to save later
        }
    }

    public String getApiKey() {
        try {
            File f = CONFIG_FILE.toFile();
            if (!f.exists()) return null;
            try (FileReader fr = new FileReader(f)) {
                JsonObject o = JsonParser.parseReader(fr).getAsJsonObject();
                if (o.has("apiKey")) return o.get("apiKey").getAsString();
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    public boolean saveApiKey(String apiKey) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("apiKey", apiKey);
            try (FileWriter fw = new FileWriter(CONFIG_FILE.toFile())) {
                gson.toJson(o, fw);
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
