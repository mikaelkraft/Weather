package com.weatherapp;

import com.weatherapp.models.ForecastEntry;
import com.weatherapp.models.WeatherData;
import javax.swing.event.SwingPropertyChangeSupport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main entry point for the Weather Information App.
 * <p>
 * This Swing application lets users search a city name and retrieve current weather
 * and a short-term forecast from OpenWeatherMap. The API key must be provided via
 * the environment variable OPENWEATHER_API_KEY.
 */
public class Main {

    public static void main(String[] args) {
        // Start Swing UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to start application: " + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void createAndShowGUI() {
        // Use a modern look and feel (Nimbus if available) and enlarge fonts for a cleaner UI.
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // ignore and use default
        }

        // improve default fonts
        Font base = UIManager.getFont("Label.font");
        if (base != null) {
            Font uiFont = base.deriveFont(base.getSize2D() + 2f);
            UIManager.put("Label.font", uiFont);
            UIManager.put("Button.font", uiFont);
            UIManager.put("ComboBox.font", uiFont);
            UIManager.put("TextField.font", uiFont);
            UIManager.put("List.font", uiFont);
        }
        JFrame frame = new JFrame("Weather Information App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel: search controls
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JLabel cityLabel = new JLabel("City:");
        JTextField cityField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        String[] unitsOptions = {"Metric (C, m/s)", "Imperial (F, mph)"};
        JComboBox<String> unitsCombo = new JComboBox<>(unitsOptions);

        top.add(cityLabel);
        top.add(cityField);
        top.add(unitsCombo);
        top.add(searchBtn);

        root.add(top, BorderLayout.NORTH);

        // Center: weather display + forecast
        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));

        // Left: current weather
        JPanel currentPanel = new JPanel(new BorderLayout(8, 8));
        currentPanel.setBorder(BorderFactory.createTitledBorder("Current Weather"));

        JPanel currentInfo = new JPanel(new GridLayout(6, 1, 4, 4));
        JLabel cityTitle = new JLabel("-", SwingConstants.LEFT);
        JLabel tempLabel = new JLabel("Temperature: -");
        JLabel condLabel = new JLabel("Condition: -");
        JLabel humidityLabel = new JLabel("Humidity: -");
        JLabel windLabel = new JLabel("Wind: -");
        JLabel updatedLabel = new JLabel("Updated: -");

        currentInfo.add(cityTitle);
        currentInfo.add(tempLabel);
        currentInfo.add(condLabel);
        currentInfo.add(humidityLabel);
        currentInfo.add(windLabel);
        currentInfo.add(updatedLabel);

        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        currentPanel.add(iconLabel, BorderLayout.WEST);
        currentPanel.add(currentInfo, BorderLayout.CENTER);

        center.add(currentPanel);

        // Right: forecast and history
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));

        // Forecast list
        JPanel forecastPanel = new JPanel(new BorderLayout());
        forecastPanel.setBorder(BorderFactory.createTitledBorder("Short-term Forecast"));
        DefaultListModel<String> forecastListModel = new DefaultListModel<>();
        JList<String> forecastList = new JList<>(forecastListModel);
        forecastPanel.add(new JScrollPane(forecastList), BorderLayout.CENTER);

        rightPanel.add(forecastPanel, BorderLayout.CENTER);

        // History
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Search History"));
        DefaultListModel<String> historyListModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyListModel);
        historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);

        rightPanel.add(historyPanel, BorderLayout.SOUTH);

        center.add(rightPanel);

        root.add(center, BorderLayout.CENTER);

        // Footer: simple status
        JLabel statusLabel = new JLabel("Ready");
        root.add(statusLabel, BorderLayout.SOUTH);

    // Instantiate backend services
    ConfigManager configManager = new ConfigManager();

    // if no env var and no saved config, prompt the user for an API key
    String envKey = System.getenv("WEATHERAPI_KEY");
    String savedKey = configManager.getApiKey();
    if ((envKey == null || envKey.isEmpty()) && (savedKey == null || savedKey.isEmpty())) {
        String entered = JOptionPane.showInputDialog(frame,
            "Enter your WeatherAPI.com API key:\n(You can obtain one at https://weatherapi.com)",
            "API Key Required", JOptionPane.PLAIN_MESSAGE);
        if (entered == null || entered.trim().isEmpty()) {
        JOptionPane.showMessageDialog(frame, "API key is required to use this application.", "Missing API Key",
            JOptionPane.ERROR_MESSAGE);
        System.exit(1);
        }
        entered = entered.trim();
        int save = JOptionPane.showConfirmDialog(frame, "Save API key to ~/.weatherapp/config.json?",
            "Save API Key", JOptionPane.YES_NO_OPTION);
        if (save == JOptionPane.YES_OPTION) {
        configManager.saveApiKey(entered);
        }
    }

    WeatherService weatherService = new WeatherService();
    HistoryManager historyManager = new HistoryManager();

    // icon loader/cache
    IconCache iconCache = new IconCache();

        // Load history into UI
        historyManager.getHistory().forEach(h -> historyListModel.addElement(h.toString()));

        // Action listener for search
        ActionListener doSearch = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String city = cityField.getText().trim();
                if (city.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a city name.", "Input Error",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Decide units
                String units = unitsCombo.getSelectedIndex() == 0 ? "metric" : "imperial";

                statusLabel.setText("Fetching weather for " + city + "...");

                // Run network call off the EDT
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    WeatherData weatherData = null;
                    List<ForecastEntry> forecast = null;

                    @Override
                    protected Void doInBackground() {
                        try {
                            weatherData = weatherService.getCurrentWeather(city, units);
                            forecast = weatherService.getForecast(city, units);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(frame, "Error fetching weather: " + ex.getMessage(),
                                    "API Error", JOptionPane.ERROR_MESSAGE);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        if (weatherData != null) {
                            cityTitle.setText(String.format("%s, %s", weatherData.getCityName(), weatherData.getCountry()));
                            tempLabel.setText(String.format("Temperature: %.1f %s", weatherData.getTemperature(),
                                    units.equals("metric") ? "°C" : "°F"));
                            condLabel.setText("Condition: " + weatherData.getDescription());
                            humidityLabel.setText("Humidity: " + weatherData.getHumidity() + "%");
                            windLabel.setText(String.format("Wind: %.1f %s", weatherData.getWindSpeed(),
                                    units.equals("metric") ? "m/s" : "mph"));

                            // updated time
                            Instant instant = Instant.ofEpochSecond(weatherData.getTimestamp());
                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                    .withZone(ZoneId.systemDefault());
                            updatedLabel.setText("Updated: " + fmt.format(instant));

                            // load icon asynchronously using IconCache
                            if (weatherData.getIcon() != null && !weatherData.getIcon().isEmpty()) {
                                String iconUrl = weatherData.getIcon();
                                iconCache.loadIcon(iconUrl, icon -> {
                                    iconLabel.setIcon(icon);
                                });
                            } else {
                                iconLabel.setIcon(null);
                            }

                            // dynamic background based on local hour
                            try {
                                int hour = Instant.ofEpochSecond(weatherData.getTimestamp())
                                        .atZone(ZoneId.systemDefault()).getHour();
                                Color bg = backgroundForHour(hour);
                                root.setBackground(bg);
                                currentPanel.setBackground(bg);
                                currentInfo.setBackground(bg);
                                forecastPanel.setBackground(bg);
                                historyPanel.setBackground(bg);
                            } catch (Exception ex) {
                                // ignore
                            }

                            // save history
                            historyManager.addEntry(city, weatherData.getTimestamp());
                            historyListModel.removeAllElements();
                            historyManager.getHistory().forEach(h -> historyListModel.addElement(h.toString()));
                        }

                        // update forecast list
                        forecastListModel.clear();
                        if (forecast != null) {
                            // show next 5 forecast entries
                            int limit = Math.min(5, forecast.size());
                            DateTimeFormatter ffmt = DateTimeFormatter.ofPattern("MMM dd HH:mm")
                                    .withZone(ZoneId.systemDefault());
                            for (int i = 0; i < limit; i++) {
                                ForecastEntry fe = forecast.get(i);
                                String item = String.format("%s — %.1f %s — %s",
                                        ffmt.format(Instant.ofEpochSecond(fe.getTimestamp())),
                                        fe.getTemperature(), units.equals("metric") ? "°C" : "°F",
                                        fe.getDescription());
                                forecastListModel.addElement(item);
                            }
                        }

                        statusLabel.setText("Ready");
                    }
                };
                worker.execute();
            }
        };

        searchBtn.addActionListener(doSearch);
        cityField.addActionListener(doSearch);

        frame.setContentPane(root);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Choose a pleasant background color based on hour of day.
     */
    private static Color backgroundForHour(int hour) {
        // simple mapping: 6-18 => day, 18-20 => sunset, 20-6 => night
        if (hour >= 6 && hour < 18) {
            return new Color(0xE8F6FF); // light sky
        } else if (hour >= 18 && hour < 20) {
            return new Color(0xFFD8A8); // sunset
        } else {
            return new Color(0x1F2A44); // night
        }
    }
}
