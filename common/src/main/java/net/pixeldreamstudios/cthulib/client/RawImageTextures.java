package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.CthuLib;
import net.pixeldreamstudios.cthulib.util.AnimatedImage;
import net.pixeldreamstudios.cthulib.util.CthuLibLogger;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class RawImageTextures {
    public record Loaded(ResourceLocation location, int width, int height, boolean animated) {
    }

    private enum ResourceState {
        LOADING,
        READY,
        FAILED
    }

    private static final Map<ResourceLocation, ResourceState> RESOURCE_STATES = new ConcurrentHashMap<>();
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static final Pattern GIF_URL = Pattern.compile("\\.gif($|[?#])", Pattern.CASE_INSENSITIVE);
    private static final String GENERATED_PATH_PREFIX = "raw/";
    private static final int NO_SIZE_LIMIT = 0;

    private RawImageTextures() {
    }

    public static boolean isRawImagePath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        for (AnimatedImage.Format format : AnimatedImage.Format.values()) {
            if (lower.endsWith("." + format.extension)) {
                return true;
            }
        }
        return false;
    }

    public static boolean looksLikeGifUrl(String url) {
        return GIF_URL.matcher(url).find();
    }

    public static ResourceLocation locationFor(String key) {
        return ResourceLocation.tryBuild(CthuLib.MOD_ID, GENERATED_PATH_PREFIX + Integer.toHexString(key.hashCode()));
    }

    public static ResourceLocation resolve(ResourceLocation texture) {
        if (texture == null) {
            return null;
        }
        Optional<AnimatedTexture> animated = AnimatedTexture.find(texture);
        if (animated.isPresent()) {
            return animated.get().tick();
        }
        if (!isRawImagePath(texture.getPath())) {
            return texture;
        }
        ResourceState state = RESOURCE_STATES.putIfAbsent(texture, ResourceState.LOADING);
        if (state == null) {
            loadResource(texture);
            return null;
        }
        return switch (state) {
            case LOADING -> null;
            case READY -> texture;
            case FAILED -> MissingTextureAtlasSprite.getLocation();
        };
    }

    public static void onResourceReload() {
        GENERATION.incrementAndGet();
        RESOURCE_STATES.keySet().forEach(AnimatedTexture::forget);
        RESOURCE_STATES.clear();
    }

    public static void release(ResourceLocation location) {
        if (location == null) {
            return;
        }
        AnimatedTexture.forget(location);
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        AbstractTexture texture = textureManager.getTexture(location, null);
        if (texture != null) {
            textureManager.release(location);
            texture.close();
        }
    }

    public static boolean load(String key, ResourceLocation location, byte[] bytes, Consumer<Optional<Loaded>> onRenderThread) {
        return load(key, location, bytes, NO_SIZE_LIMIT, onRenderThread);
    }

    public static boolean load(String key, ResourceLocation location, byte[] bytes, int maxStillSide, Consumer<Optional<Loaded>> onRenderThread) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            ImageIO.scanForPlugins();
            Optional<AnimatedImage> animated = AnimatedImage.read(bytes);
            if (animated.isPresent()) {
                AnimatedImage image = animated.get();
                int[] firstFrame = image.decodeFrame(0);
                minecraft.execute(() -> {
                    AnimatedTexture registered = AnimatedTexture.register(key, location, image, firstFrame);
                    onRenderThread.accept(Optional.of(new Loaded(registered.location(), registered.width(), registered.height(), true)));
                });
                return true;
            }

            BufferedImage still = ImageIO.read(new ByteArrayInputStream(bytes));
            if (still != null) {
                BufferedImage sized = limitSize(still, maxStillSide);
                NativeImage nativeImage = toNativeImage(sized);
                minecraft.execute(() -> {
                    AnimatedTexture.forget(location);
                    minecraft.getTextureManager().register(location, new DynamicTexture(nativeImage));
                    onRenderThread.accept(Optional.of(new Loaded(location, sized.getWidth(), sized.getHeight(), false)));
                });
                return true;
            }
        } catch (IOException | RuntimeException e) {
            CthuLibLogger.error("Could not load image " + key + ": " + e.getMessage());
        }
        minecraft.execute(() -> onRenderThread.accept(Optional.empty()));
        return false;
    }

    public static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                nativeImage.setPixelRGBA(x, y, argbToAbgr(image.getRGB(x, y)));
            }
        }
        return nativeImage;
    }

    public static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00) | (argb & 0xFF) << 16 | (argb >>> 16 & 0xFF);
    }

    private static BufferedImage limitSize(BufferedImage image, int maxSide) {
        if (maxSide <= NO_SIZE_LIMIT || (image.getWidth() <= maxSide && image.getHeight() <= maxSide)) {
            return image;
        }
        float scale = Math.min((float) maxSide / image.getWidth(), (float) maxSide / image.getHeight());
        int width = Math.max(1, (int) (image.getWidth() * scale));
        int height = Math.max(1, (int) (image.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private static void loadResource(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        int generation = GENERATION.get();
        Util.ioPool().execute(() -> {
            try (InputStream in = minecraft.getResourceManager().open(texture)) {
                load(texture.toString(), texture, in.readAllBytes(), loaded -> {
                    if (GENERATION.get() != generation) {
                        AnimatedTexture.forget(texture);
                        return;
                    }
                    RESOURCE_STATES.put(texture, loaded.isPresent() ? ResourceState.READY : ResourceState.FAILED);
                });
            } catch (IOException e) {
                CthuLibLogger.error("Could not read texture " + texture + ": " + e.getMessage());
                if (GENERATION.get() == generation) {
                    RESOURCE_STATES.put(texture, ResourceState.FAILED);
                }
            }
        });
    }
}
