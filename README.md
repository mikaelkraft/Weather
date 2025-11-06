# Weather Information App (Java Swing)
# Note:
`Requesting extra features should come as issues with proper labels as REQUESTS`

Quickstart: export WEATHERAPI_KEY=your_key && mvn package && java -jar target/weather-app-1.0-myApp.jar

This is a small Java Swing application that fetches real-time weather information and a short-term forecast from WeatherAPI.com and displays it in a modern, user-friendly GUI.

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
This repository was updated to use WeatherAPI.com (weatherapi.com). The application supports multiple ways to provide the API key and stores it securely when saved from the GUI.

Where the app obtains the API key (checked in order):

- Environment variable `WEATHERAPI_KEY` (preferred)
- Legacy environment variable `OPENWEATHER_API_KEY` (supported for backwards compatibility)
- Saved encrypted config at `~/.weatherapp/config.json` (if you chose to save the key in the GUI)

To set the environment variable (Linux/macOS):

```bash
export WEATHERAPI_KEY=your_api_key_here
```

If you do not set an environment variable, the GUI will prompt for the API key on first run and offer to save it. When saved, the key is encrypted with AES-GCM and stored in `~/.weatherapp/config.json`. A per-user secret key is stored at `~/.weatherapp/secret.key` and is used to encrypt/decrypt the API key. The app attempts to set restrictive file permissions on `secret.key` on POSIX systems.

Security notes:

- The saved API key in `~/.weatherapp/config.json` is encrypted. The file contents will look like a base64 IV and ciphertext separated by a colon.
- The secret key file `~/.weatherapp/secret.key` is sensitive — keep it private. If you delete `secret.key`, the saved config cannot be decrypted.
- To remove the saved API key (the app will then prompt for a key or use the environment variable), remove both files:

```bash
rm -f ~/.weatherapp/config.json ~/.weatherapp/secret.key
```

Other files and locations

- Packaged runnable jar: `target/weather-app-1.0-myApp.jar` (created by `mvn package` and renamed to this filename as the primary runnable artifact)
	- Note: the build may also produce a non-fat `weather-app-1.0.jar` alongside the fat jar; you can safely delete the non-fat jar if you prefer to keep only the runnable fat jar in `target/`.
- Search history (created in the working directory where you run the app): `weather-search-history.json` (default location)
- Persistent icon cache (downloaded icons): `~/.weatherapp/icons/` (cached PNG files)

Running the app

Build:

```bash
mvn package
```

Run the GUI (desktop environment required):

```bash
export WEATHERAPI_KEY=your_api_key_here   # optional if you saved the key
java -jar target/weather-app-1.0-myApp.jar
```

Run the ConsoleRunner (headless-friendly) for quick API checks:

```bash
WEATHERAPI_KEY=your_api_key_here java -cp target/weather-app-1.0-myApp.jar com.weatherapp.ConsoleRunner London metric
```

UI and appearance

- The app uses FlatLaf for a modern look-and-feel and will select a light or dark theme depending on local time at startup (day → light, night → dark).
- Backgrounds dynamically change based on the weather timestamp returned by the API (day / sunset / night gradients).
- Icons are loaded asynchronously and cached on disk under `~/.weatherapp/icons/` to speed subsequent loads.

Testing

- Unit tests use JUnit 5. Run them with:

```bash
mvn test
```

Notes & next steps

- The app saves the API key encrypted by default; if you prefer OS-native secure storage (Keychain, Credential Manager, Secret Service) I can add that if you request via issues.
- Icon cache is persisted as PNG files; if you want eviction or size limits I can add a simple LRU cleanup.
- The GUI prompt saves the key in plain text only transiently (entered by the user) and stores it encrypted.

If you'd like a README section formatted differently, or want me to add a small CLI to print/decrypt the saved key (requires confirmation), request and I'll add it.

- A WeatherAPI.com API key (free tier works)

Setup
1. Obtain an API key from OpenWeatherMap:(Legacy) https://openweathermap.org/api
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
- The app uses the WeatherApi/OpenWeatherMap current weather and 5-day/3-hour forecast endpoints.
- JSON parsing is done with Gson.
- The app saves recent searches to `weather-search-history.json` in the working directory.
- Icons are loaded directly from WeatherApi/OpenWeatherMap icon URLs (no local binaries are committed).

Extending / Next steps
- I will add Fuzzy matching or auto-complete for city names.
- Improve UI with JavaFX (if desired).
- Add unit tests around JSON parsing and HistoryManager.

Troubleshooting
- If you see an error on startup that `OPENWEATHER_API_KEY` is not set, make sure you exported the environment variable in the shell used to run the app.
- If the API returns errors, check your API key usage and quota on OpenWeatherMap.

License
This code is provided as-is for demo/learning purposes.
# Weather
Weather app built with Java
