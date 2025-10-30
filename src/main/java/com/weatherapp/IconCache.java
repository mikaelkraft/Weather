package com.weatherapp;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Simple async icon loader with in-memory cache. Downloads icons off the EDT and invokes
 * a callback on the EDT with the loaded ImageIcon (or null on failure).
 */
public class IconCache {
    private final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();
    private final Path iconDir = Path.of(System.getProperty("user.home"), ".weatherapp", "icons");

    public IconCache() {
        try {
            Files.createDirectories(iconDir);
        } catch (Exception ex) {
            // ignore
        }
    }

    public void loadIcon(String url, Consumer<ImageIcon> cb) {
        if (url == null || url.isEmpty()) {
            SwingUtilities.invokeLater(() -> cb.accept(null));
            return;
        }
        if (cache.containsKey(url)) {
            SwingUtilities.invokeLater(() -> cb.accept(cache.get(url)));
            return;
        }

        // check disk cache
        try {
            String key = sha256(url);
            Path p = iconDir.resolve(key + ".png");
            if (Files.exists(p)) {
                BufferedImage img = ImageIO.read(p.toFile());
                if (img != null) {
                    ImageIcon icon = new ImageIcon(img);
                    cache.put(url, icon);
                    SwingUtilities.invokeLater(() -> cb.accept(icon));
                    return;
                }
            }
        } catch (Exception ex) {
            // ignore disk cache errors
        }

        // download off the EDT
        new Thread(() -> {
            try (InputStream in = new URL(url).openStream()) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    ImageIcon icon = new ImageIcon(img);
                    cache.put(url, icon);
                    // persist to disk
                    try {
                        String key = sha256(url);
                        Path p = iconDir.resolve(key + ".png");
                        ImageIO.write(img, "png", p.toFile());
                    } catch (Exception ex) {
                        // ignore
                    }
                    SwingUtilities.invokeLater(() -> cb.accept(icon));
                    return;
                }
            } catch (Exception ex) {
                // ignore
            }
            SwingUtilities.invokeLater(() -> cb.accept(null));
        }, "IconCache-Loader").start();
    }

    private static String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
    }
}
