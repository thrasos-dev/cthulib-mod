package net.pixeldreamstudios.cthulib.util;

public class CthuLibLogger {
    private static final String PREFIX = "[CthuLib] ";
    
    public static void apiCall(String endpoint) {
        System.out.println(PREFIX + "API Call: " + endpoint);
    }
    
    public static void apiSuccess(String endpoint, String result) {
        System.out.println(PREFIX + "✓ " + endpoint + ": " + result);
    }
    
    public static void apiFailed(String endpoint, String reason) {
        System.out.println(PREFIX + "✗ " + endpoint + ": " + reason);
    }
    
    public static void cacheHit(String type, String key) {
        System.out.println(PREFIX + "Cache hit: " + type + " (" + key + ")");
    }
    
    public static void cacheMiss(String type, String key) {
        System.out.println(PREFIX + "Cache miss: " + type + " (" + key + ")");
    }
    
    public static void downloading(String type, String url) {
        System.out.println(PREFIX + "Downloading " + type + ": " + url);
    }
    
    public static void downloadSuccess(String type, String identifier) {
        System.out.println(PREFIX + "✓ Downloaded: " + type + " (" + identifier + ")");
    }
    
    public static void downloadFailed(String type, String identifier, String reason) {
        System.out.println(PREFIX + "✗ Download failed: " + type + " (" + identifier + ") - " + reason);
    }
    
    public static void info(String message) {
        System.out.println(PREFIX + message);
    }
    
    public static void error(String message) {
        System.err.println(PREFIX + "ERROR: " + message);
    }
}
