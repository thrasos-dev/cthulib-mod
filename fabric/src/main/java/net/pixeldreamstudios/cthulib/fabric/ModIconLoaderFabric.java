package net.pixeldreamstudios.cthulib.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.pixeldreamstudios.cthulib.client.ModData;
import net.pixeldreamstudios.cthulib.client.ModIconLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModIconLoaderFabric {
    public static void init() {
        ModIconLoader.setImplementation(ModIconLoaderFabric:: loadIcon);
    }

    private static void loadIcon(ModData mod, ModIconLoader.IconCallback callback) {
        FabricLoader.getInstance()
                .getModContainer(mod.modId)
                .flatMap(c -> c.getRootPaths().stream().findFirst())
                .ifPresentOrElse(root -> {

                    String iconPath = readIconPathFromJson(root);

                    List<String> pathsToTry = new ArrayList<>();

                    if (iconPath != null && !iconPath.isEmpty()) {
                        pathsToTry.add(iconPath);
                    }

                    pathsToTry.add("assets/" + mod.modId + "/icon.png");
                    pathsToTry.add("assets/" + mod.modId + "/logo.png");


                    if (mod.modId.contains("-")) {
                        String underscoreId = mod.modId.replace("-", "_");
                        pathsToTry.add("assets/" + underscoreId + "/icon.png");
                        pathsToTry.add("assets/" + underscoreId + "/logo.png");
                    } else if (mod.modId.contains("_")) {
                        String dashId = mod.modId.replace("_", "-");
                        pathsToTry.add("assets/" + dashId + "/icon.png");
                        pathsToTry.add("assets/" + dashId + "/logo.png");
                    }

                    pathsToTry.add("icon.png");
                    pathsToTry.add("logo.png");
                    pathsToTry.add("META-INF/icon.png");
                    pathsToTry.add("META-INF/logo.png");


                    for (String pathStr : pathsToTry) {
                        try {
                            Path iconFilePath = root.resolve(pathStr);
                            if (Files.exists(iconFilePath)) {
                                try (InputStream is = Files.newInputStream(iconFilePath)) {
                                    BufferedImage bi = ImageIO.read(is);
                                    if (bi != null) {
                                        var rl = ModIconLoader.registerIcon(mod.modId, bi);
                                        if (rl != null) {
                                            callback.onIconLoaded(rl);
                                            return;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {

                        }
                    }


                    callback.onLoadFailed();
                }, callback::onLoadFailed);
    }

    private static String readIconPathFromJson(Path root) {
        try {
            Path jsonPath = root.resolve("fabric.mod.json");
            if (Files.exists(jsonPath)) {
                try (InputStream is = Files.newInputStream(jsonPath);
                     InputStreamReader reader = new InputStreamReader(is)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                    if (json.has("icon")) {
                        return json.get("icon").getAsString();
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
    }
}