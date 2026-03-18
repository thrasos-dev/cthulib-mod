package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QuickConfigPanel {
    private final String widgetType;
    private final int panelX;
    private final int panelY;
    private final int panelWidth = 320;
    private final int panelHeight = 280;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<Integer> widgetBaseY = new ArrayList<>();
    private boolean visible = false;
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private AbstractWidget draggingWidget = null;
    private AbstractWidget focusedWidget = null;
    private final Runnable rebuildCallback;

    public QuickConfigPanel(String widgetType, int mouseX, int mouseY) {
        this(widgetType, mouseX, mouseY, null);
    }

    public QuickConfigPanel(String widgetType, int mouseX, int mouseY, Runnable rebuildCallback) {
        this.rebuildCallback = rebuildCallback;
        this.widgetType = widgetType;
        
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        this.panelX = Math.min(Math.max(mouseX + 10, 10), screenWidth - panelWidth - 10);
        this.panelY = Math.min(Math.max(mouseY - 50, 10), screenHeight - panelHeight - 10);
        
        createWidgets();
    }
    
    private void createWidgets() {
        widgets.clear();
        widgetBaseY.clear();
        
        CthuLibConfig cfg = CthuLibConfig.getInstance();
        int x = panelX + 10;
        int yPos = panelY + 40;
        int entryHeight = 26;
        int labelHeight = 14;
        
        switch (widgetType) {
            case "Promo Button":
                addIntSlider("X", cfg.promoButtonX, -1000, 1000, val -> cfg.promoButtonX = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Y", cfg.promoButtonY, -1000, 1000, val -> cfg.promoButtonY = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Width", cfg.promoButtonWidth, 20, 400, val -> {
                    cfg.promoButtonWidth = val;
                }, x, yPos);
                yPos += entryHeight;
                addIntSlider("Height", cfg.promoButtonHeight, 10, 200, val -> {
                    cfg.promoButtonHeight = val;
                }, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Scale", cfg.promoButtonScale, 0.1f, 3.0f, val -> cfg.promoButtonScale = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Scale with Screen", cfg.promoButtonScaleWithScreen, val -> cfg.promoButtonScaleWithScreen = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Use Anchor (GUI-pixel offset)", cfg.promoButtonUseAnchor, val -> cfg.promoButtonUseAnchor = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Start From Center X", cfg.promoButtonStartFromCenterX, val -> cfg.promoButtonStartFromCenterX = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Start From Center Y", cfg.promoButtonStartFromCenterY, val -> cfg.promoButtonStartFromCenterY = val, x, yPos);
                yPos += entryHeight;
                addLabel("\u2500\u2500 Animations \u2500\u2500", x, yPos);
                yPos += labelHeight;
                addCheckbox("Animations Enabled", cfg.animationsEnabled, val -> cfg.animationsEnabled = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Hover Speed", cfg.hoverAnimationSpeed, 0.01f, 1.0f, val -> cfg.hoverAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Press Speed", cfg.pressAnimationSpeed, 0.01f, 1.0f, val -> cfg.pressAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Pulse Speed", cfg.pulseAnimationSpeed, 0.0f, 0.2f, val -> cfg.pulseAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Shine Effect", cfg.shineEffectEnabled, val -> cfg.shineEffectEnabled = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Shine Speed", cfg.shineAnimationSpeed, 0.1f, 5.0f, val -> cfg.shineAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                break;
                
            case "Project Slider":
                addIntSlider("X", cfg.sliderX, -2000, 2000, val -> cfg.sliderX = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Y", cfg.sliderY, -1000, 1000, val -> cfg.sliderY = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Card Width", cfg.sliderCardWidth, 100, 600, val -> {
                    cfg.sliderCardWidth = val;
                    scheduleRebuild();
                }, x, yPos);
                yPos += entryHeight;
                addIntSlider("Card Height", cfg.sliderCardHeight, 40, 300, val -> {
                    cfg.sliderCardHeight = val;
                    scheduleRebuild();
                }, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Scale", cfg.sliderScale, 0.3f, 2.0f, val -> {
                    cfg.sliderScale = val;
                    scheduleRebuild();
                }, x, yPos);
                yPos += entryHeight;
                addCheckbox("Scale with Screen", cfg.sliderScaleWithScreen, val -> cfg.sliderScaleWithScreen = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Use Anchor (GUI-pixel offset)", cfg.sliderUseAnchor, val -> cfg.sliderUseAnchor = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Start From Center X", cfg.sliderStartFromCenterX, val -> cfg.sliderStartFromCenterX = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Start From Center Y", cfg.sliderStartFromCenterY, val -> cfg.sliderStartFromCenterY = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Show Filter Button", cfg.sliderShowFilterButton, val -> cfg.sliderShowFilterButton = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Filter Btn Offset X", cfg.sliderFilterOffsetX, -200, 200, val -> cfg.sliderFilterOffsetX = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Filter Btn Offset Y", cfg.sliderFilterOffsetY, -200, 200, val -> cfg.sliderFilterOffsetY = val, x, yPos);
                yPos += entryHeight;
                addCycleButton("Default Filter", new String[]{"ALL", "MOD", "MODPACK"}, cfg.sliderDefaultFilter, val -> cfg.sliderDefaultFilter = val, x, yPos);
                yPos += entryHeight;
                addTextField("Arrow Idle Color", "hex, e.g. 4ADBFF", cfg.sliderArrowIdleColor, val -> cfg.sliderArrowIdleColor = val, x, yPos);
                yPos += entryHeight;
                addTextField("Arrow Hover Color", "hex, e.g. FFAA33", cfg.sliderArrowHoverColor, val -> cfg.sliderArrowHoverColor = val, x, yPos);
                yPos += entryHeight;
                addTextField("Blacklisted Projects", "names/IDs, comma-sep", cfg.sliderBlacklistedProjects, val -> cfg.sliderBlacklistedProjects = val, x, yPos);
                yPos += entryHeight;
                addTextField("Unmaintained Projects", "names/IDs, comma-sep", cfg.sliderUnmaintainedProjects, val -> cfg.sliderUnmaintainedProjects = val, x, yPos);
                yPos += entryHeight;
                addLabel("\u2500\u2500 Animations \u2500\u2500", x, yPos);
                yPos += labelHeight;
                addCheckbox("Animations Enabled", cfg.animationsEnabled, val -> cfg.animationsEnabled = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Hover Speed", cfg.hoverAnimationSpeed, 0.01f, 1.0f, val -> cfg.hoverAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Press Speed", cfg.pressAnimationSpeed, 0.01f, 1.0f, val -> cfg.pressAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Pulse Speed", cfg.pulseAnimationSpeed, 0.0f, 0.2f, val -> cfg.pulseAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Entry Animation", cfg.sliderEntryAnimationEnabled, val -> cfg.sliderEntryAnimationEnabled = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Entry Speed", cfg.sliderEntryAnimationSpeed, 0.01f, 1.0f, val -> cfg.sliderEntryAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Fade-In Speed", cfg.sliderFadeInSpeed, 0.01f, 1.0f, val -> cfg.sliderFadeInSpeed = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Card Transition", cfg.sliderCardTransitionEnabled, val -> cfg.sliderCardTransitionEnabled = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Transition Speed", cfg.sliderCardTransitionSpeed, 0.01f, 1.0f, val -> cfg.sliderCardTransitionSpeed = val, x, yPos);
                yPos += entryHeight;
                break;
                
            case "Changelog Button":
                addIntSlider("X", cfg.changelogButtonX, -1000, 1000, val -> cfg.changelogButtonX = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Y", cfg.changelogButtonY, -1000, 1000, val -> cfg.changelogButtonY = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Width", cfg.changelogButtonWidth, 50, 300, val -> {
                    cfg.changelogButtonWidth = val;
                }, x, yPos);
                yPos += entryHeight;
                addIntSlider("Height", cfg.changelogButtonHeight, 10, 100, val -> {
                    cfg.changelogButtonHeight = val;
                }, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Scale", cfg.changelogButtonScale, 0.1f, 3.0f, val -> cfg.changelogButtonScale = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Scale with Screen", cfg.changelogButtonScaleWithScreen, val -> cfg.changelogButtonScaleWithScreen = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Use Anchor (GUI-pixel offset)", cfg.changelogButtonUseAnchor, val -> cfg.changelogButtonUseAnchor = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Start From Center X", cfg.changelogButtonStartFromCenterX, val -> cfg.changelogButtonStartFromCenterX = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Start From Center Y", cfg.changelogButtonStartFromCenterY, val -> cfg.changelogButtonStartFromCenterY = val, x, yPos);
                yPos += entryHeight;
                addLabel("\u2500\u2500 Animations \u2500\u2500", x, yPos);
                yPos += labelHeight;
                addCheckbox("Animations Enabled", cfg.animationsEnabled, val -> cfg.animationsEnabled = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Hover Speed", cfg.hoverAnimationSpeed, 0.01f, 1.0f, val -> cfg.hoverAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Press Speed", cfg.pressAnimationSpeed, 0.01f, 1.0f, val -> cfg.pressAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Pulse Speed", cfg.pulseAnimationSpeed, 0.0f, 0.2f, val -> cfg.pulseAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Shine Effect", cfg.shineEffectEnabled, val -> cfg.shineEffectEnabled = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Shine Speed", cfg.shineAnimationSpeed, 0.1f, 5.0f, val -> cfg.shineAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                break;

            case "Modlist Button":
                addIntSlider("X", cfg.buttonXtitleScreen, -2000, 2000, val -> cfg.buttonXtitleScreen = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Y", cfg.buttonYtitleScreen, -2000, 2000, val -> cfg.buttonYtitleScreen = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Width", cfg.modlistButtonWidth, 10, 100, val -> cfg.modlistButtonWidth = val, x, yPos);
                yPos += entryHeight;
                addIntSlider("Height", cfg.modlistButtonHeight, 10, 100, val -> cfg.modlistButtonHeight = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Scale", cfg.modlistButtonScale, 0.3f, 3.0f, val -> cfg.modlistButtonScale = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Scale with Screen", cfg.modlistButtonScaleWithScreen, val -> cfg.modlistButtonScaleWithScreen = val, x, yPos);
                yPos += entryHeight;
                addCheckbox("Use Anchor (GUI-pixel offset)", cfg.modlistButtonUseAnchor, val -> cfg.modlistButtonUseAnchor = val, x, yPos);
                yPos += entryHeight;
                addLabel("── Animations ──", x, yPos);
                yPos += labelHeight;
                addCheckbox("Animations Enabled", cfg.animationsEnabled, val -> cfg.animationsEnabled = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Hover Speed", cfg.hoverAnimationSpeed, 0.01f, 1.0f, val -> cfg.hoverAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                addFloatSlider("Press Speed", cfg.pressAnimationSpeed, 0.01f, 1.0f, val -> cfg.pressAnimationSpeed = val, x, yPos);
                yPos += entryHeight;
                break;
        }
        
        contentHeight = yPos - panelY + 10;
    }

    private void scheduleRebuild() {
        if (rebuildCallback != null) {
            rebuildCallback.run();
        }
    }

    private void addIntSlider(String label, int initial, int min, int max, Consumer<Integer> callback, int x, int y) {
        AbstractSliderButton slider = new AbstractSliderButton(x, y, 240, 20, Component.literal(label + ": " + initial),
                (double)(initial - min) / (max - min)) {
            @Override
            protected void updateMessage() {
                int value = min + (int)(this.value * (max - min));
                this.setMessage(Component.literal(label + ": " + value));
            }

            @Override
            protected void applyValue() {
                int value = min + (int)(this.value * (max - min));
                callback.accept(value);
                CthuLibConfig.getInstance().save();
            }
        };
        widgets.add(slider);
        widgetBaseY.add(y);
    }
    
    private void addFloatSlider(String label, float initial, float min, float max, Consumer<Float> callback, int x, int y) {
        AbstractSliderButton slider = new AbstractSliderButton(x, y, 240, 20, Component.literal(label + ": " + String.format("%.2f", initial)),
                (initial - min) / (max - min)) {
            @Override
            protected void updateMessage() {
                float value = min + (float)(this.value * (max - min));
                this.setMessage(Component.literal(label + ": " + String.format("%.2f", value)));
            }

            @Override
            protected void applyValue() {
                float value = min + (float)(this.value * (max - min));
                callback.accept(value);
                CthuLibConfig.getInstance().save();
            }
        };
        widgets.add(slider);
        widgetBaseY.add(y);
    }
    
    private void addCheckbox(String label, boolean initial, Consumer<Boolean> callback, int x, int y) {
        Checkbox checkbox = Checkbox.builder(Component.literal(label), Minecraft.getInstance().font)
                .selected(initial)
                .onValueChange((cb, val) -> {
                    callback.accept(val);
                    CthuLibConfig.getInstance().save();
                })
                .pos(x, y)
                .build();
        widgets.add(checkbox);
        widgetBaseY.add(y);
    }

    private void addLabel(String text, int x, int y) {
        AbstractWidget label = new AbstractWidget(x, y, 240, 10, Component.literal(text)) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int mx, int my, float pt) {
                guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX(), this.getY() + 1, 0xAAAAAA, false);
            }
            @Override
            public void updateWidgetNarration(NarrationElementOutput n) {}
        };
        widgets.add(label);
        widgetBaseY.add(y);
    }

    private void addTextField(String label, String hint, String initial, Consumer<String> callback, int x, int y) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, 240, 20, Component.literal(label));
        box.setMaxLength(256);
        box.setValue(initial == null ? "" : initial);
        box.setHint(Component.literal(hint));
        box.setResponder(val -> {
            callback.accept(val);
            CthuLibConfig.getInstance().save();
        });
        widgets.add(box);
        widgetBaseY.add(y);
    }

    private void addCycleButton(String prefix, String[] options, String current, Consumer<String> callback, int x, int y) {
        int startIdx = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(current)) { startIdx = i; break; }
        }
        final int[] idx = {startIdx};
        Button btn = Button.builder(Component.literal(prefix + ": " + options[idx[0]]), b -> {
            idx[0] = (idx[0] + 1) % options.length;
            b.setMessage(Component.literal(prefix + ": " + options[idx[0]]));
            callback.accept(options[idx[0]]);
            CthuLibConfig.getInstance().save();
        }).bounds(x, y, 240, 20).build();
        widgets.add(btn);
        widgetBaseY.add(y);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return focusedWidget != null && focusedWidget.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return focusedWidget != null && focusedWidget.charTyped(codePoint, modifiers);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0000000);
        
        graphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY, 0xFF6A3F9E);
        graphics.fill(panelX - 1, panelY + panelHeight, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF6A3F9E);
        graphics.fill(panelX - 1, panelY, panelX, panelY + panelHeight, 0xFF6A3F9E);
        graphics.fill(panelX + panelWidth, panelY, panelX + panelWidth + 1, panelY + panelHeight, 0xFF6A3F9E);
        
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, widgetType + " Config", panelX + 10, panelY + 10, 0xFFFFFF, false);
        graphics.fill(panelX + 5, panelY + 23, panelX + panelWidth - 5, panelY + 24, 0xFF6A3F9E);

        graphics.enableScissor(panelX, panelY + 27, panelX + panelWidth, panelY + panelHeight);
        
        for (int i = 0; i < widgets.size(); i++) {
            AbstractWidget widget = widgets.get(i);
            int baseY = widgetBaseY.get(i);
            widget.setY(baseY - scrollOffset);
            widget.render(graphics, mouseX, mouseY, partialTick);
        }
        
        graphics.disableScissor();
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
            mouseY >= panelY && mouseY <= panelY + panelHeight) {

            if (focusedWidget != null) {
                focusedWidget.setFocused(false);
                focusedWidget = null;
            }

            for (AbstractWidget widget : widgets) {
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    draggingWidget = widget;
                    focusedWidget = widget;
                    widget.setFocused(true);
                    return true;
                }
            }
            return true;
        }

        if (focusedWidget != null) {
            focusedWidget.setFocused(false);
            focusedWidget = null;
        }
        visible = false;
        draggingWidget = null;
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        draggingWidget = null;
        for (AbstractWidget widget : widgets) {
            widget.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible) return false;

        if (draggingWidget != null) {
            return draggingWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible) return false;
        
        if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
            mouseY >= panelY && mouseY <= panelY + panelHeight) {
            int maxScroll = Math.max(0, contentHeight - (panelHeight - 40));
            scrollOffset = Mth.clamp(scrollOffset - (int)(scrollY * 10), 0, maxScroll);
            return true;
        }
        return false;
    }
    
    public void show() {
        visible = true;
    }
    
    public void hide() {
        visible = false;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void toggle() {
        visible = !visible;
    }
}
