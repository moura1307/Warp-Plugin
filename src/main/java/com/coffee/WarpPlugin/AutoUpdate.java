package com.coffee.WarpPlugin;

import org.codehaus.plexus.util.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HexFormat;

public class AutoUpdate {
    private final Main plugin;

    private File pendingUpdateFile;

    public AutoUpdate(Main plugin) {
        this.plugin = plugin;
    }

    public void checkUpdates(String githubRepo, String currentVersion) {
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

                String latestVersion = latestTag.replace("v", "");

                if (isNewerVersion(latestVersion, currentVersion)) {
                    JSONArray assets = (JSONArray) response.get("assets");
                    if (assets == null || assets.isEmpty()) return;

                    String downloadUrl = (String) ((JSONObject) assets.get(0)).get("browser_download_url");

                    // Add the new message condition
                    if (!plugin.getConfig().getBoolean("auto-update", true)) {
                        plugin.getLogger().warning("==============================================");
                        plugin.getLogger().warning("A new update (v" + latestVersion + ") is available!");
                        plugin.getLogger().warning("Download it at: " + downloadUrl);
                        plugin.getLogger().warning("Or enable auto-update in config.yml");
                        plugin.getLogger().warning("==============================================");
                    } else {
                        // Existing auto-update logic
                        downloadUpdate(downloadUrl, latestVersion, githubRepo);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Update check failed: " + e.getMessage());
        }
    }

    public boolean isNewerVersion(String latest, String current) {
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

    private void downloadUpdate(String downloadUrl, String latestVersion, String githubRepo) {
        File tempFile = null;
        try {
            //how tf didnt i know this existed????????
            File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
            if (!updateFolder.exists()) updateFolder.mkdirs();

            String fileName = "TheWarpPlugin.jar";
            tempFile = new File(updateFolder, fileName + ".tmp");
            File finalUpdateFile = new File(updateFolder, fileName);

            URL url = new URL(downloadUrl);
            FileUtils.copyURLToFile(url, tempFile);

            String expectedHash = getExpectedHashFromGitHub(githubRepo);
            if (expectedHash != null) {
                String actualHash = calculateSHA256(tempFile);
                if (!expectedHash.equalsIgnoreCase(actualHash)) {
                    tempFile.delete();
                    plugin.getLogger().severe("Hash verification failed! Update aborted.");
                    return;
                }
            }

            if (finalUpdateFile.exists()) finalUpdateFile.delete();
            if (!tempFile.renameTo(finalUpdateFile)) {
                throw new Exception("Failed to finalize update file.");
            }

            plugin.getLogger().info("========================================");
            plugin.getLogger().info("Update v" + latestVersion + " is ready!");
            plugin.getLogger().info("It has been placed in the /plugins/update folder.");
            plugin.getLogger().info("RESTART your server to apply the changes.");
            plugin.getLogger().info("========================================");

        } catch (Exception e) {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            plugin.getLogger().warning("Update failed: " + e.getMessage());
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

    private String getExpectedHashFromGitHub(String githubRepo) {
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
            plugin.getLogger().warning("Couldn't fetch hash from GitHub: " + e.getMessage());
        }
        return null;
    }

    public void applyUpdate() {
        File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
        File pending = new File(updateFolder, "TheWarpPlugin.jar");

        if (pending.exists()) {
            plugin.getLogger().info("=== UPDATE READY ===");
            plugin.getLogger().info("The new version is in the update folder.");
            plugin.getLogger().info("Restart the server to apply the changes!");
        }
    }
}
