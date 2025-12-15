package net.pixeldreamstudios.cthulib.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.pixeldreamstudios.cthulib.client.ModCollector;
import net.pixeldreamstudios.cthulib.client.ModData;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModCollectorFabric {
    public static void init() {
        ModCollector.setImplementation(ModCollectorFabric::collectMods);
    }

    private static List<ModData> collectMods() {
        CthuLibConfig config = CthuLibConfig.get();
        List<ModData> mods = new ArrayList<>();

        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            ModMetadata metadata = container.getMetadata();

            boolean hasMatchingAuthor = metadata.getAuthors().stream()
                    .map(Person::getName)
                    .anyMatch(author -> config.authors.stream()
                            .anyMatch(configAuthor -> configAuthor.equalsIgnoreCase(author)));

            if (hasMatchingAuthor) {
                String author = metadata.getAuthors().isEmpty() ? "Unknown" :
                        metadata.getAuthors().iterator().next().getName();

                ModData modData = new ModData(
                        metadata.getId(),
                        metadata.getName(),
                        metadata.getDescription(),
                        metadata.getVersion().getFriendlyString(),
                        author
                );

                extractLinks(container, modData);
                mods.add(modData);
            }
        }

        mods.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return mods;
    }

    private static void extractLinks(ModContainer container, ModData modData) {
        container.getRootPaths().stream().findFirst().ifPresent(root -> {
            try {
                Path fabricJson = root.resolve("fabric.mod.json");
                if (Files.exists(fabricJson)) {
                    try (InputStream is = Files.newInputStream(fabricJson)) {
                        JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();

                        if (json.has("contact")) {
                            JsonObject contact = json.getAsJsonObject("contact");
                            if (contact.has("homepage")) {
                                modData.links.put("homepage", contact.get("homepage").getAsString());
                            }
                            if (contact.has("sources")) {
                                modData.links.put("sources", contact.get("sources").getAsString());
                            }
                            if (contact.has("issues")) {
                                modData.links.put("issues", contact.get("issues").getAsString());
                            }
                        }
                    }
                }
            } catch (Exception e) {

            }
        });
    }
}