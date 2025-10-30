# Weather Information App (Java Swing)

This is a small Java Swing application that fetches real-time weather information and a short-term forecast from OpenWeatherMap and displays it in a user-friendly GUI.

Features
- Search by city name
- Shows temperature, humidity, wind speed, weather condition and icon
- Short-term forecast (next few 3-hour entries)
- Unit switching (Metric / Imperial)
- Search history with timestamps (saved to `weather-search-history.json`)
- Dynamic background color based on time of day
- Error handling for invalid inputs and API failures

Requirements
- Java 17+
- Maven
- An OpenWeatherMap API key (free tier works)

This repository was updated to use WeatherAPI.com (weatherapi.com). The app will look for an API key in the following places (in order):

- The environment variable `WEATHERAPI_KEY`
- The legacy environment variable `OPENWEATHER_API_KEY` (if present)
- A simple config file at `~/.weatherapp/config.json` (you can save the key from the GUI on first run)

Set the environment variable like this (Linux/macOS):

```bash
export WEATHERAPI_KEY=your_api_key_here
```

Or when you run the GUI, you'll be prompted for a key and can choose to save it to `~/.weatherapp/config.json`.

- An WeatherAPI.com API key (free tier works)

Setup
1. Obtain an API key from OpenWeatherMap: https://openweathermap.org/api
2. Set the environment variable `OPENWEATHER_API_KEY` to your API key. Example (Linux/macOS):

```bash
export OPENWEATHER_API_KEY=your_api_key_here

If you switched to WeatherAPI.com, set the `WEATHERAPI_KEY` environment variable instead (or `OPENWEATHER_API_KEY` will still be accepted as a legacy name):

```bash
export WEATHERAPI_KEY=your_weatherapi_key_here
```
```

Build

```bash
mvn -q package
```

This will produce a fat jar under `target/` named similar to `weather-app-1.0-jar-with-dependencies.jar`.

Run

```bash
java -jar target/weather-app-1.0-jar-with-dependencies.jar
```

Usage
- Enter a city name (e.g., "London") and press Enter or click Search.
- Switch units between Metric (C, m/s) and Imperial (F, mph).
- The left panel displays current weather and an icon fetched from OpenWeatherMap.
- The right panel shows a short forecast and your search history with timestamps.

Implementation notes
- The app uses the OpenWeatherMap current weather and 5-day/3-hour forecast endpoints.
- JSON parsing is done with Gson.
- The app saves recent searches to `weather-search-history.json` in the working directory.
- Icons are loaded directly from OpenWeatherMap icon URLs (no local binaries are committed).

Extending / Next steps
- Add Fuzzy matching or auto-complete for city names.
- Improve UI with JavaFX (if desired).
- Add unit tests around JSON parsing and HistoryManager.

Troubleshooting
- If you see an error on startup that `OPENWEATHER_API_KEY` is not set, make sure you exported the environment variable in the shell used to run the app.
- If the API returns errors, check your API key usage and quota on OpenWeatherMap.

License
This code is provided as-is for demo/learning purposes.
# Weather
Weather app built with Java
