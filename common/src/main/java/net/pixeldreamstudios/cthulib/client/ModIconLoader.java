package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

public class ModIconLoader {
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
    public static ResourceLocation registerIcon(String modId, BufferedImage img) {
        try {
            com.mojang.blaze3d.platform.NativeImage ni =
                    new com.mojang.blaze3d.platform.NativeImage(img.getWidth(), img.getHeight(), true);

            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int rgb = img.getRGB(x, y);
                    ni.setPixelRGBA(x, y,
                            (rgb & 0xFF000000) |
                                    ((rgb & 0xFF) << 16) |
                                    (rgb & 0xFF00) |
                                    ((rgb & 0xFF0000) >> 16));
                }
            }

            DynamicTexture dt = new DynamicTexture(ni);
            return Minecraft.getInstance().getTextureManager()
                    .register("cthulib_mod_" + modId, dt);
        } catch (Exception e) {
            return null;
        }
    }

    @FunctionalInterface
    public interface IconCallback {
        void onIconLoaded(ResourceLocation icon);

        default void onLoadFailed() {
        }
    }
}