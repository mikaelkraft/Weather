package com.weatherapp;

import com.weatherapp.models.ForecastEntry;
import com.weatherapp.models.WeatherData;

import java.util.List;

/**
 * Simple console runner to verify WeatherService and API connectivity without launching the GUI.
 *
 * Usage: java -cp target/weather-app-1.0-jar-with-dependencies.jar com.weatherapp.ConsoleRunner [city] [metric|imperial]
 */
public class ConsoleRunner {
    public static void main(String[] args) {
        String city = args.length > 0 ? args[0] : "London";
        String units = args.length > 1 ? args[1] : "metric";

        System.out.println("ConsoleRunner: fetching weather for '" + city + "' (units=" + units + ")");

        try {
            WeatherService ws = new WeatherService();
            WeatherData wd = ws.getCurrentWeather(city, units);
            List<ForecastEntry> fc = ws.getForecast(city, units);

            System.out.println("Current:");
            System.out.printf("  Location: %s, %s\n", wd.getCityName(), wd.getCountry());
            System.out.printf("  Temp: %.1f %s\n", wd.getTemperature(), units.equals("metric") ? "°C" : "°F");
            System.out.printf("  Condition: %s\n", wd.getDescription());
            System.out.printf("  Humidity: %d%%\n", wd.getHumidity());
            System.out.printf("  Wind: %.1f %s\n", wd.getWindSpeed(), units.equals("metric") ? "m/s" : "mph");
            System.out.println();

            System.out.println("Forecast (next items):");
            int limit = Math.min(5, fc.size());
            for (int i = 0; i < limit; i++) {
                ForecastEntry e = fc.get(i);
                System.out.printf("  - %d: %.1f %s — %s\n", e.getTimestamp(), e.getTemperature(),
                        units.equals("metric") ? "°C" : "°F", e.getDescription());
            }
        } catch (Exception ex) {
            System.err.println("Error fetching weather: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
