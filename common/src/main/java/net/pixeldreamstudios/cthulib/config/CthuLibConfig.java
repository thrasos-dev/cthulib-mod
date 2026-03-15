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
    public boolean resetResourcePacks = false;
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
    public String brightnessButtonTexture = "cthulib:textures/gui/button_normal.png";
    public String wikiLink = "https://pixeldreamstudios.net";
    public String wikiLogo = "cthulib:textures/gui/button_normal.png";
    public int wikiButtonX = 125;
    public int wikiButtonY = 0;
    public String discordLink = "https://discord.pixeldreamstudios.net/";
    public String discordLogo = "cthulib:textures/gui/button_discord.png";
    public int discordButtonX = -125;
    public int discordButtonY = 0;
    public boolean showPromoButton = false;
    public String promoButtonTexture = "cthulib:textures/gui/promo_button.png";
    public String promoButtonTooltip = "Special Offer!";
    public String promoButtonUrl = "https://example.com";
    public int promoButtonX = 0;
    public int promoButtonY = -50;
    public int promoButtonWidth = 100;
    public int promoButtonHeight = 25;    public float promoButtonScale = 1.0f;    public boolean promoButtonScaleWithScreen = false;
    public float promoButtonMinScale = 0.5f;
    public float promoButtonMaxScale = 1.5f;
    public boolean promoButtonStartFromCenterX = true;
    public boolean promoButtonStartFromCenterY = false;
    public boolean promoButtonStartFromLeftX = false;
    public boolean promoButtonStartFromRightX = false;

    public boolean showProjectSlider = true;
    public String sliderHeaderText = "Check our other projects!";
    public int sliderX = -260;
    public int sliderY = -140;
    public int sliderCardWidth = 240;
    public int sliderCardHeight = 64;
    public boolean sliderAutoSlide = true;
    public float sliderAutoSlideDelay = 5.0f;
    public boolean sliderShowPreviews = true;
    public float sliderPreviewScale = 0.7f;
    public int sliderPreviewOffset = 15;
    public float sliderScale = 1.0f;
    public boolean sliderStartFromCenterX = false;
    public boolean sliderStartFromCenterY = false;
    public boolean sliderStartFromLeftX = false;
    public boolean sliderStartFromRightX = false;
    public boolean sliderScaleWithScreen = false;
    public float sliderMinScale = 0.5f;
    public float sliderMaxScale = 1.5f;
    public boolean sliderShowFilterButton = true;
    public int sliderFilterOffsetX = 0;
    public int sliderFilterOffsetY = 0;

    public boolean showChangelogButton = true;
    public String changelogButtonTexture = "";
    public int changelogButtonX = -130;
    public int changelogButtonY = -35;
    public int changelogButtonWidth = 120;
    public int changelogButtonHeight = 24;
    public float changelogButtonScale = 1.0f;
    public boolean changelogButtonScaleWithScreen = false;
    public float changelogButtonMinScale = 0.5f;
    public float changelogButtonMaxScale = 1.5f;
    public boolean changelogButtonStartFromCenterX = true;
    public boolean changelogButtonStartFromCenterY = false;
    public boolean changelogButtonStartFromLeftX = false;
    public boolean changelogButtonStartFromRightX = false;
    
    public int changelogPanelX = -1;
    public int changelogPanelY = -1;
    public int changelogPanelWidth = 600;
    public int changelogPanelHeight = 400;
    public String changelogPanelTexture = "";
    public boolean showChangelogNotification = true;
    public String lastReadChangelogVersion = "";
    public int devModeKey1 = 80; // P key (GLFW_KEY_P)
    public int devModeKey2 = 68; // D key (GLFW_KEY_D)
    public int devModeKey3 = 83; // S key (GLFW_KEY_S)
    public boolean animationsEnabled = true;
    public float hoverAnimationSpeed = 0.15f;
    public float pressAnimationSpeed = 0.3f;
    public float pulseAnimationSpeed = 0.04f;
    public boolean shineEffectEnabled = true;
    public float shineAnimationSpeed = 1.0f;
    public float sliderEntryAnimationSpeed = 0.22f;
    public float sliderFadeInSpeed = 0.25f;
    public float sliderCardTransitionSpeed = 0.05f;
    public boolean sliderEntryAnimationEnabled = true;
    public boolean sliderCardTransitionEnabled = true;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "config/cthulib/cthulib.json");
    private static CthuLibConfig INSTANCE;
    private static boolean needsCacheInvalidation = false;

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

                String[] stringFields = {"wikiLink", "wikiLogo", "discordLink", "discordLogo", "logoPath", "name", "backgroundPath", "buttonTooltip", 
                        "promoButtonTexture", "promoButtonTooltip", "promoButtonUrl", "changelogButtonTexture", "brightnessButtonTexture"};
                for (String field :  stringFields) {
                    if (! existingJson.has(field)) {
                        needsSave = true;
                    }
                }

                String[] intFields = {"buttonXtitleScreen", "buttonYtitleScreen", "buttonXsettings", "buttonYsettings",
                        "wikiButtonX", "wikiButtonY", "discordButtonX", "discordButtonY",
                        "promoButtonX", "promoButtonY", "promoButtonWidth", "promoButtonHeight",
                        "sliderX", "sliderY", "sliderCardWidth", "sliderCardHeight", "sliderPreviewOffset",
                        "sliderFilterOffsetX", "sliderFilterOffsetY",
                        "changelogButtonX", "changelogButtonY", "changelogButtonWidth", "changelogButtonHeight"};
                for (String field : intFields) {
                    if (! existingJson.has(field)) {
                        needsSave = true;
                    }
                }
                
                String[] boolFields = {"promoButtonScaleWithScreen", "promoButtonStartFromCenterX", "promoButtonStartFromCenterY", "promoButtonStartFromLeftX", "promoButtonStartFromRightX",
                        "showProjectSlider", "sliderAutoSlide", "sliderShowPreviews", "sliderStartFromCenterX", "sliderStartFromCenterY", "sliderStartFromLeftX", "sliderStartFromRightX", "sliderScaleWithScreen",
                        "sliderShowFilterButton", "sliderEntryAnimationEnabled", "sliderCardTransitionEnabled",
                        "showChangelogButton", "changelogButtonScaleWithScreen", "changelogButtonStartFromCenterX", "changelogButtonStartFromCenterY", "changelogButtonStartFromLeftX", "changelogButtonStartFromRightX",
                        "animationsEnabled", "shineEffectEnabled"};
                for (String field : boolFields) {
                    if (! existingJson.has(field)) {
                        needsSave = true;
                    }
                }
                
                String[] floatFields = {"promoButtonMinScale", "promoButtonMaxScale", "sliderAutoSlideDelay", "sliderPreviewScale", "sliderScale", "sliderMinScale", "sliderMaxScale",
                        "sliderEntryAnimationSpeed", "sliderFadeInSpeed", "sliderCardTransitionSpeed",
                        "changelogButtonMinScale", "changelogButtonMaxScale",
                        "hoverAnimationSpeed", "pressAnimationSpeed", "pulseAnimationSpeed", "shineAnimationSpeed",
                        "promoButtonScale", "changelogButtonScale"};
                for (String field : floatFields) {
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

    public static CthuLibConfig getInstance() {
        if (INSTANCE == null) {
            return load();
        }
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
        config.buttonXtitleScreen = 584;
        config.buttonYtitleScreen = 236;
        config.buttonXsettings = -1;
        config.buttonYsettings = 1;
        config.showName = true;
        config.name = "";
        config.logoPath = "cthulib:textures/gui/pdslogo.png";
        config.backgroundPath = "cthulib:textures/gui/pdsbg.png";
        config.brightnessButtonTexture = "cthulib:textures/gui/button_normal.png";
        config.wikiLink = "https://pixeldreamstudios.net";
        config.wikiLogo = "cthulib:textures/gui/button_web.png";
        config.wikiButtonX = 125;
        config.wikiButtonY = 0;
        config.discordLink = "https://discord.pixeldreamstudios.net/";
        config.discordLogo = "cthulib:textures/gui/button_discord.png";
        config.discordButtonX = -125;
        config.discordButtonY = 0;
        
        config.showPromoButton = true;
        config.promoButtonTexture = "cthulib:textures/gui/promo_button.png";
        config.promoButtonTooltip = "Need a Server?";
        config.promoButtonUrl = "https://bisecthosting.com/PixelDream";
        config.promoButtonX = 320;
        config.promoButtonY = -227;
        config.promoButtonWidth = 320;
        config.promoButtonHeight = 50;
        config.promoButtonScale = 1.0f;
        config.promoButtonScaleWithScreen = false;
        config.promoButtonMinScale = 0.8625001f;
        config.promoButtonMaxScale = 1.5f;
        config.promoButtonStartFromCenterX = false;
        config.promoButtonStartFromCenterY = false;
        config.promoButtonStartFromLeftX = false;
        config.promoButtonStartFromRightX = false;

        config.showProjectSlider = true;
        config.sliderHeaderText = " ";
        config.sliderX = -46;
        config.sliderY = -186;
        config.sliderCardWidth = 225;
        config.sliderCardHeight = 64;
        config.sliderAutoSlide = true;
        config.sliderAutoSlideDelay = 5.0f;
        config.sliderShowPreviews = true;
        config.sliderPreviewScale = 0.7f;
        config.sliderPreviewOffset = 54;
        config.sliderScale = 0.7506466f;
        config.sliderStartFromCenterX = true;
        config.sliderStartFromCenterY = false;
        config.sliderStartFromLeftX = false;
        config.sliderStartFromRightX = false;
        config.sliderScaleWithScreen = false;
        config.sliderMinScale = 0.5f;
        config.sliderMaxScale = 1.5f;
        config.sliderShowFilterButton = true;
        config.sliderFilterOffsetX = 58;
        config.sliderFilterOffsetY = 0;

        config.showChangelogButton = true;
        config.changelogButtonTexture = "";
        config.changelogButtonX = -54;
        config.changelogButtonY = -176;
        config.changelogButtonWidth = 62;
        config.changelogButtonHeight = 14;
        config.changelogButtonScale = 1.0f;
        config.changelogButtonScaleWithScreen = true;
        config.changelogButtonMinScale = 0.5f;
        config.changelogButtonMaxScale = 1.5f;
        config.changelogButtonStartFromCenterX = true;
        config.changelogButtonStartFromCenterY = false;
        config.changelogButtonStartFromLeftX = false;
        config.changelogButtonStartFromRightX = false;
        
        config.changelogPanelX = -1;
        config.changelogPanelY = -1;
        config.changelogPanelWidth = 600;
        config.changelogPanelHeight = 400;
        config.changelogPanelTexture = "";
        config.showChangelogNotification = true;
        config.lastReadChangelogVersion = "";

        config.devModeKey1 = 80; // P key
        config.devModeKey2 = 68; // D key
        config.devModeKey3 = 83; // S key

        config.animationsEnabled = true;
        config.hoverAnimationSpeed = 0.15f;
        config.pressAnimationSpeed = 0.3f;
        config.pulseAnimationSpeed = 0.04f;
        config.shineEffectEnabled = true;
        config.shineAnimationSpeed = 1.0f;
        config.sliderEntryAnimationSpeed = 0.22f;
        config.sliderFadeInSpeed = 0.25f;
        config.sliderCardTransitionSpeed = 0.05f;
        config.sliderEntryAnimationEnabled = true;
        config.sliderCardTransitionEnabled = true;

        return config;
    }

    public void resetGeneralSettings() {
        CthuLibConfig defaults = createDefault();
        this.showInTitleScreen = defaults.showInTitleScreen;
        this.showInSettings = defaults.showInSettings;
        this.showName = defaults.showName;
        this.logoPath = defaults.logoPath;
        this.backgroundPath = defaults.backgroundPath;
        this.brightnessButtonTexture = defaults.brightnessButtonTexture;
        this.wikiLogo = defaults.wikiLogo;
        this.discordLogo = defaults.discordLogo;
    }
    
    public void resetPromoButton() {
        CthuLibConfig defaults = createDefault();
        this.showPromoButton = defaults.showPromoButton;
        this.promoButtonTexture = defaults.promoButtonTexture;
        this.promoButtonTooltip = defaults.promoButtonTooltip;
        this.promoButtonUrl = defaults.promoButtonUrl;
        this.promoButtonX = defaults.promoButtonX;
        this.promoButtonY = defaults.promoButtonY;
        this.promoButtonWidth = defaults.promoButtonWidth;
        this.promoButtonHeight = defaults.promoButtonHeight;
        this.promoButtonScaleWithScreen = defaults.promoButtonScaleWithScreen;
        this.promoButtonMinScale = defaults.promoButtonMinScale;
        this.promoButtonMaxScale = defaults.promoButtonMaxScale;
        this.promoButtonStartFromCenterX = defaults.promoButtonStartFromCenterX;
        this.promoButtonStartFromCenterY = defaults.promoButtonStartFromCenterY;
        this.promoButtonStartFromLeftX = defaults.promoButtonStartFromLeftX;
        this.promoButtonStartFromRightX = defaults.promoButtonStartFromRightX;
    }
    
    public void resetProjectSlider() {
        CthuLibConfig defaults = createDefault();
        this.sliderHeaderText = defaults.sliderHeaderText;
        this.showProjectSlider = defaults.showProjectSlider;
        this.sliderX = defaults.sliderX;
        this.sliderY = defaults.sliderY;
        this.sliderCardWidth = defaults.sliderCardWidth;
        this.sliderCardHeight = defaults.sliderCardHeight;
        this.sliderStartFromCenterY = defaults.sliderStartFromCenterY;
        this.sliderStartFromLeftX = defaults.sliderStartFromLeftX;
        this.sliderStartFromRightX = defaults.sliderStartFromRightX;
        this.sliderScaleWithScreen = defaults.sliderScaleWithScreen;
        this.sliderMinScale = defaults.sliderMinScale;
        this.sliderMaxScale = defaults.sliderMaxScale;
    }
    
    public void resetChangelog() {
        CthuLibConfig defaults = createDefault();
        this.showChangelogButton = defaults.showChangelogButton;
        this.changelogButtonTexture = defaults.changelogButtonTexture;
        this.changelogButtonX = defaults.changelogButtonX;
        this.changelogButtonY = defaults.changelogButtonY;
        this.changelogButtonWidth = defaults.changelogButtonWidth;
        this.changelogButtonHeight = defaults.changelogButtonHeight;
        this.changelogButtonScaleWithScreen = defaults.changelogButtonScaleWithScreen;
        this.changelogButtonMinScale = defaults.changelogButtonMinScale;
        this.changelogButtonMaxScale = defaults.changelogButtonMaxScale;
        this.changelogButtonStartFromCenterX = defaults.changelogButtonStartFromCenterX;
        this.changelogButtonStartFromCenterY = defaults.changelogButtonStartFromCenterY;
        this.changelogButtonStartFromLeftX = defaults.changelogButtonStartFromLeftX;
        this.changelogButtonStartFromRightX = defaults.changelogButtonStartFromRightX;
        this.changelogPanelWidth = defaults.changelogPanelWidth;
        this.changelogPanelHeight = defaults.changelogPanelHeight;
        this.changelogPanelTexture = defaults.changelogPanelTexture;
        this.showChangelogNotification = defaults.showChangelogNotification;
        this.lastReadChangelogVersion = defaults.lastReadChangelogVersion;
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
            needsCacheInvalidation = true;
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
    
    public static boolean needsCacheInvalidation() {
        return needsCacheInvalidation;
    }
    
    public static void clearCacheInvalidationFlag() {
        needsCacheInvalidation = false;
    }
    
    public static void markForCacheInvalidation() {
        needsCacheInvalidation = true;
    }
}