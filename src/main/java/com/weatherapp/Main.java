package com.weatherapp;

import com.weatherapp.models.ForecastEntry;
import com.weatherapp.models.WeatherData;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

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
        // choose theme based on local hour (light by day, dark by night)
        int startupHour = Instant.now().atZone(ZoneId.systemDefault()).getHour();
        if (startupHour >= 6 && startupHour < 18) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }

        GradientPanel root = new GradientPanel(startupHour);
        root.setLayout(new BorderLayout(10, 10));
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
    currentPanel.setOpaque(false);
    currentPanel.setBorder(BorderFactory.createTitledBorder("Current Weather"));

    JPanel currentInfo = new JPanel(new GridLayout(6, 1, 4, 4));
    currentInfo.setOpaque(false);
    JLabel cityTitle = new JLabel("-", SwingConstants.LEFT);
    JLabel tempLabel = new JLabel("Temperature: -");
    JLabel condLabel = new JLabel("Condition: -");
    JLabel humidityLabel = new JLabel("Humidity: -");
    JLabel windLabel = new JLabel("Wind: -");
    JLabel updatedLabel = new JLabel("Updated: -");

    // make typography nicer
    cityTitle.setFont(cityTitle.getFont().deriveFont(Font.BOLD, 20f));
    tempLabel.setFont(tempLabel.getFont().deriveFont(Font.BOLD, 36f));
    condLabel.setFont(condLabel.getFont().deriveFont(Font.ITALIC, 16f));

        currentInfo.add(cityTitle);
        currentInfo.add(tempLabel);
        currentInfo.add(condLabel);
        currentInfo.add(humidityLabel);
        currentInfo.add(windLabel);
        currentInfo.add(updatedLabel);

    JLabel iconLabel = new JLabel();
    iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
    iconLabel.setPreferredSize(new Dimension(120, 120));

        currentPanel.add(iconLabel, BorderLayout.WEST);
        currentPanel.add(currentInfo, BorderLayout.CENTER);

        center.add(currentPanel);

        // Right: forecast and history
    JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
    rightPanel.setOpaque(false);

        // Forecast list
    JPanel forecastPanel = new JPanel(new BorderLayout());
    forecastPanel.setOpaque(false);
    forecastPanel.setBorder(BorderFactory.createTitledBorder("Short-term Forecast"));
    DefaultListModel<ForecastEntry> forecastListModel = new DefaultListModel<>();
    // icon cache needs to be available for the renderer
    IconCache iconCacheLocal = new IconCache();
    JList<ForecastEntry> forecastList = new JList<>(forecastListModel);
    forecastList.setCellRenderer(new ForecastCellRenderer(iconCacheLocal));
    forecastPanel.add(new JScrollPane(forecastList), BorderLayout.CENTER);

        rightPanel.add(forecastPanel, BorderLayout.CENTER);

        // History
    JPanel historyPanel = new JPanel(new BorderLayout());
    historyPanel.setOpaque(false);
    historyPanel.setBorder(BorderFactory.createTitledBorder("Search History"));
    DefaultListModel<HistoryManager.HistoryEntry> historyListModel = new DefaultListModel<>();
    JList<HistoryManager.HistoryEntry> historyList = new JList<>(historyListModel);
    historyList.setCellRenderer(new HistoryCellRenderer());
    historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);

        rightPanel.add(historyPanel, BorderLayout.SOUTH);

        center.add(rightPanel);

        root.add(center, BorderLayout.CENTER);

    // Footer: simple status
    JLabel statusLabel = new JLabel("Ready");
    statusLabel.setBorder(new EmptyBorder(6,6,6,6));
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

    // icon loader/cache (shared instance used for loading in workers)
    IconCache iconCache = new IconCache();

    // Load history into UI
    historyManager.getHistory().forEach(h -> historyListModel.addElement(h));

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
                tempLabel.setText(String.format("%.1f %s", weatherData.getTemperature(),
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
                                iconCache.loadIcon(iconUrl, icon -> iconLabel.setIcon(icon));
                            } else {
                                iconLabel.setIcon(null);
                            }

                            // dynamic background based on local hour
                            try {
                int hour = Instant.ofEpochSecond(weatherData.getTimestamp())
                    .atZone(ZoneId.systemDefault()).getHour();
                root.setHour(hour);
                root.repaint();
                            } catch (Exception ex) {
                                // ignore
                            }

                            // save history
                            historyManager.addEntry(city, weatherData.getTimestamp());
                            historyListModel.removeAllElements();
                            historyManager.getHistory().forEach(h -> historyListModel.addElement(h));
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
                                // preload icons for smoother rendering
                                if (fe.getIcon() != null && !fe.getIcon().isEmpty()) {
                                    iconCache.loadIcon(fe.getIcon(), ic -> forecastList.repaint());
                                }
                                forecastListModel.addElement(fe);
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

    // Gradient panel paints a subtle background based on hour (day/sunset/night)
    private static class GradientPanel extends JPanel {
        private int hour;

        public GradientPanel(int hour) {
            this.hour = hour;
            setOpaque(true);
        }

        public void setHour(int hour) {
            this.hour = hour;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Color c1 = backgroundForHour(hour);
            Color c2 = c1.darker();
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, c1, 0, h, c2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    // Renderer for forecast entries (icon + time + temp + description)
    private static class ForecastCellRenderer extends JPanel implements ListCellRenderer<ForecastEntry> {
        private final JLabel icon = new JLabel();
        private final JLabel title = new JLabel();
        private final IconCache iconCache;

        public ForecastCellRenderer(IconCache iconCache) {
            this.iconCache = Objects.requireNonNull(iconCache);
            setLayout(new BorderLayout(8, 8));
            setOpaque(false);
            icon.setPreferredSize(new Dimension(48, 48));
            title.setFont(title.getFont().deriveFont(14f));
            add(icon, BorderLayout.WEST);
            add(title, BorderLayout.CENTER);
            setBorder(new EmptyBorder(6,6,6,6));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ForecastEntry> list, ForecastEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            DateTimeFormatter ffmt = DateTimeFormatter.ofPattern("MMM dd HH:mm").withZone(ZoneId.systemDefault());
            String when = ffmt.format(Instant.ofEpochSecond(value.getTimestamp()));
            String temp = String.format("%.1f", value.getTemperature());
            title.setText(String.format("%s — %s — %s", when, temp, value.getDescription()));
            // try to load icon (async)
            icon.setIcon(null);
            if (value.getIcon() != null && !value.getIcon().isEmpty()) {
                iconCache.loadIcon(value.getIcon(), ic -> {
                    icon.setIcon(ic);
                    list.repaint();
                });
            }
            if (isSelected) setBackground(new Color(0,0,0,30));
            return this;
        }
    }

    // Renderer for history entries
    private static class HistoryCellRenderer extends JPanel implements ListCellRenderer<HistoryManager.HistoryEntry> {
        private final JLabel label = new JLabel();

        public HistoryCellRenderer() {
            setLayout(new BorderLayout());
            setOpaque(false);
            label.setFont(label.getFont().deriveFont(14f));
            add(label, BorderLayout.CENTER);
            setBorder(new EmptyBorder(4,4,4,4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends HistoryManager.HistoryEntry> list, HistoryManager.HistoryEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            label.setText(value.toString());
            return this;
        }
    }
}
