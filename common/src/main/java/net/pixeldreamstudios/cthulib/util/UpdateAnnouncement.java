package net.pixeldreamstudios.cthulib.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateAnnouncement {
    private static final File CACHE_FILE = new File(Minecraft.getInstance().gameDirectory, "cthulib_cache/update_announcement.json");
    private static final Pattern VERSION_NUMBER = Pattern.compile("\\d+");
    private static final String KEY_VERSION = "version";
    private static final String KEY_CHANGELOG = "changelog";
    private static final String KEY_FILE_DATE = "fileDate";
    private static final String KEY_DISMISSED = "dismissed";
    private static final AtomicReference<Announcement> PENDING = new AtomicReference<>();

    public record Announcement(String version, String installedVersion, String fileDate, String changelog) {
    }

    public static void check(String projectId, String installedVersion) {
        CthuLibConfig config = CthuLibConfig.getInstance();
        if (!config.showUpdateAnnouncement || projectId == null || installedVersion == null) {
            return;
        }

        Optional<String> newestName = CurseForgeAPI.newestFileName();
        if (newestName.isEmpty()) {
            return;
        }
        String version = ChangelogCache.extractVersion(config.changelogVersionPattern, newestName.get());
        if (version == null || !isNewer(version, installedVersion)) {
            return;
        }

        Optional<JsonObject> saved = readSaved().filter(json -> json.has(KEY_VERSION) && version.equals(json.get(KEY_VERSION).getAsString()));
        boolean dismissed = saved.map(json -> json.has(KEY_DISMISSED) && json.get(KEY_DISMISSED).getAsBoolean()).orElse(false);
        Optional<Announcement> announcement = saved.filter(json -> json.has(KEY_CHANGELOG) && json.has(KEY_FILE_DATE))
                .map(json -> new Announcement(version, installedVersion, stringOrNull(json, KEY_FILE_DATE), json.get(KEY_CHANGELOG).getAsString()));

        if (announcement.isEmpty()) {
            CurseForgeAPI.FetchedChangelog fetched = CurseForgeAPI.fetchNewestFileChangelog(projectId);
            if (fetched == null || fetched.changelog == null || fetched.changelog.isEmpty()) {
                return;
            }
            announcement = Optional.of(new Announcement(version, installedVersion, fetched.fileDate, fetched.changelog));
            write(announcement.get(), dismissed);
        }

        ChangelogCache.storeChangelog(version, announcement.get().fileDate(), announcement.get().changelog());
        if (!dismissed) {
            PENDING.set(announcement.get());
        }
    }

    private static String stringOrNull(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    public static Optional<Announcement> takePending() {
        return Optional.ofNullable(PENDING.getAndSet(null));
    }

    public static void dismiss(Announcement announcement) {
        write(announcement, true);
    }

    public static boolean isNewer(String candidate, String installed) {
        List<BigInteger> candidateParts = versionNumbers(candidate);
        List<BigInteger> installedParts = versionNumbers(installed);
        if (candidateParts.isEmpty() || installedParts.isEmpty()) {
            return !candidate.equals(installed);
        }
        int length = Math.max(candidateParts.size(), installedParts.size());
        for (int i = 0; i < length; i++) {
            BigInteger a = i < candidateParts.size() ? candidateParts.get(i) : BigInteger.ZERO;
            BigInteger b = i < installedParts.size() ? installedParts.get(i) : BigInteger.ZERO;
            int compared = a.compareTo(b);
            if (compared != 0) {
                return compared > 0;
            }
        }
        return false;
    }

    private static List<BigInteger> versionNumbers(String version) {
        List<BigInteger> parts = new ArrayList<>();
        Matcher matcher = VERSION_NUMBER.matcher(version);
        while (matcher.find()) {
            parts.add(new BigInteger(matcher.group()));
        }
        return parts;
    }

    private static Optional<JsonObject> readSaved() {
        if (!CACHE_FILE.exists()) {
            return Optional.empty();
        }
        try (FileReader reader = new FileReader(CACHE_FILE)) {
            return Optional.of(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static void write(Announcement announcement, boolean dismissed) {
        JsonObject json = new JsonObject();
        json.addProperty(KEY_VERSION, announcement.version());
        json.addProperty(KEY_FILE_DATE, announcement.fileDate());
        json.addProperty(KEY_CHANGELOG, announcement.changelog());
        json.addProperty(KEY_DISMISSED, dismissed);
        CACHE_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CACHE_FILE)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
