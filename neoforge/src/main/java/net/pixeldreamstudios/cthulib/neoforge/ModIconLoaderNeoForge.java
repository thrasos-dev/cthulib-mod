package net.pixeldreamstudios.cthulib.neoforge;

import net.neoforged.fml.ModList;
import net.pixeldreamstudios.cthulib.client.ModData;
import net.pixeldreamstudios.cthulib.client.ModIconLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModIconLoaderNeoForge {
    public static void init() {
        ModIconLoader.setImplementation(ModIconLoaderNeoForge::loadIcon);
    }

    private static void loadIcon(ModData mod, ModIconLoader.IconCallback callback) {
        ModList.get().getModContainerById(mod.modId).ifPresentOrElse(container -> {
            try {
                Path modRoot = container.getModInfo().getOwningFile().getFile().getFilePath();

                String logoPath = readLogoPathFromToml(modRoot, mod.modId);

                List<String> pathsToTry = new ArrayList<>();

                if (logoPath != null && ! logoPath.isEmpty()) {
                    pathsToTry.add(logoPath);

                    if (!logoPath.startsWith("assets/") && !logoPath.startsWith("/")) {
                        pathsToTry.add(logoPath);
                    }
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

                BufferedImage image = null;

                if (Files.isRegularFile(modRoot) && modRoot.toString().endsWith(".jar")) {
                    try (FileSystem fs = FileSystems.newFileSystem(modRoot, (ClassLoader) null)) {
                        for (String pathStr : pathsToTry) {
                            Path iconPath = fs.getPath(pathStr);
                            if (Files.exists(iconPath)) {
                                try (InputStream is = Files.newInputStream(iconPath)) {
                                    image = ImageIO.read(is);
                                    if (image != null) break;
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                } else {
                    for (String pathStr : pathsToTry) {
                        Path iconPath = modRoot.resolve(pathStr);
                        if (Files.exists(iconPath)) {
                            try (InputStream is = Files.newInputStream(iconPath)) {
                                image = ImageIO.read(is);
                                if (image != null) break;
                            } catch (Exception e) {
                            }
                        }
                    }
                }

                if (image != null) {
                    var rl = ModIconLoader.registerIcon(mod.modId, image);
                    if (rl != null) {
                        callback.onIconLoaded(rl);
                        return;
                    }
                }

                callback.onLoadFailed();
            } catch (Exception e) {
                callback.onLoadFailed();
            }
        }, callback::onLoadFailed);
    }

    private static String readLogoPathFromToml(Path modRoot, String modId) {
        try {
            InputStream tomlStream = null;

            if (Files.isRegularFile(modRoot) && modRoot.toString().endsWith(".jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(modRoot, (ClassLoader) null)) {
                    Path tomlPath = fs.getPath("META-INF/neoforge.mods.toml");
                    if (Files.exists(tomlPath)) {
                        tomlStream = Files.newInputStream(tomlPath);
                    }
                }
            } else {
                Path tomlPath = modRoot.resolve("META-INF/neoforge.mods.toml");
                if (Files.exists(tomlPath)) {
                    tomlStream = Files.newInputStream(tomlPath);
                }
            }

            if (tomlStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(tomlStream))) {
                    String line;
                    boolean inCorrectMod = false;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();

                        if (line.startsWith("modId")) {
                            String id = extractValue(line);
                            inCorrectMod = id.equals(modId);
                        }

                        if (inCorrectMod && line.startsWith("logoFile")) {
                            return extractValue(line);
                        }

                        if (line.equals("[[mods]]")) {
                            inCorrectMod = false;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }

        return null;
    }

    private static String extractValue(String line) {

        int equalsIndex = line.indexOf('=');
        if (equalsIndex == -1) return null;

        String value = line.substring(equalsIndex + 1).trim();

        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        } else if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }

        return value;
    }
}