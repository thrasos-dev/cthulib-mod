package net.pixeldreamstudios.cthulib.client;

import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModCollector {
    private static List<ModData> cachedMods = null;
    private static List<String> cachedAuthors = null;
    private static Supplier<List<ModData>> implementation = null;
    private static boolean isPreloading = false;

    public static void setImplementation(Supplier<List<ModData>> impl) {
        implementation = impl;
    }

    public static void preloadModData() {
        if (isPreloading || cachedMods != null) return;

        isPreloading = true;

        Thread preloadThread = new Thread(() -> {
            try {
                getModsByConfiguredAuthors();

                if (cachedMods != null) {
                    for (ModData mod : cachedMods) {
                        ModIconLoader.loadIcon(mod, new ModIconLoader.IconCallback() {
                            @Override
                            public void onIconLoaded(ResourceLocation icon) {

                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isPreloading = false;
            }
        }, "CthuLib-ModData-Preloader");

        preloadThread.setDaemon(true);
        preloadThread.start();
    }

    public static List<ModData> getModsByConfiguredAuthors() {
        if (implementation == null) {
            throw new IllegalStateException("ModCollector implementation not set!  Platform-specific collector must call setImplementation() during initialization.");
        }

        CthuLibConfig config = CthuLibConfig.get();

        if (cachedMods != null && cachedAuthors != null &&
                cachedAuthors.equals(config.authors)) {
            return new ArrayList<>(cachedMods);
        }

        List<ModData> mods = implementation.get();

        cachedMods = mods;
        cachedAuthors = new ArrayList<>(config.authors);

        return new ArrayList<>(mods);
    }

    public static void clearCache() {
        cachedMods = null;
        cachedAuthors = null;
        isPreloading = false;
    }

    public static boolean isDataReady() {
        return cachedMods != null && ! isPreloading;
    }
}