package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
public class UIElementPositionManager {

    public enum ElementType {
        BRIGHTNESS_BUTTON_TITLE("brightness_button_title", Placements.MODLIST_TITLE),
        BRIGHTNESS_BUTTON_SETTINGS("brightness_button_settings", Placements.MODLIST_SETTINGS),
        PROMO_BUTTON("promo_button", Placements.PROMO),
        CHANGELOG_BUTTON("changelog_button", Placements.CHANGELOG),
        PROJECT_SLIDER("project_slider", Placements.SLIDER),
        WIKI_BUTTON("wiki_button", Placements.WIKI),
        DISCORD_BUTTON("discord_button", Placements.DISCORD),
        CHANGELOG_PANEL("changelog_panel", new Placement.IntField(cfg -> cfg.changelogPanelX, (cfg, x) -> cfg.changelogPanelX = x),
                new Placement.IntField(cfg -> cfg.changelogPanelY, (cfg, y) -> cfg.changelogPanelY = y));

        private static final String FANCYMENU_PREFIX = "cthulib_";

        private final String id;
        private final Placement.Access access;
        private final Placement.IntField x;
        private final Placement.IntField y;

        ElementType(String id, Placement.Access access) {
            this.id = id;
            this.access = access;
            this.x = access.x();
            this.y = access.y();
        }

        ElementType(String id, Placement.IntField x, Placement.IntField y) {
            this.id = id;
            this.access = null;
            this.x = x;
            this.y = y;
        }

        public String id() {
            return id;
        }

        public String fancyMenuId() {
            return FANCYMENU_PREFIX + id;
        }

        public Placement.Access access() {
            return access;
        }

        public int getX(CthuLibConfig config) {
            return x.get().applyAsInt(config);
        }

        public int getY(CthuLibConfig config) {
            return y.get().applyAsInt(config);
        }

        public void setX(CthuLibConfig config, int value) {
            x.set().accept(config, value);
        }

        public void setY(CthuLibConfig config, int value) {
            y.set().accept(config, value);
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
            Minecraft client = Minecraft.getInstance();
            Optional<Placement.Origin> origin = type.access() == null ? Optional.empty()
                    : Placement.origin(type.access().anchor().get().apply(config), client.getWindow().getGuiScaledWidth(),
                    client.getWindow().getGuiScaledHeight(), type.fancyMenuId());
            type.setX(config, state.currentX - origin.map(Placement.Origin::x).orElse(0));
            type.setY(config, state.currentY - origin.map(Placement.Origin::y).orElse(0));
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
        endDragAdvanced(type, elementName, baseWidth, baseHeight, enableScaling, minScale, maxScale,
                startFromCenterX, startFromCenterY, startFromLeftX, startFromRightX, useAnchor, "");
    }

    public static void finishDrag(ElementType type, String elementName) {
        Placement.Spec spec = type.access().spec(CthuLibConfig.getInstance());
        endDragAdvanced(type, elementName, spec.scaledWidth(), spec.scaledHeight(), spec.scaleWithScreen(), spec.minScale(), spec.maxScale(),
                spec.centerX(), spec.centerY(), spec.leftX(), spec.rightX(), spec.useAnchor(), spec.anchor());
    }

    public static PositionInfo place(ElementType type) {
        Placement.Spec spec = type.access().spec(CthuLibConfig.getInstance());
        return calculateAdvancedPosition(type, spec.scaledWidth(), spec.scaledHeight(), spec.scaleWithScreen(), spec.minScale(), spec.maxScale(),
                spec.centerX(), spec.centerY(), spec.leftX(), spec.rightX(), spec.useAnchor(), spec.anchor());
    }

    public static void endDragAdvanced(ElementType type, String elementName,
                                      int baseWidth, int baseHeight,
                                      boolean enableScaling, float minScale, float maxScale,
                                      boolean startFromCenterX, boolean startFromCenterY,
                                      boolean startFromLeftX, boolean startFromRightX,
                                      boolean useAnchor, String anchor) {
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

            Optional<Placement.Origin> origin = Placement.origin(anchor, screenWidth, screenHeight, type.fancyMenuId());
            int configX;
            if (origin.isPresent()) {
                configX = state.currentX - origin.get().x();
            } else if (startFromCenterX) {
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
            if (origin.isPresent()) {
                configY = state.currentY - origin.get().y();
            } else if (startFromCenterY) {
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
                        (origin.isPresent() ? " [" + anchor + "]" : useAnchor ? " [Anchored]" : "")
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
        return calculateAdvancedPosition(type, baseWidth, baseHeight, enableScaling, minScale, maxScale,
                startFromCenterX, startFromCenterY, startFromLeftX, startFromRightX, useAnchor, "");
    }

    public static PositionInfo calculateAdvancedPosition(
            ElementType type,
            int baseWidth, int baseHeight,
            boolean enableScaling, float minScale, float maxScale,
            boolean startFromCenterX, boolean startFromCenterY,
            boolean startFromLeftX, boolean startFromRightX,
            boolean useAnchor, String anchor) {

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

        Optional<Placement.Origin> origin = Placement.origin(anchor, screenWidth, screenHeight, type.fancyMenuId());
        if (origin.isPresent()) {
            return new PositionInfo(origin.get().x() + configX, origin.get().y() + configY, width, height);
        }

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
