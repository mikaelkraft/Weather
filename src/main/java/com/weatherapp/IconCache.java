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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Async icon loader with in-memory cache and disk persistence.
 * Icons are normalized to a square size and a lightweight placeholder is
 * immediately delivered while the real icon is fetched.
 */
public class IconCache {
    private final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();
    private final Path iconDir = Path.of(System.getProperty("user.home"), ".weatherapp", "icons");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> new Thread(r, "IconCache-Loader"));
    private final int iconSize; // square size (px)
    private final ImageIcon placeholder;

    /**
     * Default constructor uses 48x48 icons.
     */
    public IconCache() {
        this(48);
    }

    /**
     * Create an IconCache that normalizes icons to a square of size {@code iconSize}px.
     */
    public IconCache(int iconSize) {
        this.iconSize = Math.max(16, iconSize);
        this.placeholder = new ImageIcon(makePlaceholderImage(this.iconSize));
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

        // immediately show a lightweight placeholder so UI can layout
        SwingUtilities.invokeLater(() -> cb.accept(placeholder));

        // check disk cache
        try {
            String key = sha256(url);
            Path p = iconDir.resolve(key + ".png");
            if (Files.exists(p)) {
                BufferedImage img = ImageIO.read(p.toFile());
                if (img != null) {
                    // ensure normalized size when loading from disk
                    BufferedImage scaled = scaleToSquare(img, iconSize);
                    ImageIcon icon = new ImageIcon(scaled);
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
                            BufferedImage scaled = scaleToSquare(img, iconSize);
                            ImageIcon icon = new ImageIcon(scaled);
                            cache.put(url, icon);
                            // persist to disk atomically
                            try {
                                String key = sha256(url);
                                Path p = iconDir.resolve(key + ".png");
                                Path tmp = iconDir.resolve(key + ".png.tmp");
                                ImageIO.write(scaled, "png", tmp.toFile());
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
     * Start downloading and caching the icon for {@code url} without delivering it to a callback.
     * Useful to warm the cache ahead of time.
     */
    public void prefetch(String url) {
        if (url == null || url.isEmpty()) return;
        if (cache.containsKey(url)) return;

        executor.submit(() -> {
            try {
                String key = sha256(url);
                Path p = iconDir.resolve(key + ".png");
                // try disk first
                if (Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        cache.put(url, new ImageIcon(img));
                        return;
                    }
                }

                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build();
                HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    try (InputStream in = resp.body()) {
                        BufferedImage img = ImageIO.read(in);
                        if (img != null) {
                            BufferedImage scaled = scaleToSquare(img, iconSize);
                            cache.put(url, new ImageIcon(scaled));
                            try {
                                Path tmp = iconDir.resolve(key + ".png.tmp");
                                ImageIO.write(scaled, "png", tmp.toFile());
                                try {
                                    Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                                } catch (AtomicMoveNotSupportedException amnse) {
                                    Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private static BufferedImage scaleToSquare(BufferedImage src, int size) {
        if (src.getWidth() == size && src.getHeight() == size) return src;
        BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            // scale while preserving aspect ratio and center
            double sx = (double) size / src.getWidth();
            double sy = (double) size / src.getHeight();
            double s = Math.min(sx, sy);
            int w = (int) Math.round(src.getWidth() * s);
            int h = (int) Math.round(src.getHeight() * s);
            int x = (size - w) / 2;
            int y = (size - h) / 2;
            g.setComposite(java.awt.AlphaComposite.SrcOver);
            g.setColor(new Color(0,0,0,0));
            g.fillRect(0,0,size,size);
            g.drawImage(src, x, y, w, h, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    private static BufferedImage makePlaceholderImage(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(220,220,220,255));
            g.fillRect(0,0,size,size);
            g.setColor(new Color(200,200,200,255));
            int pad = Math.max(2, size/8);
            g.fillRoundRect(pad, pad, size-2*pad, size-2*pad, pad, pad);
            g.setColor(new Color(160,160,160,255));
            int c = size/3;
            g.fillOval((size-c)/2, (size-c)/2, c, c);
        } finally {
            g.dispose();
        }
        return img;
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
