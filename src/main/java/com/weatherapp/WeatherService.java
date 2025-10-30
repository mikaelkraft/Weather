package com.weatherapp;

import com.google.gson.*;
import com.weatherapp.models.ForecastEntry;
import com.weatherapp.models.WeatherData;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * WeatherService adapted to use WeatherAPI.com (weatherapi.com).
 *
 * Environment variable checked (in order): WEATHERAPI_KEY, OPENWEATHER_API_KEY (legacy).
 */
public class WeatherService {

    private final String apiKey;
    private final HttpClient client;
    private final Gson gson = new Gson();

    public WeatherService() {
        String k = System.getenv("WEATHERAPI_KEY");
        if (k == null || k.isEmpty()) {
            // fall back to older env var name if present
            k = System.getenv("OPENWEATHER_API_KEY");
        }
        // also check config file
        if (k == null || k.isEmpty()) {
            try {
                ConfigManager cm = new ConfigManager();
                String cfgKey = cm.getApiKey();
                if (cfgKey != null && !cfgKey.isEmpty()) k = cfgKey;
            } catch (Exception ex) {
                // ignore
            }
        }
        if (k == null || k.isEmpty()) {
            throw new IllegalStateException("WEATHERAPI_KEY (or OPENWEATHER_API_KEY) environment variable is not set.");
        }
        apiKey = k;
        client = HttpClient.newHttpClient();
    }

    /**
     * Fetch current weather for the given city. Units are handled in the UI; WeatherAPI returns both C and F.
     */
    public WeatherData getCurrentWeather(String city, String units) throws IOException, InterruptedException {
        String q = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = String.format("https://api.weatherapi.com/v1/current.json?key=%s&q=%s&aqi=no", apiKey, q);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("API returned status " + resp.statusCode() + ": " + resp.body());
        }

        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonObject loc = obj.getAsJsonObject("location");
        JsonObject cur = obj.getAsJsonObject("current");

        WeatherData wd = new WeatherData();
        wd.setCityName(loc.has("name") ? loc.get("name").getAsString() : city);
        wd.setCountry(loc.has("country") ? loc.get("country").getAsString() : "");

        // WeatherAPI returns both metric and imperial temps; pick based on units requested
        if (units != null && units.equalsIgnoreCase("imperial")) {
            wd.setTemperature(cur.get("temp_f").getAsDouble());
        } else {
            wd.setTemperature(cur.get("temp_c").getAsDouble());
        }

        wd.setHumidity(cur.get("humidity").getAsInt());
        // WeatherAPI gives wind_kph and wind_mph
        if (units != null && units.equalsIgnoreCase("imperial")) {
            wd.setWindSpeed(cur.get("wind_mph").getAsDouble());
        } else {
            // convert kph to m/s for metric; original app expects m/s for metric
            double kph = cur.get("wind_kph").getAsDouble();
            wd.setWindSpeed(kph / 3.6);
        }

        JsonObject cond = cur.getAsJsonObject("condition");
        if (cond != null) {
            wd.setDescription(cond.has("text") ? cond.get("text").getAsString() : "");
            String icon = cond.has("icon") ? cond.get("icon").getAsString() : "";
            // icons may start with //, make absolute URL
            if (icon.startsWith("//")) icon = "https:" + icon;
            wd.setIcon(icon);
        }

        // timestamp: use current.last_updated_epoch when available
        if (cur.has("last_updated_epoch")) {
            wd.setTimestamp(cur.get("last_updated_epoch").getAsLong());
        } else if (loc.has("localtime_epoch")) {
            wd.setTimestamp(loc.get("localtime_epoch").getAsLong());
        } else {
            wd.setTimestamp(Instant.now().getEpochSecond());
        }

        return wd;
    }

    /**
     * Fetch short-term hourly forecast for the given city. Uses the forecast endpoint and collects hourly entries.
     */
    public List<ForecastEntry> getForecast(String city, String units) throws IOException, InterruptedException {
        String q = URLEncoder.encode(city, StandardCharsets.UTF_8);
        // request 2 days to ensure we have several hourly entries
        String url = String.format("https://api.weatherapi.com/v1/forecast.json?key=%s&q=%s&days=2&aqi=no&alerts=no", apiKey, q);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("API returned status " + resp.statusCode() + ": " + resp.body());
        }

        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonObject forecast = obj.getAsJsonObject("forecast");
        List<ForecastEntry> result = new ArrayList<>();
        if (forecast != null && forecast.has("forecastday")) {
            JsonArray days = forecast.getAsJsonArray("forecastday");
            for (JsonElement dayElem : days) {
                JsonObject dayObj = dayElem.getAsJsonObject();
                JsonArray hours = dayObj.getAsJsonArray("hour");
                for (JsonElement h : hours) {
                    JsonObject he = h.getAsJsonObject();
                    ForecastEntry fe = new ForecastEntry();
                    fe.setTimestamp(he.get("time_epoch").getAsLong());
                    if (units != null && units.equalsIgnoreCase("imperial")) {
                        fe.setTemperature(he.get("temp_f").getAsDouble());
                    } else {
                        fe.setTemperature(he.get("temp_c").getAsDouble());
                    }
                    JsonObject cond = he.getAsJsonObject("condition");
                    if (cond != null) {
                        fe.setDescription(cond.has("text") ? cond.get("text").getAsString() : "");
                        String icon = cond.has("icon") ? cond.get("icon").getAsString() : "";
                        if (icon.startsWith("//")) icon = "https:" + icon;
                        fe.setIcon(icon);
                    }
                    result.add(fe);
                }
            }
        }

        return result;
    }
}
