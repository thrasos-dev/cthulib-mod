package net.pixeldreamstudios.cthulib.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import net.pixeldreamstudios.cthulib.client.ProjectData;
import net.pixeldreamstudios.cthulib.client.ProjectType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurseForgeAPI {
    private static final String PROXY_BASE = "https://admin.pixeldreamstudios.net/api/cf-proxy";
    private static final String API_TOKEN = "Q560FhgPpr/HF9+8uZqPrpHBDd5eXDXDTWx+nFJwwqI=";
    private static final int GAME_ID = 432;
    private static final String AUTHOR_ID = "102811478";

    public static List<ProjectData> fetchAuthorProjects() {
        List<ProjectData> projects = new ArrayList<>();
        
        try {
            URI uri = new URI(PROXY_BASE + "/v1/mods/search?gameId=" + GAME_ID + "&authorId=" + AUTHOR_ID + "&pageSize=50");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestProperty("X-Mod-Token", API_TOKEN);
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                JsonArray data = json.getAsJsonArray("data");
                
                for (JsonElement elem : data) {
                    JsonObject mod = elem.getAsJsonObject();
                    
                    String projectId = String.valueOf(mod.get("id").getAsLong());
                    String name = mod.get("name").getAsString();
                    String summary = mod.get("summary").getAsString();
                    
                    String iconUrl = "";
                    if (mod.has("logo") && !mod.get("logo").isJsonNull()) {
                        iconUrl = mod.getAsJsonObject("logo").get("url").getAsString();
                    }
                    
                    List<String> supportedVersions = new ArrayList<>();
                    List<String> loaders = new ArrayList<>();
                    if (mod.has("latestFilesIndexes")) {
                        JsonArray filesIndexes = mod.getAsJsonArray("latestFilesIndexes");
                        for (JsonElement fileElem : filesIndexes) {
                            JsonObject file = fileElem.getAsJsonObject();
                            if (file.has("gameVersion")) {
                                String version = file.get("gameVersion").getAsString();
                                if (!supportedVersions.contains(version)) {
                                    supportedVersions.add(version);
                                }
                            }
                            if (file.has("modLoader")) {
                                int loaderType = file.get("modLoader").getAsInt();
                                String loaderName = getLoaderName(loaderType);
                                if (!loaders.contains(loaderName)) {
                                    loaders.add(loaderName);
                                }
                            }
                        }
                    }
                    
                    List<String> authors = new ArrayList<>();
                    if (mod.has("authors")) {
                        JsonArray authorsArray = mod.getAsJsonArray("authors");
                        for (JsonElement authorElem : authorsArray) {
                            JsonObject author = authorElem.getAsJsonObject();
                            authors.add(author.get("name").getAsString());
                        }
                    }
                    
                    int classId = mod.get("classId").getAsInt();
                    ProjectType type = classId == 4471 ? ProjectType.MODPACK : ProjectType.MOD;
                    
                    JsonObject links = mod.getAsJsonObject("links");
                    String projectUrl = links.get("websiteUrl").getAsString();
                    
                    projects.add(new ProjectData(projectId, name, summary, iconUrl,
                            supportedVersions, loaders, authors, type, projectUrl));
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return projects;
    }

    public static class FetchedChangelog {
        public final String displayName;
        public final String fileDate;
        public final String changelog;

        public FetchedChangelog(String displayName, String fileDate, String changelog) {
            this.displayName = displayName;
            this.fileDate = fileDate;
            this.changelog = changelog;
        }
    }

    private static final int FILE_SEARCH_PAGE_SIZE = 5;

    private static JsonArray listFiles(String projectId, int pageSize) throws Exception {
        URI filesUri = new URI(PROXY_BASE + "/v1/mods/" + projectId + "/files?pageSize=" + pageSize + "&index=0");
        HttpURLConnection filesConn = (HttpURLConnection) filesUri.toURL().openConnection();
        filesConn.setRequestProperty("X-Mod-Token", API_TOKEN);
        filesConn.setRequestMethod("GET");

        if (filesConn.getResponseCode() != 200) {
            filesConn.disconnect();
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(filesConn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        filesConn.disconnect();

        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
        if (!json.has("data")) {
            return null;
        }

        JsonArray files = json.getAsJsonArray("data");
        return files.size() > 0 ? files : null;
    }

    private static JsonObject findMatch(JsonArray files, Function<String, Boolean> versionMatcher) {
        if (files == null || versionMatcher == null) {
            return null;
        }
        for (JsonElement elem : files) {
            JsonObject file = elem.getAsJsonObject();
            String displayName = file.get("displayName").getAsString();
            if (Boolean.TRUE.equals(versionMatcher.apply(displayName))) {
                return file;
            }
        }
        return null;
    }

    private static FetchedChangelog fetchChangelogForFile(String projectId, JsonObject file) throws Exception {
        String fileId = String.valueOf(file.get("id").getAsLong());
        String displayName = file.get("displayName").getAsString();
        String fileDate = file.has("fileDate") && !file.get("fileDate").isJsonNull()
                ? file.get("fileDate").getAsString()
                : null;

        URI changelogUri = new URI(PROXY_BASE + "/v1/mods/" + projectId + "/files/" + fileId + "/changelog");
        HttpURLConnection changelogConn = (HttpURLConnection) changelogUri.toURL().openConnection();
        changelogConn.setRequestProperty("X-Mod-Token", API_TOKEN);
        changelogConn.setRequestMethod("GET");

        if (changelogConn.getResponseCode() == 200) {
            BufferedReader changelogReader = new BufferedReader(new InputStreamReader(changelogConn.getInputStream()));
            StringBuilder changelogResponse = new StringBuilder();
            String changelogLine;
            while ((changelogLine = changelogReader.readLine()) != null) {
                changelogResponse.append(changelogLine);
            }
            changelogReader.close();

            JsonObject changelogJson = JsonParser.parseString(changelogResponse.toString()).getAsJsonObject();
            if (changelogJson.has("data") && !changelogJson.get("data").isJsonNull()) {
                String htmlChangelog = changelogJson.get("data").getAsString();
                changelogConn.disconnect();

                try {
                    MutableDataSet options = new MutableDataSet()
                        .set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false);
                    FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder(options).build();
                    String markdownChangelog = converter.convert(htmlChangelog);
                    return new FetchedChangelog(displayName, fileDate, markdownChangelog);
                } catch (Exception conversionError) {
                    System.err.println("[CthuLib] HTML to Markdown conversion failed: " + conversionError.getMessage());
                    return new FetchedChangelog(displayName, fileDate, htmlChangelog);
                }
            }
        }
        changelogConn.disconnect();
        return null;
    }

    public static FetchedChangelog fetchLatestChangelog(String projectId, Function<String, Boolean> versionMatcher) {
        try {
            JsonArray latestOnly = listFiles(projectId, 1);
            if (latestOnly == null) {
                return null;
            }
            JsonObject latestFile = latestOnly.get(0).getAsJsonObject();

            if (versionMatcher != null && Boolean.TRUE.equals(versionMatcher.apply(latestFile.get("displayName").getAsString()))) {
                return fetchChangelogForFile(projectId, latestFile);
            }

            JsonArray recentFiles = listFiles(projectId, FILE_SEARCH_PAGE_SIZE);
            JsonObject matched = findMatch(recentFiles, versionMatcher);

            return fetchChangelogForFile(projectId, matched != null ? matched : latestFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    
    public static String extractPromoCard(String changelog) {
        if (changelog == null || changelog.isEmpty()) {
            return null;
        }
        
        Pattern pattern = Pattern.compile("!\\[.*?\\]\\((https?://[^)]+\\.webp)\\)");
        Matcher matcher = pattern.matcher(changelog);
        
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        
        return lastMatch;
    }
    
    private static String getLoaderName(int loaderType) {
        return switch (loaderType) {
            case 1 -> "Forge";
            case 4 -> "Fabric";
            case 6 -> "NeoForge";
            default -> "Unknown";
        };
    }
}
