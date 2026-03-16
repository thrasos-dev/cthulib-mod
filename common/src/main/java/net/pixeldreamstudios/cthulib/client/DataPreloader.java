package net.pixeldreamstudios.cthulib.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.pixeldreamstudios.cthulib.util.ChangelogCache;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

public class DataPreloader {
    private static boolean hasPreloaded = false;

    public static void preloadData() {
        if (hasPreloaded) {
            return;
        }
        hasPreloaded = true;

        Util.ioPool().execute(() -> {
            try {
                System.out.println("[CthuLib] Preloading project data...");
                List<ProjectData> projects = ProjectData.fetchAndCacheProjects();
                
                if (projects != null && !projects.isEmpty()) {
                    System.out.println("[CthuLib] Preloaded " + projects.size() + " projects");
                    
                    for (ProjectData project : projects) {
                        String iconUrl = project.getIconUrl();
                        if (iconUrl != null && !iconUrl.isEmpty()) {
                            preloadImageToCache(iconUrl, "logos");
                        }
                    }
                }
                
                String projectId = ChangelogCache.getProjectId();
                String version = ChangelogCache.getCurrentVersion();
                
                if (projectId != null && version != null) {
                    System.out.println("[CthuLib] Preloading changelog for project " + projectId + " v" + version);
                    ChangelogCache.getChangelog(projectId, version);
                }
                
                String promoCardUrl = ChangelogCache.getPromoCardUrl();
                if (promoCardUrl != null && !promoCardUrl.isEmpty()) {
                    preloadImageToCache(promoCardUrl, "promo_buttons");
                }
                
                System.out.println("[CthuLib] Data preloading complete");
            } catch (Exception e) {
                System.err.println("[CthuLib] Error during data preloading: " + e.getMessage());
            }
        });
    }

    private static void preloadImageToCache(String imageUrl, String subfolder) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            File cacheDir = new File(minecraft.gameDirectory, "cthulib_cache/" + subfolder);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            String fileName = imageUrl.hashCode() + ".png";
            File cacheFile = new File(cacheDir, fileName);
            if (!cacheFile.exists()) {
                URI uri = new URI(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                if (connection.getResponseCode() == 200) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedImage image = ImageIO.read(inputStream);
                    inputStream.close();
                    if (image != null) {
                        ImageIO.write(image, "png", cacheFile);
                    }
                }
                connection.disconnect();
            }
        } catch (Exception e) {
            // Failed silently
        }
    }
}
