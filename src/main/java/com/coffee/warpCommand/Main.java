package com.coffee.warpCommand;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.util.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;

public class Main extends JavaPlugin {
    private File pendingUpdateFile;
    private String currentVersion;
    private String githubRepo;
    private long lastUpdateCheck = 0;
    private WarpCommand warpCommand;
    private WarpConfig warpConfig;

    @Override
    public void onEnable() {
        // Initialize configuration first
        saveDefaultConfig();

        // Initialize commands
        this.warpCommand = new WarpCommand(this);
        this.warpConfig = new WarpConfig(this);

        // Register command executors
        getCommand("warp").setExecutor(warpCommand);
        getCommand("warpconfig").setExecutor(warpConfig);
        getCommand("warpconfig").setTabCompleter(warpConfig);

        // Load saved locations
        warpCommand.loadLocations();

        // Set up update checker
        currentVersion = getDescription().getVersion();
        githubRepo = "moura1307/Warp-Plugin";

        checkUpdates();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::checkUpdates, 0, 20 * 60 * 60 * 24);
    }

    private void downloadUpdate(String downloadUrl) {
        File tempFile = null;
        try {
            // Setup download paths
            URL url = new URL(downloadUrl);
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
            File pluginsDir = new File(Bukkit.getServer().getWorldContainer(), "plugins");
            tempFile = new File(pluginsDir, fileName + ".tmp");
            File updateFile = new File(pluginsDir, fileName);

            // Download to temporary file
            FileUtils.copyURLToFile(url, tempFile);

            // Get expected hash from GitHub
            String expectedHash = getExpectedHashFromGitHub();
            if (expectedHash != null) {
                // Calculate actual hash using Java's built-in MessageDigest
                String actualHash = calculateSHA256(tempFile);

                if (!expectedHash.equalsIgnoreCase(actualHash)) {
                    tempFile.delete();
                    getLogger().severe("Hash verification failed! Downloaded file may be corrupted or tampered with.");
                    return;
                }
            }

            // Safe file replacement
            if (updateFile.exists()) {
                File backupFile = new File(pluginsDir, fileName + ".bak");
                if (backupFile.exists()) backupFile.delete();
                updateFile.renameTo(backupFile);
            }

            if (!tempFile.renameTo(updateFile)) {
                throw new Exception("Failed to rename temporary file");
            }

            getLogger().warning("Update successfully downloaded. Restart server to apply v" +
                    getDescription().getVersion());

        } catch (Exception e) {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            getLogger().warning("Update failed: " + e.getMessage());
        }
    }

    private void checkUpdates() {
        if (!getConfig().getBoolean("auto-update.enabled", true)) return;

        try {
            URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONObject response = (JSONObject) new JSONParser().parse(
                        new InputStreamReader(connection.getInputStream())
                );

                String latestTag = (String) response.get("tag_name");
                if (latestTag == null) return;

                JSONArray assets = (JSONArray) response.get("assets");
                if (assets == null || assets.isEmpty()) return;

                String latestVersion = latestTag.replace("v", "");
                String downloadUrl = (String) ((JSONObject) assets.get(0)).get("browser_download_url");

                if (isNewerVersion(latestVersion, currentVersion)) {
                    downloadUpdate(downloadUrl);
                }
            }
        } catch (Exception e) {
            getLogger().warning("Update check failed: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        warpCommand.saveLocations();

        if (pendingUpdateFile != null && pendingUpdateFile.exists()) {
            try {
                File currentJar = getFile();
                File pluginsDir = currentJar.getParentFile();

                // Create backup
                File backupFile = new File(pluginsDir, currentJar.getName() + ".bak");
                if (backupFile.exists()) Files.delete(backupFile.toPath());
                Files.copy(currentJar.toPath(), backupFile.toPath());

                // Apply update
                Files.move(
                        pendingUpdateFile.toPath(),
                        currentJar.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );

                getLogger().info("Successfully updated to new version!");
            } catch (Exception e) {
                getLogger().severe("Failed to apply update: " + e.getMessage());
                try {
                    if (pendingUpdateFile.exists()) Files.delete(pendingUpdateFile.toPath());
                } catch (Exception ex) {
                    getLogger().warning("Failed to clean up: " + ex.getMessage());
                }
            }
        }
    }

    private String calculateSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteBuffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(byteBuffer)) != -1) {
                digest.update(byteBuffer, 0, bytesRead);
            }
        }
        byte[] hashedBytes = digest.digest();
        return HexFormat.of().formatHex(hashedBytes);
    }

    private String getExpectedHashFromGitHub() {
        try {
            URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (conn.getResponseCode() == 200) {
                JSONObject response = (JSONObject) new JSONParser().parse(
                        new InputStreamReader(conn.getInputStream())
                );

                // Option 1: Get from release body
                String body = (String) response.get("body");
                if (body != null) {
                    for (String line : body.split("\\r?\\n")) {
                        if (line.trim().startsWith("SHA-256:")) {
                            return line.trim().substring(8).trim().split("\\s+")[0];
                        }
                    }
                }

                // Option 2: Get from asset metadata (would require auth)
            }
        } catch (Exception e) {
            getLogger().warning("Couldn't fetch hash from GitHub: " + e.getMessage());
        }
        return null;
    }


    private boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
            int latestNum = (i < latestParts.length) ? Integer.parseInt(latestParts[i]) : 0;
            int currentNum = (i < currentParts.length) ? Integer.parseInt(currentParts[i]) : 0;

            if (latestNum > currentNum) return true;
            if (latestNum < currentNum) return false;
        }
        return false;
    }
}