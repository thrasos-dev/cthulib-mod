package net.pixeldreamstudios.cthulib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class CthuLibConfig {
    public boolean showInTitleScreen = true;
    public boolean showInSettings = true;
    public List<String> authors = new ArrayList<>();
    public String buttonLogo = "cthulib:textures/gui/button_normal.png";
    public String buttonTooltip = "Pixel Dream Studios";
    public int buttonXtitleScreen = 0;
    public int buttonYtitleScreen = 0;
    public int buttonXsettings = 0;
    public int buttonYsettings = 0;
    public boolean showName = true;
    public String name = "";
    public String logoPath = "cthulib:textures/gui/pdslogo.png";
    public String backgroundPath = "cthulib:textures/gui/pdsbg.png";
    public String wikiLink = "https://pixeldreamstudios.net";
    public String wikiLogo = "cthulib:textures/gui/button_normal.png";
    public int wikiButtonX = 125;
    public int wikiButtonY = 0;
    public String discordLink = "https://discord.pixeldreamstudios.net/";
    public String discordLogo = "cthulib:textures/gui/button_christmas.png";
    public int discordButtonX = -125;
    public int discordButtonY = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "config/cthulib/cthulib.json");
    private static CthuLibConfig INSTANCE;

    public static CthuLibConfig load() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        CthuLibConfig defaultConfig = createDefault();

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject existingJson = JsonParser.parseReader(reader).getAsJsonObject();
                INSTANCE = GSON.fromJson(existingJson, CthuLibConfig.class);

                boolean needsSave = false;

                if (INSTANCE.authors == null || INSTANCE.authors.isEmpty()) {
                    INSTANCE.authors = defaultConfig.authors;
                    needsSave = true;
                }

                if (INSTANCE.buttonLogo == null || INSTANCE.buttonLogo.isEmpty()) {
                    INSTANCE.buttonLogo = defaultConfig.buttonLogo;
                    needsSave = true;
                }

                if (INSTANCE.buttonTooltip == null || INSTANCE.buttonTooltip.isEmpty()) {
                    INSTANCE.buttonTooltip = defaultConfig.buttonTooltip;
                    needsSave = true;
                }

                String[] stringFields = {"wikiLink", "wikiLogo", "discordLink", "discordLogo", "logoPath", "name", "backgroundPath", "buttonTooltip"};
                for (String field :  stringFields) {
                    if (! existingJson.has(field)) {
                        needsSave = true;
                    }
                }

                String[] intFields = {"buttonXtitleScreen", "buttonYtitleScreen", "buttonXsettings", "buttonYsettings",
                        "wikiButtonX", "wikiButtonY", "discordButtonX", "discordButtonY"};
                for (String field : intFields) {
                    if (! existingJson.has(field)) {
                        needsSave = true;
                    }
                }

                if (needsSave) {
                    INSTANCE.save();
                }

                return INSTANCE;
            } catch (Exception e) {
                e.printStackTrace();
                INSTANCE = defaultConfig;
                INSTANCE.save();
                return INSTANCE;
            }
        }

        INSTANCE = defaultConfig;
        INSTANCE.save();
        return INSTANCE;
    }

    private static CthuLibConfig createDefault() {
        CthuLibConfig config = new CthuLibConfig();
        config.showInTitleScreen = true;
        config.showInSettings = true;
        config.authors.add("KevzCz");
        config.authors.add("lwkysad");
        config.authors.add("steficy");
        config.authors.add("Bandit-bytes");
        config.authors.add("VeroxUniverse");
        config.authors.add("Rebel459");
        config.authors.add("Thrasos");
        config.authors.add("noodlescript");
        config.authors.add("alshanex");
        config.authors.add("starrysock");
        config.authors.add("aleganza");
        config.authors.add("Pixel Dream Studios");
        config.authors.add("kyber-6");
        config.buttonLogo = "cthulib:textures/gui/button_normal.png";
        config.buttonTooltip = "Pixel Dream Studios";
        config.buttonXtitleScreen = 0;
        config.buttonYtitleScreen = 0;
        config.buttonXsettings = 0;
        config.buttonYsettings = 0;
        config.showName = true;
        config.name = "";
        config.logoPath = "cthulib:textures/gui/pdslogo.png";
        config.backgroundPath = "cthulib:textures/gui/pdsbg.png";
        config.wikiLink = "https://pixeldreamstudios.net";
        config.wikiLogo = "cthulib:textures/gui/button_web.png";
        config.wikiButtonX = 125;
        config.wikiButtonY = 0;
        config.discordLink = "https://discord.pixeldreamstudios.net/";
        config.discordLogo = "cthulib:textures/gui/button_discord.png";
        config.discordButtonX = -125;
        config.discordButtonY = 0;
        return config;
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CthuLibConfig get() {
        if (INSTANCE == null) {
            return load();
        }
        return INSTANCE;
    }
}