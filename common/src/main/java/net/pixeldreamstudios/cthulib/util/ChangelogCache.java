package net.pixeldreamstudios.cthulib.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class ChangelogCache {
    private static final File CACHE_DIR = new File(Minecraft.getInstance().gameDirectory, "cthulib_cache/changelogs");
    private static final File PROMO_CACHE_FILE = new File(Minecraft.getInstance().gameDirectory, "cthulib_cache/promo_card.json");
    private static final Map<String, CachedChangelog> cache = new HashMap<>();
    private static String cachedPromoCardUrl = null;
    
    static {
        CACHE_DIR.mkdirs();
    }

    public static class CachedChangelog {
        public String version;
        public String changelog;
        public long timestamp;

        public CachedChangelog(String version, String changelog, long timestamp) {
            this.version = version;
            this.changelog = changelog;
            this.timestamp = timestamp;
        }
    }

    public static String getCurrentVersion() {
        try {
            File bccFile = new File(Minecraft.getInstance().gameDirectory, "config/bcc.json");
            if (!bccFile.exists()) {
                return null;
            }

            try (FileReader reader = new FileReader(bccFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("modpackVersion")) {
                    return json.get("modpackVersion").getAsString();
                } else if (json.has("ignoreUpdatesAbove")) {
                    return json.get("ignoreUpdatesAbove").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getProjectId() {
        try {
            File bccFile = new File(Minecraft.getInstance().gameDirectory, "config/bcc.json");
            if (!bccFile.exists()) {
                return null;
            }

            try (FileReader reader = new FileReader(bccFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("projectID")) {
                    if (json.get("projectID").isJsonPrimitive()) {
                        return json.get("projectID").getAsJsonPrimitive().isString() 
                            ? json.get("projectID").getAsString()
                            : String.valueOf(json.get("projectID").getAsLong());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CachedChangelog getCachedChangelog(String projectId) {
        if (cache.containsKey(projectId)) {
            return cache.get(projectId);
        }

        File cacheFile = new File(CACHE_DIR, projectId + ".json");
        if (!cacheFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(cacheFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String version = json.get("version").getAsString();
            String changelog = json.get("changelog").getAsString();
            long timestamp = json.get("timestamp").getAsLong();

            CachedChangelog cached = new CachedChangelog(version, changelog, timestamp);
            cache.put(projectId, cached);
            return cached;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveChangelog(String projectId, String version, String changelog) {
        try {
            CachedChangelog cached = new CachedChangelog(version, changelog, System.currentTimeMillis());
            cache.put(projectId, cached);

            File cacheFile = new File(CACHE_DIR, projectId + ".json");
            JsonObject json = new JsonObject();
            json.addProperty("version", version);
            json.addProperty("changelog", changelog);
            json.addProperty("timestamp", cached.timestamp);

            try (FileWriter writer = new FileWriter(cacheFile)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean shouldUpdateChangelog(String projectId) {
        String currentVersion = getCurrentVersion();
        if (currentVersion == null) {
            return false;
        }

        CachedChangelog cached = getCachedChangelog(projectId);
        if (cached == null) {
            return true;
        }

        return !cached.version.equals(currentVersion);
    }

    public static String getChangelog(String projectId, String version) {
        if (!shouldUpdateChangelog(projectId)) {
            CachedChangelog cached = getCachedChangelog(projectId);
            if (cached != null && cached.version.equals(version)) {
                return cached.changelog;
            }
        }

        String changelog = fetchChangelogFromBackend(projectId, version);
        
        if (changelog != null && !changelog.isEmpty()) {
            saveChangelog(projectId, version, changelog);
            return changelog;
        }

        CachedChangelog cached = getCachedChangelog(projectId);
        if (cached != null) {
            return cached.changelog;
        }

        return "No changelog available";
    }

    private static String fetchChangelogFromBackend(String projectId, String version) {
        String changelog = CurseForgeAPI.fetchChangelog(projectId, version);
        
        if (changelog == null || changelog.isEmpty()) {
            return null;
        }
        
        return changelog;
    }
    
    public static String getPromoCardUrl() {
        if (cachedPromoCardUrl != null) {
            return cachedPromoCardUrl;
        }
        
        if (PROMO_CACHE_FILE.exists()) {
            try (FileReader reader = new FileReader(PROMO_CACHE_FILE)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                cachedPromoCardUrl = json.get("url").getAsString();
                return cachedPromoCardUrl;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return null;
    }
    
    public static void savePromoCard(String url) {
        try {
            cachedPromoCardUrl = url;
            
            JsonObject json = new JsonObject();
            json.addProperty("url", url);
            json.addProperty("timestamp", System.currentTimeMillis());
            
            try (FileWriter writer = new FileWriter(PROMO_CACHE_FILE)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
