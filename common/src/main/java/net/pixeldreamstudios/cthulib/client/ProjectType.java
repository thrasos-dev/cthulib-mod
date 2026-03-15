package net.pixeldreamstudios.cthulib.client;

public enum ProjectType {
    MOD("Mod"),
    MODPACK("Modpack"),
    ALL("All");

    private final String displayName;

    ProjectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ProjectType next() {
        return switch (this) {
            case MOD -> MODPACK;
            case MODPACK -> ALL;
            case ALL -> MOD;
        };
    }
}
