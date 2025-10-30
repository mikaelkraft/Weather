package com.weatherapp;

import com.google.gson.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.nio.file.attribute.PosixFilePermission;


/**
 * Simple config manager that persists a small JSON config under ~/.weatherapp/config.json
 */
public class ConfigManager {
    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".weatherapp");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path secretKeyFile = CONFIG_DIR.resolve("secret.key");

    // AES-GCM parameters
    private static final int GCM_TAG_LEN = 128;
    private static final int IV_LEN = 12;

    public ConfigManager() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
        } catch (Exception ex) {
            // ignore creation errors; will try to save later
        }
    }

    public String getApiKey() {
        try {
            File f = CONFIG_FILE.toFile();
            if (!f.exists()) return null;
            try (FileReader fr = new FileReader(f)) {
                JsonObject o = JsonParser.parseReader(fr).getAsJsonObject();
                if (o.has("apiKey")) {
                    String stored = o.get("apiKey").getAsString();
                    // if the stored value looks like base64 blob with ':' separator, treat as encrypted
                    if (stored.contains(":")) {
                        return decryptApiKey(stored);
                    }
                    return stored;
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    public boolean saveApiKey(String apiKey) {
        try {
            // encrypt api key before saving
            String enc = encryptApiKey(apiKey);
            JsonObject o = new JsonObject();
            o.addProperty("apiKey", enc);
            try (FileWriter fw = new FileWriter(CONFIG_FILE.toFile())) {
                gson.toJson(o, fw);
            }
            // ensure secret key file has restrictive permissions where supported
            try {
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------");
                java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = java.nio.file.attribute.PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(secretKeyFile, perms);
            } catch (Exception ignore) {}
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private byte[] getSecretKey() throws Exception {
        if (Files.exists(secretKeyFile)) {
            return Files.readAllBytes(secretKeyFile);
        }
        // generate random 32-byte key
        byte[] key = new byte[32];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(key);
        Files.write(secretKeyFile, key);
        try {
                java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = java.nio.file.attribute.PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(secretKeyFile, perms);
            } catch (Exception ignore) {}
        return key;
    }

    private String encryptApiKey(String plain) throws Exception {
        byte[] key = getSecretKey();
        byte[] iv = new byte[IV_LEN];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec kspec = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LEN, iv);
        cipher.init(Cipher.ENCRYPT_MODE, kspec, spec);
        byte[] ct = cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String enc = Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ct);
        return enc;
    }

    private String decryptApiKey(String stored) throws Exception {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return stored;
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] ct = Base64.getDecoder().decode(parts[1]);
        byte[] key = getSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec kspec = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LEN, iv);
        cipher.init(Cipher.DECRYPT_MODE, kspec, spec);
        byte[] plain = cipher.doFinal(ct);
        return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
    }
}
