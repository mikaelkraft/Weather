package com.weatherapp;

import com.google.gson.*;
import com.weatherapp.models.ForecastEntry;
import com.weatherapp.models.WeatherData;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper parser that parses WeatherAPI.com JSON payloads into model objects.
 * Useful for unit tests to validate parsing without making network calls.
 */
public class WeatherApiParser {

    public static WeatherData parseCurrent(String json, String units) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        JsonObject loc = obj.getAsJsonObject("location");
        JsonObject cur = obj.getAsJsonObject("current");
        WeatherData wd = new WeatherData();
        wd.setCityName(loc.has("name") ? loc.get("name").getAsString() : "");
        wd.setCountry(loc.has("country") ? loc.get("country").getAsString() : "");
        if (units != null && units.equalsIgnoreCase("imperial")) {
            wd.setTemperature(cur.get("temp_f").getAsDouble());
        } else {
            wd.setTemperature(cur.get("temp_c").getAsDouble());
        }
        wd.setHumidity(cur.get("humidity").getAsInt());
        if (units != null && units.equalsIgnoreCase("imperial")) {
            wd.setWindSpeed(cur.get("wind_mph").getAsDouble());
        } else {
            wd.setWindSpeed(cur.get("wind_kph").getAsDouble() / 3.6);
        }
        JsonObject cond = cur.getAsJsonObject("condition");
        if (cond != null) {
            wd.setDescription(cond.has("text") ? cond.get("text").getAsString() : "");
            String icon = cond.has("icon") ? cond.get("icon").getAsString() : "";
            if (icon.startsWith("//")) icon = "https:" + icon;
            wd.setIcon(icon);
        }
        if (cur.has("last_updated_epoch")) {
            wd.setTimestamp(cur.get("last_updated_epoch").getAsLong());
        }
        return wd;
    }

    public static List<ForecastEntry> parseForecast(String json, String units) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
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
