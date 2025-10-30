package com.weatherapp;

import com.weatherapp.models.ForecastEntry;
import com.weatherapp.models.WeatherData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WeatherApiParserTest {
    @Test
    public void parseCurrent_minimal() {
        String json = "{\n" +
                "  \"location\": { \"name\": \"TestCity\", \"country\": \"TC\" },\n" +
                "  \"current\": { \"temp_c\": 10.5, \"temp_f\": 50.9, \"humidity\": 80, \"wind_kph\": 10.8, \"wind_mph\": 6.7, \"last_updated_epoch\": 1600000000, \"condition\": { \"text\": \"Sunny\", \"icon\": \"//cdn.weatherapi.com/icons/sunny.png\" } }\n" +
                "}";

        WeatherData wd = WeatherApiParser.parseCurrent(json, "metric");
        assertEquals("TestCity", wd.getCityName());
        assertEquals("TC", wd.getCountry());
        assertEquals(10.5, wd.getTemperature(), 0.001);
        assertEquals(80, wd.getHumidity());
        assertTrue(wd.getWindSpeed() > 0);
        assertEquals("Sunny", wd.getDescription());
        assertTrue(wd.getIcon().startsWith("https:"));
    }

    @Test
    public void parseForecast_minimal() {
        String json = "{\n" +
                "  \"forecast\": { \"forecastday\": [ { \"hour\": [ { \"time_epoch\": 1600003600, \"temp_c\": 11.0, \"temp_f\": 51.8, \"condition\": { \"text\": \"Cloudy\", \"icon\": \"//cdn.weatherapi.com/icons/cloudy.png\" } } ] } ] }\n" +
                "}";

        List<ForecastEntry> list = WeatherApiParser.parseForecast(json, "metric");
        assertEquals(1, list.size());
        ForecastEntry fe = list.get(0);
        assertEquals(1600003600L, fe.getTimestamp());
        assertEquals(11.0, fe.getTemperature(), 0.001);
        assertEquals("Cloudy", fe.getDescription());
        assertTrue(fe.getIcon().startsWith("https:"));
    }
}
