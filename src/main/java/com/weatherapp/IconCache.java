package com.weatherapp;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Simple async icon loader with in-memory cache. Downloads icons off the EDT and invokes
 * a callback on the EDT with the loaded ImageIcon (or null on failure).
 */
public class IconCache {
    private final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();
    private final Path iconDir = Path.of(System.getProperty("user.home"), ".weatherapp", "icons");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> new Thread(r, "IconCache-Loader"));

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

        // download off the EDT using a shared executor and HttpClient
        executor.submit(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    try (InputStream in = resp.body()) {
                        BufferedImage img = ImageIO.read(in);
                        if (img != null) {
                            ImageIcon icon = new ImageIcon(img);
                            cache.put(url, icon);
                            // persist to disk atomically
                            try {
                                String key = sha256(url);
                                Path p = iconDir.resolve(key + ".png");
                                Path tmp = iconDir.resolve(key + ".png.tmp");
                                ImageIO.write(img, "png", tmp.toFile());
                                try {
                                    Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                                } catch (AtomicMoveNotSupportedException amnse) {
                                    Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
                                }
                                // try to restrict permissions on POSIX
                                try {
                                    if (Files.getFileStore(iconDir).supportsFileAttributeView("posix")) {
                                        Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rw-------"));
                                    }
                                } catch (Exception ignored) {
                                }
                            } catch (Exception ex) {
                                // ignore disk write issues
                            }
                            SwingUtilities.invokeLater(() -> cb.accept(icon));
                            return;
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore network/parse errors
            }
            SwingUtilities.invokeLater(() -> cb.accept(null));
        });
    }

    /**
     * Return a cached ImageIcon if present in memory, otherwise null.
     */
    public ImageIcon getCached(String url) {
        return cache.get(url);
    }

    /**
     * Remove an entry from both memory cache and disk cache (best-effort).
     */
    public void remove(String url) {
        cache.remove(url);
        try {
            String key = sha256(url);
            Path p = iconDir.resolve(key + ".png");
            Files.deleteIfExists(p);
        } catch (Exception ignored) {
        }
    }

    /**
     * Clear in-memory cache. Does not remove files on disk.
     */
    public void clearMemoryCache() {
        cache.clear();
    }

    /**
     * Try to remove all files from the disk cache. This may be slow.
     */
    public void clearDiskCache() {
        try {
            if (Files.isDirectory(iconDir)) {
                try (var s = Files.list(iconDir)) {
                    s.forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
    }
}
