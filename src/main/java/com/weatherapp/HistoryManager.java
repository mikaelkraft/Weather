package com.weatherapp;

import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple history manager that records recent searches to a JSON file in the project directory.
 */
public class HistoryManager {
    private static final String HISTORY_FILE = "weather-search-history.json";
    private final List<HistoryEntry> history = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public HistoryManager() {
        load();
    }

    public void addEntry(String city, long timestamp) {
        history.add(0, new HistoryEntry(city, timestamp)); // add to front
        // keep only last 50
        if (history.size() > 50) history.remove(history.size() - 1);
        save();
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    private void load() {
        try {
            File f = new File(HISTORY_FILE);
            if (!f.exists()) return;
            try (FileReader fr = new FileReader(f)) {
                JsonArray arr = JsonParser.parseReader(fr).getAsJsonArray();
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    String city = o.get("city").getAsString();
                    long ts = o.get("timestamp").getAsLong();
                    history.add(new HistoryEntry(city, ts));
                }
            }
        } catch (Exception ex) {
            // ignore load errors
        }
    }

    private void save() {
        try {
            File f = new File(HISTORY_FILE);
            try (FileWriter fw = new FileWriter(f)) {
                JsonArray arr = new JsonArray();
                for (HistoryEntry he : history) {
                    JsonObject o = new JsonObject();
                    o.addProperty("city", he.getCity());
                    o.addProperty("timestamp", he.getTimestamp());
                    arr.add(o);
                }
                gson.toJson(arr, fw);
            }
        } catch (Exception ex) {
            // ignore save errors
        }
    }

    public static class HistoryEntry {
        private final String city;
        private final long timestamp;

        public HistoryEntry(String city, long timestamp) {
            this.city = city;
            this.timestamp = timestamp;
        }

        public String getCity() {
            return city;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());
            return city + " — " + fmt.format(Instant.ofEpochSecond(timestamp));
        }
    }
}
