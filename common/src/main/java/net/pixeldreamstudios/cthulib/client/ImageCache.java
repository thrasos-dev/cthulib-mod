package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final Map<String, ResourceLocation> textureCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> downloadingImages = new ConcurrentHashMap<>();
    
    public static ResourceLocation getOrLoadImage(String imageUrl, String cacheSubfolder, String texturePrefix) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        ResourceLocation cached = textureCache.get(imageUrl);
        if (cached != null) {
            return cached;
        }
        
        Boolean downloading = downloadingImages.get(imageUrl);
        if (downloading != null && downloading) {
            return null;
        }
        
        downloadingImages.put(imageUrl, true);
        Minecraft minecraft = Minecraft.getInstance();
        
        Util.ioPool().execute(() -> {
            try {
                File cacheDir = new File(minecraft.gameDirectory, "cthulib_cache/" + cacheSubfolder);
                cacheDir.mkdirs();
                
                String fileName = imageUrl.hashCode() + ".png";
                File cacheFile = new File(cacheDir, fileName);
                
                if (!cacheFile.exists()) {
                    downloadAndConvertImage(imageUrl, cacheFile);
                }
                
                if (cacheFile.exists()) {
                    BufferedImage image = ImageIO.read(cacheFile);
                    if (image != null) {
                        minecraft.execute(() -> {
                            NativeImage nativeImage = convertToNativeImage(image);
                            if (nativeImage != null) {
                                DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                                ResourceLocation location = minecraft.getTextureManager().register(
                                    texturePrefix + "_" + imageUrl.hashCode(), dynamicTexture);
                                textureCache.put(imageUrl, location);
                            }
                            downloadingImages.put(imageUrl, false);
                        });
                    } else {
                        downloadingImages.put(imageUrl, false);
                    }
                } else {
                    downloadingImages.put(imageUrl, false);
                }
            } catch (Exception e) {
                downloadingImages.put(imageUrl, false);
            }
        });
        
        return null;
    }
    
    private static void downloadAndConvertImage(String imageUrl, File cacheFile) {
        try {
            // Downloading image
            URI uri = new URI(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            if (connection.getResponseCode() == 200) {
                File tempFile = new File(cacheFile.getParentFile(), cacheFile.getName() + ".tmp");
                InputStream inputStream = connection.getInputStream();
                FileOutputStream fos = new FileOutputStream(tempFile);
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                
                fos.close();
                inputStream.close();
                
                BufferedImage bufferedImage = loadImage(tempFile, imageUrl);
                
                if (bufferedImage != null) {
                    FileOutputStream pngFos = new FileOutputStream(cacheFile);
                    ImageIO.write(bufferedImage, "png", pngFos);
                    pngFos.close();
                } else {
                }
                
                tempFile.delete();
            }
            connection.disconnect();
        } catch (Exception e) {
            // Download failed
        }
    }
    
    private static BufferedImage loadImage(File file, String imageUrl) {
        try {
            return ImageIO.read(file);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static NativeImage convertToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, true);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }
        
        return nativeImage;
    }
    
    public static void clearCache() {
        textureCache.clear();
        downloadingImages.clear();
    }
}
