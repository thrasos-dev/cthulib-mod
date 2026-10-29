package net.pixeldreamstudios.cthulib.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChangelogCache {
    private static final File CACHE_DIR = new File(Minecraft.getInstance().gameDirectory, "cthulib_cache/changelogs");
    private static final File PROMO_CACHE_FILE = new File(Minecraft.getInstance().gameDirectory, "cthulib_cache/promo_card.json");
    private static final File READ_CACHE_FILE = new File(Minecraft.getInstance().gameDirectory, "cthulib_cache/read_changelog.json");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy-HHmm");
    private static CachedChangelog latestCached = null;
    private static boolean latestCacheLoaded = false;
    private static CachedChangelog sessionFetchedChangelog = null;
    private static boolean sessionFetchAttempted = false;
    private static String cachedPromoCardUrl = null;
    private static String lastReadHash = null;
    private static boolean readHashLoaded = false;
    private static String cachedVersion = null;
    private static String cachedProjectId = null;
    private static boolean bccLoaded = false;
    private static boolean usesTomlConfig = false;

    static {
        CACHE_DIR.mkdirs();
        new File(Minecraft.getInstance().gameDirectory, "cthulib_cache").mkdirs();
    }

    public static void setUsesTomlConfig(boolean usesToml) {
        usesTomlConfig = usesToml;
    }

    public static class CachedChangelog {
        public String version;
        public String changelog;
        public long timestamp;
        public String fileDate;

        public CachedChangelog(String version, String changelog, long timestamp) {
            this(version, changelog, timestamp, null);
        }

        public CachedChangelog(String version, String changelog, long timestamp, String fileDate) {
            this.version = version;
            this.changelog = changelog;
            this.timestamp = timestamp;
            this.fileDate = fileDate;
        }
    }

    private static void loadBccConfig() {
        if (bccLoaded) return;
        bccLoaded = true;

        if (usesTomlConfig) {
            File bccToml = new File(Minecraft.getInstance().gameDirectory, "config/bcc-common.toml");
            if (bccToml.exists()) {
                loadBccToml(bccToml);
            }
        } else {
            File bccJson = new File(Minecraft.getInstance().gameDirectory, "config/bcc.json");
            if (bccJson.exists()) {
                loadBccJson(bccJson);
            }
        }
    }

    private static void loadBccJson(File bccFile) {
        try (FileReader reader = new FileReader(bccFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (cachedVersion == null) {
                if (json.has("modpackVersion")) {
                    cachedVersion = json.get("modpackVersion").getAsString();
                } else if (json.has("ignoreUpdatesAbove")) {
                    cachedVersion = json.get("ignoreUpdatesAbove").getAsString();
                }
            }
            if (cachedProjectId == null && json.has("projectID") && json.get("projectID").isJsonPrimitive()) {
                cachedProjectId = json.get("projectID").getAsJsonPrimitive().isString()
                    ? json.get("projectID").getAsString()
                    : String.valueOf(json.get("projectID").getAsLong());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadBccToml(File bccFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(bccFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int equalsIndex = line.indexOf('=');
                if (equalsIndex == -1) continue;

                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }

                if (key.equals("modpackVersion")) {
                    cachedVersion = value;
                } else if (key.equals("modpackProjectID")) {
                    cachedProjectId = value;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentVersion() {
        loadBccConfig();
        return cachedVersion;
    }

    public static String getProjectId() {
        loadBccConfig();
        return cachedProjectId;
    }

    private static String formatFileDate(String isoFileDate) {
        if (isoFileDate == null) {
            return "unknown";
        }
        try {
            return FILE_DATE_FORMAT.format(Instant.parse(isoFileDate).atZone(ZoneOffset.UTC));
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String sanitizeForFilename(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static String extractVersion(String template, String displayName) {
        if (template == null || displayName == null) {
            return null;
        }

        int placeholderIndex = template.indexOf("{version}");
        if (placeholderIndex < 0) {
            return null;
        }

        String prefix = template.substring(0, placeholderIndex);
        String suffix = template.substring(placeholderIndex + "{version}".length());

        if (!displayName.startsWith(prefix)) {
            return null;
        }

        int suffixIndex = displayName.indexOf(suffix, prefix.length());
        if (suffixIndex < 0) {
            return null;
        }

        String extracted = displayName.substring(prefix.length(), suffixIndex).trim();
        return extracted.isEmpty() ? null : extracted;
    }

    public static CachedChangelog getLatestCachedChangelog() {
        if (latestCacheLoaded) {
            return latestCached;
        }
        latestCacheLoaded = true;

        File[] files = CACHE_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return null;
        }

        File newestFile = null;
        for (File file : files) {
            if (newestFile == null || file.lastModified() > newestFile.lastModified()) {
                newestFile = file;
            }
        }

        latestCached = readCacheFile(newestFile);
        return latestCached;
    }

    private static CachedChangelog readCacheFile(File cacheFile) {
        try (FileReader reader = new FileReader(cacheFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String version = json.get("version").getAsString();
            String changelog = json.get("changelog").getAsString();
            long timestamp = json.get("timestamp").getAsLong();
            String fileDate = json.has("fileDate") && !json.get("fileDate").isJsonNull()
                    ? json.get("fileDate").getAsString()
                    : parseFileDateFromFilename(cacheFile.getName());
            return new CachedChangelog(version, changelog, timestamp, fileDate);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String parseFileDateFromFilename(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        String nameWithoutExt = dotIndex >= 0 ? filename.substring(0, dotIndex) : filename;
        int dashIndex = nameWithoutExt.lastIndexOf('-');
        if (dashIndex < 0) {
            return null;
        }

        String datePart = nameWithoutExt.substring(Math.max(0, nameWithoutExt.length() - "ddMMyyyy-HHmm".length()));
        try {
            LocalDateTime parsed = LocalDateTime.parse(datePart, FILE_DATE_FORMAT);
            return parsed.atZone(ZoneOffset.UTC).toInstant().toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean cacheFileExists(String fetchedVersion, String fileDate) {
        String filename = sanitizeForFilename(fetchedVersion) + "-" + formatFileDate(fileDate) + ".json";
        return new File(CACHE_DIR, filename).exists();
    }

    public static void saveChangelog(String fetchedVersion, String fileDate, String changelog) {
        try {
            long timestamp = System.currentTimeMillis();
            CachedChangelog cached = new CachedChangelog(fetchedVersion, changelog, timestamp, fileDate);
            latestCached = cached;
            latestCacheLoaded = true;

            String filename = sanitizeForFilename(fetchedVersion) + "-" + formatFileDate(fileDate) + ".json";
            File cacheFile = new File(CACHE_DIR, filename);
            JsonObject json = new JsonObject();
            json.addProperty("version", fetchedVersion);
            json.addProperty("fileDate", fileDate);
            json.addProperty("changelog", changelog);
            json.addProperty("timestamp", timestamp);

            try (FileWriter writer = new FileWriter(cacheFile)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<CachedChangelog> listCachedChangelogsSorted() {
        File[] files = CACHE_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        List<CachedChangelog> entries = new ArrayList<>();
        for (File file : files) {
            CachedChangelog cached = readCacheFile(file);
            if (cached != null) {
                entries.add(cached);
            }
        }

        entries.sort(Comparator.comparing(c -> parseInstantOrEpoch(c.fileDate)));
        return entries;
    }

    private static Instant parseInstantOrEpoch(String isoFileDate) {
        if (isoFileDate == null) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(isoFileDate);
        } catch (Exception e) {
            return Instant.EPOCH;
        }
    }

    public static int findIndexForVersion(List<CachedChangelog> sorted, String version) {
        if (sorted == null || version == null) {
            return -1;
        }
        for (int i = 0; i < sorted.size(); i++) {
            if (version.equals(sorted.get(i).version)) {
                return i;
            }
        }
        return -1;
    }

    public static String getDisplayedFileDate() {
        if (sessionFetchedChangelog != null) {
            return sessionFetchedChangelog.fileDate;
        }
        CachedChangelog cached = getLatestCachedChangelog();
        return cached != null ? cached.fileDate : null;
    }

    public static int findIndexForFileDate(List<CachedChangelog> sorted, String fileDate) {
        if (sorted == null || fileDate == null) {
            return -1;
        }
        for (int i = 0; i < sorted.size(); i++) {
            if (fileDate.equals(sorted.get(i).fileDate)) {
                return i;
            }
        }
        return -1;
    }

    public static String getChangelog(String projectId, String version) {
        if (sessionFetchAttempted && sessionFetchedChangelog != null) {
            return sessionFetchedChangelog.changelog;
        }

        String pattern = CthuLibConfig.getInstance().changelogVersionPattern;
        CurseForgeAPI.FetchedChangelog fetched = CurseForgeAPI.fetchLatestChangelog(projectId,
                displayName -> version != null && version.equals(extractVersion(pattern, displayName)));

        if (fetched != null && fetched.changelog != null && !fetched.changelog.isEmpty()) {
            String extractedVersion = extractVersion(pattern, fetched.displayName);
            String cacheVersion = extractedVersion != null ? extractedVersion : sanitizeForFilename(fetched.displayName);

            if (!cacheFileExists(cacheVersion, fetched.fileDate)) {
                saveChangelog(cacheVersion, fetched.fileDate, fetched.changelog);
            } else {
                latestCached = new CachedChangelog(cacheVersion, fetched.changelog, System.currentTimeMillis(), fetched.fileDate);
                latestCacheLoaded = true;
            }

            sessionFetchedChangelog = new CachedChangelog(cacheVersion, fetched.changelog, System.currentTimeMillis(), fetched.fileDate);
            sessionFetchAttempted = true;
            return fetched.changelog;
        }

        sessionFetchAttempted = true;

        CachedChangelog cached = getLatestCachedChangelog();
        if (cached != null) {
            return cached.changelog;
        }

        return "No changelog available";
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

    private static String computeContentHash(String content) {
        return Integer.toHexString(content.hashCode()) + "_" + content.length();
    }

    private static String getLastReadHash() {
        if (!readHashLoaded) {
            readHashLoaded = true;
            if (READ_CACHE_FILE.exists()) {
                try (FileReader reader = new FileReader(READ_CACHE_FILE)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("hash")) {
                        lastReadHash = json.get("hash").getAsString();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return lastReadHash;
    }

    public static void markChangelogAsRead(String projectId) {
        CachedChangelog cached = getLatestCachedChangelog();
        if (cached == null || cached.changelog == null || cached.changelog.isEmpty()) return;

        String hash = computeContentHash(cached.changelog);
        lastReadHash = hash;
        readHashLoaded = true;

        try {
            READ_CACHE_FILE.getParentFile().mkdirs();
            JsonObject json = new JsonObject();
            json.addProperty("hash", hash);
            json.addProperty("timestamp", System.currentTimeMillis());
            try (FileWriter writer = new FileWriter(READ_CACHE_FILE)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isChangelogUnread(String projectId) {
        CachedChangelog cached = getLatestCachedChangelog();
        if (cached == null || cached.changelog == null || cached.changelog.isEmpty()) return false;

        String cachedHash = computeContentHash(cached.changelog);
        String readHash = getLastReadHash();
        return !cachedHash.equals(readHash);
    }
}
