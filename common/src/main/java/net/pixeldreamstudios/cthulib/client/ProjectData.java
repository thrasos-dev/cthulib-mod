package net.pixeldreamstudios.cthulib.client;

import net.pixeldreamstudios.cthulib.util.CurseForgeAPI;

import java.util.ArrayList;
import java.util.List;

public class ProjectData {
    private final String projectId;
    private final String name;
    private final String description;
    private final String iconUrl;
    private final List<String> supportedVersions;
    private final List<String> loaders;
    private final List<String> authors;
    private final ProjectType type;
    private final String projectUrl;
    private static List<ProjectData> cachedProjects = null;

    public ProjectData(String projectId, String name, String description, String iconUrl,
                      List<String> supportedVersions, List<String> loaders,
                      List<String> authors, ProjectType type, String projectUrl) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.supportedVersions = supportedVersions;
        this.loaders = loaders;
        this.authors = authors;
        this.type = type;
        this.projectUrl = projectUrl;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public List<String> getSupportedVersions() {
        return supportedVersions;
    }

    public List<String> getLoaders() {
        return loaders;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public ProjectType getType() {
        return type;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public static List<ProjectData> getCachedProjects() {
        if (cachedProjects != null) {
            return cachedProjects;
        }
        
        return getFallbackProjects();
    }
    public static List<ProjectData> fetchAndCacheProjects() {
        if (cachedProjects != null) {
            return cachedProjects;
        }
        
        List<ProjectData> projects = CurseForgeAPI.fetchAuthorProjects();
        
        if (projects.isEmpty()) {
            cachedProjects = getFallbackProjects();
        } else {
            cachedProjects = projects;
        }
        
        return cachedProjects;
    }
    
    private static List<ProjectData> getFallbackProjects() {
        List<ProjectData> projects = new ArrayList<>();
        
        projects.add(new ProjectData(
                "no-projects",
                "No Projects Available",
                "Projects could not be loaded. Check your internet connection or try again later.",
                "",
                List.of(),
                List.of(),
                List.of(),
                ProjectType.MOD,
                ""
        ));
        
        return projects;
    }
    public static void clearCache() {
        cachedProjects = null;
    }
}
