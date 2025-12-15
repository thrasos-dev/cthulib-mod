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

public class PromoMessageConfig {
    public List<ServerPromo> serverPromos = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "config/cthulib/promo_messages.json");
    private static PromoMessageConfig INSTANCE;

    public static class ServerPromo {
        public List<String> servers = new ArrayList<>();
        public List<MessagePart> messageParts = new ArrayList<>();

        public ServerPromo() {}

        public ServerPromo(List<String> servers, List<MessagePart> messageParts) {
            this.servers = servers;
            this.messageParts = messageParts;
        }
    }

    public static class MessagePart {
        public String text = "";
        public String color = "";
        public boolean bold = false;
        public boolean italic = false;
        public boolean underlined = false;
        public String clickAction = "";
        public String clickValue = "";

        public MessagePart() {}

        public MessagePart(String text) {
            this.text = text;
        }

        public MessagePart(String text, String color) {
            this.text = text;
            this.color = color;
        }

        public MessagePart(String text, String color, boolean bold, boolean italic, boolean underlined) {
            this.text = text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underlined = underlined;
        }

        public MessagePart(String text, String color, boolean bold, boolean italic, boolean underlined, String clickAction, String clickValue) {
            this.text = text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underlined = underlined;
            this.clickAction = clickAction;
            this.clickValue = clickValue;
        }
    }

    public static PromoMessageConfig load() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject existingJson = JsonParser.parseReader(reader).getAsJsonObject();
                INSTANCE = GSON.fromJson(existingJson, PromoMessageConfig.class);
                return INSTANCE;
            } catch (Exception e) {
                e.printStackTrace();
                INSTANCE = createDefault();
                INSTANCE.save();
                return INSTANCE;
            }
        }

        INSTANCE = createDefault();
        INSTANCE.save();
        return INSTANCE;
    }

    private static PromoMessageConfig createDefault() {
        PromoMessageConfig config = new PromoMessageConfig();

        List<String> cduServers = new ArrayList<>();
        cduServers.add("playcdu.co");

        List<MessagePart> cduMessage = new ArrayList<>();
        cduMessage.add(new MessagePart("Use code "));
        cduMessage.add(new MessagePart("PixelDream", "light_purple", false, false, false));
        cduMessage.add(new MessagePart(" (case-sensitive) for 30% off on the "));
        cduMessage.add(new MessagePart("CDU Store", "white", false, false, true, "open_url", "https://store.playcdu.co"));
        cduMessage.add(new MessagePart("! "));

        config.serverPromos.add(new ServerPromo(cduServers, cduMessage));

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

    public static PromoMessageConfig get() {
        if (INSTANCE == null) {
            return load();
        }
        return INSTANCE;
    }
}