package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;
import org.lwjgl.glfw.GLFW;

public class DevModeManager {
    private static boolean devModeEnabled = false;
    private static long lastToggleTime = 0;
    private static final long TOGGLE_COOLDOWN = 500;
    
    public static boolean isDevModeEnabled() {
        return devModeEnabled;
    }
    
    public static void setDevModeEnabled(boolean enabled) {
        devModeEnabled = enabled;
        if (enabled) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.literal("§6Dev Mode: §aENABLED §7(Alt+Drag to move elements)"),
                false
            );
        } else {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.literal("§6Dev Mode: §cDISABLED"),
                false
            );
        }
    }
    
    public static void toggleDevMode() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToggleTime < TOGGLE_COOLDOWN) {
            return;
        }
        lastToggleTime = currentTime;
        setDevModeEnabled(!devModeEnabled);
    }
    
    public static boolean checkDevModeKeybind() {
        CthuLibConfig config = CthuLibConfig.getInstance();
        long window = Minecraft.getInstance().getWindow().getWindow();
        
        boolean key1Pressed = GLFW.glfwGetKey(window, config.devModeKey1) == GLFW.GLFW_PRESS;
        boolean key2Pressed = GLFW.glfwGetKey(window, config.devModeKey2) == GLFW.GLFW_PRESS;
        boolean key3Pressed = GLFW.glfwGetKey(window, config.devModeKey3) == GLFW.GLFW_PRESS;
        
        return key1Pressed && key2Pressed && key3Pressed;
    }
    
    public static boolean isAltPressed() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
               GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }
    
    public static boolean shouldAllowDragging() {
        return devModeEnabled && isAltPressed();
    }
}
