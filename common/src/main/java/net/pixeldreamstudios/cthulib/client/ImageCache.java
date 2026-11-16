package net.pixeldreamstudios.cthulib.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.util.AnimatedImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final Map<String, ResourceLocation> textureCache = new ConcurrentHashMap<>();
    private static final Set<String> loadingImages = ConcurrentHashMap.newKeySet();
    private static final Set<String> failedImages = ConcurrentHashMap.newKeySet();
    private static final String CACHE_ROOT = "cthulib_cache/";
    private static final String PNG_FORMAT = "png";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    public static ResourceLocation getOrLoadImage(String imageUrl, String cacheSubfolder, String texturePrefix) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        ResourceLocation cached = textureCache.get(imageUrl);
        if (cached != null) {
            return RawImageTextures.resolve(cached);
        }
        if (failedImages.contains(imageUrl) || !loadingImages.add(imageUrl)) {
            return null;
        }

        File cacheDir = cacheDir(cacheSubfolder);
        String key = texturePrefix + "_" + imageUrl;
        Util.ioPool().execute(() -> {
            try {
                Optional<File> file = prefetch(imageUrl, cacheDir, cacheName(imageUrl));
                if (file.isEmpty()) {
                    fail(imageUrl);
                    return;
                }
                boolean accepted = RawImageTextures.load(key, RawImageTextures.locationFor(key), Files.readAllBytes(file.get().toPath()), loaded -> {
                    loaded.ifPresentOrElse(result -> textureCache.put(imageUrl, result.location()), () -> failedImages.add(imageUrl));
                    loadingImages.remove(imageUrl);
                });
                if (!accepted) {
                    file.get().delete();
                }
            } catch (IOException | URISyntaxException | RuntimeException e) {
                fail(imageUrl);
            }
        });

        return null;
    }

    public static File cacheDir(String cacheSubfolder) {
        return new File(Minecraft.getInstance().gameDirectory, CACHE_ROOT + cacheSubfolder);
    }

    public static String cacheName(String imageUrl) {
        return String.valueOf(imageUrl.hashCode());
    }

    public static Optional<File> prefetch(String imageUrl, File cacheDir, String baseName) throws IOException, URISyntaxException {
        cacheDir.mkdirs();
        for (AnimatedImage.Format format : AnimatedImage.Format.values()) {
            File raw = new File(cacheDir, baseName + "." + format.extension);
            if (raw.exists()) {
                return Optional.of(raw);
            }
        }
        File png = new File(cacheDir, baseName + "." + PNG_FORMAT);
        if (png.exists() && !RawImageTextures.looksLikeGifUrl(imageUrl)) {
            return Optional.of(png);
        }

        HttpURLConnection connection = (HttpURLConnection) new URI(imageUrl).toURL().openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return Optional.empty();
            }
            byte[] bytes;
            try (InputStream in = connection.getInputStream()) {
                bytes = in.readAllBytes();
            }

            Optional<AnimatedImage.Format> rawFormat = AnimatedImage.rawCacheFormat(bytes);
            if (rawFormat.isPresent()) {
                File raw = new File(cacheDir, baseName + "." + rawFormat.get().extension);
                Files.write(raw.toPath(), bytes);
                png.delete();
                return Optional.of(raw);
            }

            ImageIO.scanForPlugins();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || !ImageIO.write(image, PNG_FORMAT, png)) {
                return Optional.empty();
            }
            return Optional.of(png);
        } finally {
            connection.disconnect();
        }
    }

    private static void fail(String imageUrl) {
        failedImages.add(imageUrl);
        loadingImages.remove(imageUrl);
    }

    public static void clearCache() {
        textureCache.clear();
        loadingImages.clear();
        failedImages.clear();
    }
}
