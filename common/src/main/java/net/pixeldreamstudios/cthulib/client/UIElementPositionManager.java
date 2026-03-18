package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
public class UIElementPositionManager {

    public enum ElementType {
        BRIGHTNESS_BUTTON_TITLE("brightness_button_title",
            cfg -> cfg.buttonXtitleScreen,
            cfg -> cfg.buttonYtitleScreen,
            (cfg, x) -> cfg.buttonXtitleScreen = x,
            (cfg, y) -> cfg.buttonYtitleScreen = y),
        
        BRIGHTNESS_BUTTON_SETTINGS("brightness_button_settings",
            cfg -> cfg.buttonXsettings,
            cfg -> cfg.buttonYsettings,
            (cfg, x) -> cfg.buttonXsettings = x,
            (cfg, y) -> cfg.buttonYsettings = y),
        
        PROMO_BUTTON("promo_button",
            cfg -> cfg.promoButtonX,
            cfg -> cfg.promoButtonY,
            (cfg, x) -> cfg.promoButtonX = x,
            (cfg, y) -> cfg.promoButtonY = y),
        
        CHANGELOG_BUTTON("changelog_button",
            cfg -> cfg.changelogButtonX,
            cfg -> cfg.changelogButtonY,
            (cfg, x) -> cfg.changelogButtonX = x,
            (cfg, y) -> cfg.changelogButtonY = y),
        
        PROJECT_SLIDER("project_slider",
            cfg -> cfg.sliderX,
            cfg -> cfg.sliderY,
            (cfg, x) -> cfg.sliderX = x,
            (cfg, y) -> cfg.sliderY = y),
        
        CHANGELOG_PANEL("changelog_panel",
            cfg -> cfg.changelogPanelX,
            cfg -> cfg.changelogPanelY,
            (cfg, x) -> cfg.changelogPanelX = x,
            (cfg, y) -> cfg.changelogPanelY = y);
        
        private final String id;
        private final Function<CthuLibConfig, Integer> xGetter;
        private final Function<CthuLibConfig, Integer> yGetter;
        private final BiConsumer<CthuLibConfig, Integer> xSetter;
        private final BiConsumer<CthuLibConfig, Integer> ySetter;
        
        ElementType(String id, 
                   Function<CthuLibConfig, Integer> xGetter,
                   Function<CthuLibConfig, Integer> yGetter,
                   BiConsumer<CthuLibConfig, Integer> xSetter,
                   BiConsumer<CthuLibConfig, Integer> ySetter) {
            this.id = id;
            this.xGetter = xGetter;
            this.yGetter = yGetter;
            this.xSetter = xSetter;
            this.ySetter = ySetter;
        }
        
        public int getX(CthuLibConfig config) {
            return xGetter.apply(config);
        }
        
        public int getY(CthuLibConfig config) {
            return yGetter.apply(config);
        }
        
        public void setX(CthuLibConfig config, int x) {
            xSetter.accept(config, x);
        }
        
        public void setY(CthuLibConfig config, int y) {
            ySetter.accept(config, y);
        }
    }
    
    public static class PositionInfo {
        public int x;
        public int y;
        public int width;
        public int height;
        
        public PositionInfo(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    private static class DragState {
        boolean isDragging = false;
        boolean hasMoved = false;
        double dragOffsetX = 0;
        double dragOffsetY = 0;
        int currentX = 0;
        int currentY = 0;
    }
    
    private static final Map<ElementType, DragState> dragStates = new HashMap<>();

    public static boolean isDragging(ElementType type) {
        return dragStates.containsKey(type) && dragStates.get(type).isDragging;
    }

    public static boolean hasMoved(ElementType type) {
        DragState state = dragStates.get(type);
        return state != null && state.hasMoved;
    }

    public static void startDrag(ElementType type, double mouseX, double mouseY, int elementX, int elementY) {
        DragState state = dragStates.computeIfAbsent(type, k -> new DragState());
        state.isDragging = true;
        state.hasMoved = false;
        state.dragOffsetX = mouseX - elementX;
        state.dragOffsetY = mouseY - elementY;
        state.currentX = elementX;
        state.currentY = elementY;
    }

    public static PositionInfo updateDrag(ElementType type, double mouseX, double mouseY, int width, int height) {
        DragState state = dragStates.get(type);
        if (state != null && state.isDragging) {
            state.currentX = (int)(mouseX - state.dragOffsetX);
            state.currentY = (int)(mouseY - state.dragOffsetY);
            state.hasMoved = true;
            return new PositionInfo(state.currentX, state.currentY, width, height);
        }
        return null;
    }

    public static void endDrag(ElementType type, String elementName) {
        DragState state = dragStates.get(type);
        if (state != null && state.isDragging) {
            state.isDragging = false;

            if (!state.hasMoved) return;

            CthuLibConfig config = CthuLibConfig.getInstance();
            type.setX(config, state.currentX);
            type.setY(config, state.currentY);
            config.save();
            
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal(
                        elementName + " Position Saved: " + state.currentX + ", " + state.currentY
                    ),
                    true
                );
            }
        }
    }

    public static void endDragAdvanced(ElementType type, String elementName,
                                      int baseWidth, int baseHeight,
                                      boolean enableScaling, float minScale, float maxScale,
                                      boolean startFromCenterX, boolean startFromCenterY,
                                      boolean startFromLeftX, boolean startFromRightX) {
        endDragAdvanced(type, elementName, baseWidth, baseHeight, enableScaling, minScale, maxScale,
                startFromCenterX, startFromCenterY, startFromLeftX, startFromRightX, false);
    }

    public static void endDragAdvanced(ElementType type, String elementName,
                                      int baseWidth, int baseHeight,
                                      boolean enableScaling, float minScale, float maxScale,
                                      boolean startFromCenterX, boolean startFromCenterY,
                                      boolean startFromLeftX, boolean startFromRightX,
                                      boolean useAnchor) {
        DragState state = dragStates.get(type);
        if (state != null && state.isDragging) {
            state.isDragging = false;

            if (!state.hasMoved) return;

            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            boolean scalePos = enableScaling && !useAnchor;
            float scaleFactor = calculateScaleFactor(enableScaling, minScale, maxScale);
            int width = calculateWidth(baseWidth, enableScaling, minScale, maxScale);
            int height = calculateHeight(baseHeight, enableScaling, minScale, maxScale);

            int configX;
            if (startFromCenterX) {
                int guiOffset = state.currentX - (screenWidth / 2) + (width / 2);
                configX = scalePos ? (int)(guiOffset / scaleFactor) : guiOffset;
            } else if (startFromLeftX) {
                configX = scalePos ? (int)(state.currentX / scaleFactor) : state.currentX;
            } else if (startFromRightX) {
                int guiOffset = state.currentX - screenWidth + width;
                configX = scalePos ? (int)(guiOffset / scaleFactor) : guiOffset;
            } else if (useAnchor) {
                configX = state.currentX - (screenWidth / 2) + (width / 2);
            } else {
                configX = scalePos ? (int)(state.currentX / scaleFactor) : state.currentX;
            }

            int configY;
            if (startFromCenterY) {
                int guiOffset = state.currentY - (screenHeight / 2) + (height / 2);
                configY = scalePos ? (int)(guiOffset / scaleFactor) : guiOffset;
            } else if (useAnchor) {
                configY = state.currentY - (screenHeight / 4) - 48;
            } else {
                if (state.currentY > screenHeight / 2) {
                    int guiOffset = state.currentY - screenHeight;
                    configY = scalePos ? (int)(guiOffset / scaleFactor) : guiOffset;
                } else {
                    configY = scalePos ? (int)(state.currentY / scaleFactor) : state.currentY;
                }
            }

            CthuLibConfig config = CthuLibConfig.getInstance();
            type.setX(config, configX);
            type.setY(config, configY);
            config.save();

            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal(
                        elementName + " Position Saved: Screen(" + state.currentX + ", " + state.currentY +
                        ") Config(" + configX + ", " + configY + ")" +
                        (useAnchor ? " [Anchored]" : "")
                    ),
                    true
                );
            }
        }
    }

    public static PositionInfo calculatePosition(ElementType type, int width, int height) {
        DragState state = dragStates.get(type);
        if (state != null && state.isDragging) {
            return new PositionInfo(state.currentX, state.currentY, width, height);
        }
        
        CthuLibConfig config = CthuLibConfig.getInstance();
        int configX = type.getX(config);
        int configY = type.getY(config);
        
        return new PositionInfo(configX, configY, width, height);
    }

    public static PositionInfo calculateAdvancedPosition(
            ElementType type,
            int baseWidth, int baseHeight,
            boolean enableScaling, float minScale, float maxScale,
            boolean startFromCenterX, boolean startFromCenterY,
            boolean startFromLeftX, boolean startFromRightX) {
        return calculateAdvancedPosition(type, baseWidth, baseHeight, enableScaling, minScale, maxScale,
                startFromCenterX, startFromCenterY, startFromLeftX, startFromRightX, false);
    }

    public static PositionInfo calculateAdvancedPosition(
            ElementType type,
            int baseWidth, int baseHeight,
            boolean enableScaling, float minScale, float maxScale,
            boolean startFromCenterX, boolean startFromCenterY,
            boolean startFromLeftX, boolean startFromRightX,
            boolean useAnchor) {

        DragState state = dragStates.get(type);
        if (state != null && state.isDragging) {
            return new PositionInfo(state.currentX, state.currentY,
                                   calculateWidth(baseWidth, enableScaling, minScale, maxScale),
                                   calculateHeight(baseHeight, enableScaling, minScale, maxScale));
        }

        int width = calculateWidth(baseWidth, enableScaling, minScale, maxScale);
        int height = calculateHeight(baseHeight, enableScaling, minScale, maxScale);

        CthuLibConfig config = CthuLibConfig.getInstance();
        int configX = type.getX(config);
        int configY = type.getY(config);

        boolean scalePos = enableScaling && !useAnchor;
        float scaleFactor = calculateScaleFactor(enableScaling, minScale, maxScale);
        int scaledConfigX = scalePos ? (int)(configX * scaleFactor) : configX;
        int scaledConfigY = scalePos ? (int)(configY * scaleFactor) : configY;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int finalX, finalY;

        if (startFromCenterX) {
            finalX = (screenWidth / 2) + scaledConfigX - (width / 2);
        } else if (startFromLeftX) {
            finalX = scaledConfigX;
        } else if (startFromRightX) {
            finalX = screenWidth - width + scaledConfigX;
        } else if (useAnchor) {
            finalX = (screenWidth / 2) - (width / 2) + scaledConfigX;
        } else {
            finalX = scaledConfigX;
        }

        if (startFromCenterY) {
            finalY = (screenHeight / 2) + scaledConfigY - (height / 2);
        } else if (useAnchor) {
            finalY = (screenHeight / 4) + 48 + scaledConfigY;
        } else {
            finalY = scaledConfigY < 0 ? screenHeight + scaledConfigY : scaledConfigY;
        }

        return new PositionInfo(finalX, finalY, width, height);
    }
    
    private static int calculateWidth(int baseWidth, boolean enableScaling, float minScale, float maxScale) {
        if (!enableScaling) return baseWidth;
        float scale = calculateScaleFactor(enableScaling, minScale, maxScale);
        return (int)(baseWidth * scale);
    }
    
    private static int calculateHeight(int baseHeight, boolean enableScaling, float minScale, float maxScale) {
        if (!enableScaling) return baseHeight;
        float scale = calculateScaleFactor(enableScaling, minScale, maxScale);
        return (int)(baseHeight * scale);
    }
    
    private static float calculateScaleFactor(boolean enableScaling, float minScale, float maxScale) {
        if (!enableScaling) return 1.0f;
        
        Minecraft mc = Minecraft.getInstance();
        int actualWindowWidth = mc.getWindow().getWidth();
        int actualWindowHeight = mc.getWindow().getHeight();
        float scaleFactor = Math.min(actualWindowWidth / 1920.0f, actualWindowHeight / 1080.0f);
        return Mth.clamp(scaleFactor, minScale, maxScale);
    }

    public static void resetAllDragStates() {
        dragStates.clear();
    }
    public static void resetDragState(ElementType type) {
        dragStates.remove(type);
    }
}
