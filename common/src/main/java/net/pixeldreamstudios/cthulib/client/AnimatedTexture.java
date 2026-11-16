package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.util.AnimatedImage;
import net.pixeldreamstudios.cthulib.util.AnimationPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimatedTexture {
    private static final Map<String, AnimatedTexture> BY_KEY = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, AnimatedTexture> BY_LOCATION = new ConcurrentHashMap<>();

    private final String key;
    private final ResourceLocation location;
    private final AnimationPlayer player;
    private final DynamicTexture texture;

    private AnimatedTexture(String key, ResourceLocation location, AnimationPlayer player, DynamicTexture texture) {
        this.key = key;
        this.location = location;
        this.player = player;
        this.texture = texture;
    }

    public static Optional<AnimatedTexture> find(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    public static Optional<AnimatedTexture> find(ResourceLocation location) {
        return Optional.ofNullable(BY_LOCATION.get(location));
    }

    public static AnimatedTexture register(String key, ResourceLocation location, AnimatedImage image, int[] firstFrame) {
        AnimatedTexture existing = BY_KEY.get(key);
        if (existing != null && existing.location.equals(location)) {
            return existing;
        }
        if (existing != null) {
            forget(existing.location);
        }
        forget(location);
        DynamicTexture texture = new DynamicTexture(new NativeImage(image.width(), image.height(), true));
        Minecraft.getInstance().getTextureManager().register(location, texture);
        AnimatedTexture animated = new AnimatedTexture(key, location, new AnimationPlayer(image, firstFrame, Util.backgroundExecutor()), texture);
        BY_KEY.put(key, animated);
        BY_LOCATION.put(location, animated);
        animated.tick();
        return animated;
    }

    public static void forget(ResourceLocation location) {
        AnimatedTexture removed = BY_LOCATION.remove(location);
        if (removed != null) {
            BY_KEY.remove(removed.key, removed);
        }
    }

    public ResourceLocation location() {
        return location;
    }

    public int width() {
        return player.width();
    }

    public int height() {
        return player.height();
    }

    public ResourceLocation tick() {
        player.update(Util.getMillis()).ifPresent(this::upload);
        return location;
    }

    private void upload(AnimationPlayer.Region region) {
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return;
        }
        int[] canvas = player.canvas();
        int canvasWidth = player.width();
        for (int y = region.y(); y < region.y() + region.height(); y++) {
            int row = y * canvasWidth;
            for (int x = region.x(); x < region.x() + region.width(); x++) {
                pixels.setPixelRGBA(x, y, RawImageTextures.argbToAbgr(canvas[row + x]));
            }
        }
        texture.bind();
        pixels.upload(0, region.x(), region.y(), region.x(), region.y(), region.width(), region.height(), false, false);
    }
}
