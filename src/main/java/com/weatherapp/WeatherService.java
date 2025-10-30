package com.weatherapp;

import com.google.gson.*;
import com.weatherapp.models.ForecastEntry;
import com.weatherapp.models.WeatherData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * WeatherService handles HTTP calls to the OpenWeatherMap API.
 * <p>
 * Expects an API key in the environment variable OPENWEATHER_API_KEY.
 */
public class WeatherService {

    private final String apiKey;
    private final HttpClient client;
    private final Gson gson = new Gson();

    public WeatherService() {
        apiKey = System.getenv("OPENWEATHER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OPENWEATHER_API_KEY environment variable is not set.");
        }
        client = HttpClient.newHttpClient();
    }

    /**
     * Fetch current weather for the given city and units (metric/imperial).
     */
    public WeatherData getCurrentWeather(String city, String units) throws IOException, InterruptedException {
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=%s",
                URIEncoder.encode(city), apiKey, units);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("API returned status " + resp.statusCode() + ": " + resp.body());
        }

        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        WeatherData wd = new WeatherData();
        wd.setCityName(obj.get("name").getAsString());
        wd.setCountry(obj.getAsJsonObject("sys").get("country").getAsString());
        wd.setTimestamp(obj.get("dt").getAsLong());

        JsonObject main = obj.getAsJsonObject("main");
        wd.setTemperature(main.get("temp").getAsDouble());
        wd.setHumidity(main.get("humidity").getAsInt());

        JsonObject wind = obj.getAsJsonObject("wind");
        wd.setWindSpeed(wind.get("speed").getAsDouble());

        JsonArray weatherArr = obj.getAsJsonArray("weather");
        if (weatherArr.size() > 0) {
            JsonObject w0 = weatherArr.get(0).getAsJsonObject();
            wd.setDescription(w0.get("description").getAsString());
            wd.setIcon(w0.get("icon").getAsString());
        }

        return wd;
    }

    /**
     * Fetch short-term forecast (3-hour intervals) for the given city.
     * Returns a list of ForecastEntry sorted by timestamp (ascending).
     */
    public List<ForecastEntry> getForecast(String city, String units) throws IOException, InterruptedException {
        String url = String.format("https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=%s",
                URIEncoder.encode(city), apiKey, units);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("API returned status " + resp.statusCode() + ": " + resp.body());
        }

        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray list = obj.getAsJsonArray("list");
        List<ForecastEntry> result = new ArrayList<>();
        for (JsonElement e : list) {
            JsonObject it = e.getAsJsonObject();
            ForecastEntry fe = new ForecastEntry();
            fe.setTimestamp(it.get("dt").getAsLong());
            JsonObject main = it.getAsJsonObject("main");
            fe.setTemperature(main.get("temp").getAsDouble());
            JsonArray wa = it.getAsJsonArray("weather");
            if (wa.size() > 0) {
                JsonObject w0 = wa.get(0).getAsJsonObject();
                fe.setDescription(w0.get("description").getAsString());
                fe.setIcon(w0.get("icon").getAsString());
            }
            result.add(fe);
        }
        return result;
    }

    // small helper to URL-encode city names
    private static class URIEncoder {
        public static String encode(String s) {
            return s.replace(" ", "%20");
        }
    }
}
