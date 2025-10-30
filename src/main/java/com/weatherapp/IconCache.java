package com.weatherapp;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Simple async icon loader with in-memory cache. Downloads icons off the EDT and invokes
 * a callback on the EDT with the loaded ImageIcon (or null on failure).
 */
public class IconCache {
    private final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();

    public void loadIcon(String url, Consumer<ImageIcon> cb) {
        if (url == null || url.isEmpty()) {
            SwingUtilities.invokeLater(() -> cb.accept(null));
            return;
        }
        if (cache.containsKey(url)) {
            SwingUtilities.invokeLater(() -> cb.accept(cache.get(url)));
            return;
        }

        // download off the EDT
        new Thread(() -> {
            try (InputStream in = new URL(url).openStream()) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    ImageIcon icon = new ImageIcon(img);
                    cache.put(url, icon);
                    SwingUtilities.invokeLater(() -> cb.accept(icon));
                    return;
                }
            } catch (Exception ex) {
                // ignore
            }
            SwingUtilities.invokeLater(() -> cb.accept(null));
        }, "IconCache-Loader").start();
    }
}
