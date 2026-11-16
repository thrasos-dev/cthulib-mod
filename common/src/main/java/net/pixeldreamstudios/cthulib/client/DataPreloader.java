package net.pixeldreamstudios.cthulib.client;

import net.minecraft.Util;
import net.pixeldreamstudios.cthulib.util.ChangelogCache;
import net.pixeldreamstudios.cthulib.util.UpdateAnnouncement;

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
                    UpdateAnnouncement.check(projectId, version);
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
            ImageCache.prefetch(imageUrl, ImageCache.cacheDir(subfolder), ImageCache.cacheName(imageUrl));
        } catch (Exception e) {
            System.err.println("[CthuLib] Could not preload image " + imageUrl + ": " + e.getMessage());
        }
    }
}
