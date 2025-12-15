package net.pixeldreamstudios.cthulib.neoforge;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.pixeldreamstudios.cthulib.client.ModCollector;
import net.pixeldreamstudios.cthulib.client.ModData;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModCollectorNeoForge {
    public static void init() {
        ModCollector.setImplementation(ModCollectorNeoForge::collectMods);
    }

    private static List<ModData> collectMods() {
        CthuLibConfig config = CthuLibConfig.get();
        List<ModData> mods = new ArrayList<>();

        for (IModInfo modInfo : ModList.get().getMods()) {
            String authors = modInfo.getConfig()
                    .<String>getConfigElement("authors")
                    .orElse("");

            boolean hasMatchingAuthor = config.authors.stream()
                    .anyMatch(configAuthor -> authors.toLowerCase().contains(configAuthor.toLowerCase()));

            if (hasMatchingAuthor) {
                String author = authors.isEmpty() ? "Unknown" : authors;

                ModData modData = new ModData(
                        modInfo.getModId(),
                        modInfo.getDisplayName(),
                        modInfo.getDescription(),
                        modInfo.getVersion().toString(),
                        author
                );

                extractLinks(modInfo, modData);
                mods.add(modData);
            }
        }

        mods.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return mods;
    }

    private static void extractLinks(IModInfo modInfo, ModData modData) {

        Optional<String> displayUrl = modInfo.getConfig().getConfigElement("displayURL");
        displayUrl.ifPresent(url -> modData.links.put("homepage", url));

        Optional<String> issueUrl = modInfo.getConfig().getConfigElement("issueTrackerURL");
        issueUrl.ifPresent(url -> modData.links.put("issues", url));
    }
}