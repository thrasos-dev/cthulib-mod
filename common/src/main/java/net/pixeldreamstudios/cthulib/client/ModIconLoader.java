package net.pixeldreamstudios.cthulib.client;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class ModIconLoader {
    private static final String ICON_KEY_PREFIX = "mod_icon_";
    private static BiConsumer<ModData, IconCallback> implementation = null;

    public static void setImplementation(BiConsumer<ModData, IconCallback> impl) {
        implementation = impl;
    }

    public static void loadIcon(ModData mod, IconCallback callback) {
        if (implementation == null) {
            callback.onLoadFailed();
            return;
        }
        implementation.accept(mod, callback);
    }

    public static boolean tryRegisterIcon(String modId, Path base, String relativePath, IconCallback callback) {
        try {
            Path path = base.resolve(relativePath);
            if (!Files.exists(path)) {
                return false;
            }
            String key = ICON_KEY_PREFIX + modId;
            return RawImageTextures.load(key, RawImageTextures.locationFor(key), Files.readAllBytes(path),
                    loaded -> loaded.ifPresent(result -> callback.onIconLoaded(result.location())));
        } catch (IOException | InvalidPathException e) {
            return false;
        }
    }

    @FunctionalInterface
    public interface IconCallback {
        void onIconLoaded(ResourceLocation icon);

        default void onLoadFailed() {
        }
    }
}
