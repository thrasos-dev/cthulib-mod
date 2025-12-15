package net.pixeldreamstudios.cthulib.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModData {
    public String modId;
    public String name;
    public String description;
    public String version;
    public String author;
    public String iconPath;
    public List<String> categories = new ArrayList<>();
    public Map<String, String> links = new HashMap<>();

    public ModData(String modId, String name, String description, String version, String author) {
        this.modId = modId;
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
    }
}