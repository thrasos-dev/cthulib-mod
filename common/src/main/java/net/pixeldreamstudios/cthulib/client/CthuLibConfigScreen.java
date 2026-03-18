package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.util.ArrayList;
import java.util.List;

public class CthuLibConfigScreen extends Screen {
    private final Screen parent;
    private final CthuLibConfig config;
    private final List<AbstractWidget> configWidgets = new ArrayList<>();
    private final List<Integer> widgetBaseY = new ArrayList<>();
    private final List<String> widgetLabels = new ArrayList<>();
    private final List<Button> resetButtons = new ArrayList<>();
    private final List<Integer> resetButtonBaseY = new ArrayList<>();
    private final List<Button> navigationButtons = new ArrayList<>();
    private final List<Integer> sectionPositions = new ArrayList<>();
    private final List<String> sectionNames = new ArrayList<>();
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int closeButtonX = 0;
    private final int closeButtonY = 6;
    private static final int CLOSE_BUTTON_SIZE = 16;
    private static final int ENTRY_HEIGHT = 26;
    private static final int NAV_BUTTON_WIDTH = 90;
    private static final int NAV_BUTTON_HEIGHT = 18;
    private static final int NAV_PADDING = 4;

    public CthuLibConfigScreen(Screen parent) {
        super(Component.literal("CthuLib Configuration"));
        this.parent = parent;
        this.config = CthuLibConfig.getInstance();
    }

    @Override
    protected void init() {
        this.configWidgets.clear();
        this.widgetBaseY.clear();
        this.widgetLabels.clear();
        this.resetButtons.clear();
        this.resetButtonBaseY.clear();
        this.navigationButtons.clear();
        this.sectionPositions.clear();
        this.sectionNames.clear();
        int yPos = 64;
        int leftX = this.width / 2 - 200;

        closeButtonX = this.width - CLOSE_BUTTON_SIZE - 6;

        addRenderableWidget(Button.builder(Component.literal("X"), btn -> {
            config.save();
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(closeButtonX, closeButtonY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build());

        yPos += 10;

        addSection("General", yPos);
        addCheckbox("Show in Title Screen", config.showInTitleScreen, val -> config.showInTitleScreen = val, leftX, yPos);
        addResetButton("Reset General", yPos, () -> {
            config.resetGeneralSettings();
            config.save();
            this.minecraft.setScreen(new CthuLibConfigScreen(parent));
        });
        yPos += ENTRY_HEIGHT;
        addCheckbox("Show in Settings", config.showInSettings, val -> config.showInSettings = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Show Name", config.showName, val -> config.showName = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Logo Path", "e.g., cthulib:textures/gui/pdslogo.png", config.logoPath, val -> config.logoPath = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Background Path", "e.g., cthulib:textures/gui/pdsbg.png", config.backgroundPath, val -> config.backgroundPath = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Button Texture", "e.g., cthulib:textures/gui/button_normal.png", config.brightnessButtonTexture, val -> config.brightnessButtonTexture = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Wiki Button Texture", "e.g., cthulib:textures/gui/button_web.png", config.wikiLogo, val -> config.wikiLogo = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Discord Button Texture", "e.g., cthulib:textures/gui/button_discord.png", config.discordLogo, val -> config.discordLogo = val, leftX, yPos);

        yPos += ENTRY_HEIGHT + 10;
        addSection("Modlist Button", yPos);
        addCheckbox("Show Modlist Button", config.showModlistButton, val -> config.showModlistButton = val, leftX, yPos);
        addResetButton("Reset Modlist", yPos, () -> {
            config.resetModlistButton();
            config.save();
            this.minecraft.setScreen(new CthuLibConfigScreen(parent));
        });
        yPos += ENTRY_HEIGHT;
        addIntSlider("Title Screen X", config.buttonXtitleScreen, -2000, 2000, val -> config.buttonXtitleScreen = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Title Screen Y", config.buttonYtitleScreen, -2000, 2000, val -> config.buttonYtitleScreen = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Width", config.modlistButtonWidth, 10, 100, val -> config.modlistButtonWidth = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Height", config.modlistButtonHeight, 10, 100, val -> config.modlistButtonHeight = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Scale", config.modlistButtonScale, 0.3f, 3.0f, val -> config.modlistButtonScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Scale with Screen", config.modlistButtonScaleWithScreen, val -> config.modlistButtonScaleWithScreen = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Use Anchor (GUI-pixel offset)", config.modlistButtonUseAnchor, val -> config.modlistButtonUseAnchor = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Min Scale", config.modlistButtonMinScale, 0.1f, 2.0f, val -> config.modlistButtonMinScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Max Scale", config.modlistButtonMaxScale, 0.1f, 3.0f, val -> config.modlistButtonMaxScale = val, leftX, yPos);

        yPos += ENTRY_HEIGHT + 10;
        addSection("Promo", yPos);
        addCheckbox("Show Promo Button", config.showPromoButton, val -> config.showPromoButton = val, leftX, yPos);
        addResetButton("Reset Promo", yPos, () -> {
            config.resetPromoButton();
            config.save();
            this.minecraft.setScreen(new CthuLibConfigScreen(parent));
        });
        yPos += ENTRY_HEIGHT;
        addTextField("Promo Texture", "e.g., cthulib:textures/gui/promo_button.png", config.promoButtonTexture, val -> config.promoButtonTexture = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Promo Tooltip", "e.g., Need a Server?", config.promoButtonTooltip, val -> config.promoButtonTooltip = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Promo URL", "e.g., https://example.com", config.promoButtonUrl, val -> config.promoButtonUrl = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Promo X", config.promoButtonX, -1000, 1000, val -> config.promoButtonX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Promo Y", config.promoButtonY, -1000, 1000, val -> config.promoButtonY = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Promo Width", config.promoButtonWidth, 20, 400, val -> config.promoButtonWidth = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Promo Height", config.promoButtonHeight, 10, 200, val -> config.promoButtonHeight = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Scale", config.promoButtonScale, 0.1f, 3.0f, val -> config.promoButtonScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Scale with Screen", config.promoButtonScaleWithScreen, val -> config.promoButtonScaleWithScreen = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Use Anchor (GUI-pixel offset)", config.promoButtonUseAnchor, val -> config.promoButtonUseAnchor = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Min Scale", config.promoButtonMinScale, 0.1f, 2.0f, val -> config.promoButtonMinScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Max Scale", config.promoButtonMaxScale, 0.1f, 3.0f, val -> config.promoButtonMaxScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Center X", config.promoButtonStartFromCenterX, val -> config.promoButtonStartFromCenterX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Center Y", config.promoButtonStartFromCenterY, val -> config.promoButtonStartFromCenterY = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Left X", config.promoButtonStartFromLeftX, val -> config.promoButtonStartFromLeftX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Right X", config.promoButtonStartFromRightX, val -> config.promoButtonStartFromRightX = val, leftX, yPos);

        yPos += ENTRY_HEIGHT + 10;
        addSection("Slider", yPos);
        addCheckbox("Show Project Slider", config.showProjectSlider, val -> config.showProjectSlider = val, leftX, yPos);
        addResetButton("Reset Slider", yPos, () -> {
            config.resetProjectSlider();
            config.save();
            this.minecraft.setScreen(new CthuLibConfigScreen(parent));
        });
        yPos += ENTRY_HEIGHT;
        addTextField("Slider Header Text", "e.g., Check our other projects!", config.sliderHeaderText, val -> config.sliderHeaderText = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Slider X", config.sliderX, -2000, 2000, val -> config.sliderX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Slider Y", config.sliderY, -1000, 1000, val -> config.sliderY = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Card Width", config.sliderCardWidth, 100, 600, val -> config.sliderCardWidth = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Card Height", config.sliderCardHeight, 40, 300, val -> config.sliderCardHeight = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Slider Scale", config.sliderScale, 0.3f, 2.0f, val -> config.sliderScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Auto Slide", config.sliderAutoSlide, val -> config.sliderAutoSlide = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Auto Slide Delay (s)", config.sliderAutoSlideDelay, 1.0f, 20.0f, val -> config.sliderAutoSlideDelay = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Center X", config.sliderStartFromCenterX, val -> config.sliderStartFromCenterX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Center Y", config.sliderStartFromCenterY, val -> config.sliderStartFromCenterY = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Left X", config.sliderStartFromLeftX, val -> config.sliderStartFromLeftX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Right X", config.sliderStartFromRightX, val -> config.sliderStartFromRightX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Scale With Screen", config.sliderScaleWithScreen, val -> config.sliderScaleWithScreen = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Use Anchor (GUI-pixel offset)", config.sliderUseAnchor, val -> config.sliderUseAnchor = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Min Scale", config.sliderMinScale, 0.1f, 1.0f, val -> config.sliderMinScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Max Scale", config.sliderMaxScale, 1.0f, 3.0f, val -> config.sliderMaxScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Show Previews", config.sliderShowPreviews, val -> config.sliderShowPreviews = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Preview Scale", config.sliderPreviewScale, 0.3f, 1.0f, val -> config.sliderPreviewScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Preview Offset", config.sliderPreviewOffset, 0, 100, val -> config.sliderPreviewOffset = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Show Filter Button", config.sliderShowFilterButton, val -> config.sliderShowFilterButton = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Filter Btn Offset X", config.sliderFilterOffsetX, -200, 200, val -> config.sliderFilterOffsetX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Filter Btn Offset Y", config.sliderFilterOffsetY, -200, 200, val -> config.sliderFilterOffsetY = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Default Filter", "ALL / MOD / MODPACK", config.sliderDefaultFilter, val -> config.sliderDefaultFilter = val.toUpperCase(), leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Arrow Idle Color (hex)", "e.g., 4ADBFF", config.sliderArrowIdleColor, val -> config.sliderArrowIdleColor = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Arrow Hover Color (hex)", "e.g., FFAA33", config.sliderArrowHoverColor, val -> config.sliderArrowHoverColor = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Blacklisted Projects", "Names or IDs, comma-separated", config.sliderBlacklistedProjects, val -> config.sliderBlacklistedProjects = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Unmaintained Projects", "Names or IDs, comma-separated", config.sliderUnmaintainedProjects, val -> config.sliderUnmaintainedProjects = val, leftX, yPos);

        yPos += ENTRY_HEIGHT + 10;
        addSection("Changelog", yPos);
        addCheckbox("Show Changelog Button", config.showChangelogButton, val -> config.showChangelogButton = val, leftX, yPos);
        addResetButton("Reset Changelog", yPos, () -> {
            config.resetChangelog();
            config.save();
            this.minecraft.setScreen(new CthuLibConfigScreen(parent));
        });
        yPos += ENTRY_HEIGHT;
        addTextField("Changelog Button Texture", "e.g., cthulib:textures/gui/changelog.png", config.changelogButtonTexture, val -> config.changelogButtonTexture = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Changelog X", config.changelogButtonX, -1000, 1000, val -> config.changelogButtonX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Changelog Y", config.changelogButtonY, -1000, 1000, val -> config.changelogButtonY = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Changelog Width", config.changelogButtonWidth, 50, 300, val -> config.changelogButtonWidth = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Changelog Height", config.changelogButtonHeight, 10, 100, val -> config.changelogButtonHeight = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Scale", config.changelogButtonScale, 0.1f, 3.0f, val -> config.changelogButtonScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Scale with Screen", config.changelogButtonScaleWithScreen, val -> config.changelogButtonScaleWithScreen = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Use Anchor (GUI-pixel offset)", config.changelogButtonUseAnchor, val -> config.changelogButtonUseAnchor = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Min Scale", config.changelogButtonMinScale, 0.1f, 2.0f, val -> config.changelogButtonMinScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Max Scale", config.changelogButtonMaxScale, 0.1f, 3.0f, val -> config.changelogButtonMaxScale = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Center X", config.changelogButtonStartFromCenterX, val -> config.changelogButtonStartFromCenterX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Center Y", config.changelogButtonStartFromCenterY, val -> config.changelogButtonStartFromCenterY = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Left X", config.changelogButtonStartFromLeftX, val -> config.changelogButtonStartFromLeftX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Start From Right X", config.changelogButtonStartFromRightX, val -> config.changelogButtonStartFromRightX = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Panel Width", config.changelogPanelWidth, 300, 1200, val -> config.changelogPanelWidth = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addIntSlider("Panel Height", config.changelogPanelHeight, 200, 800, val -> config.changelogPanelHeight = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addTextField("Panel Texture", "e.g., cthulib:textures/gui/panel.png", config.changelogPanelTexture, val -> config.changelogPanelTexture = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Show Notification Badge", config.showChangelogNotification, val -> config.showChangelogNotification = val, leftX, yPos);

        yPos += ENTRY_HEIGHT + 10;
        addSection("Animations", yPos);
        addCheckbox("Animations Enabled", config.animationsEnabled, val -> config.animationsEnabled = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Hover Speed", config.hoverAnimationSpeed, 0.01f, 1.0f, val -> config.hoverAnimationSpeed = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Press Speed", config.pressAnimationSpeed, 0.01f, 1.0f, val -> config.pressAnimationSpeed = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Pulse Speed", config.pulseAnimationSpeed, 0.0f, 0.2f, val -> config.pulseAnimationSpeed = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Shine Effect", config.shineEffectEnabled, val -> config.shineEffectEnabled = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Shine Speed", config.shineAnimationSpeed, 0.1f, 5.0f, val -> config.shineAnimationSpeed = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Slider Entry Anim.", config.sliderEntryAnimationEnabled, val -> config.sliderEntryAnimationEnabled = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Entry Speed", config.sliderEntryAnimationSpeed, 0.01f, 1.0f, val -> config.sliderEntryAnimationSpeed = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Fade-In Speed", config.sliderFadeInSpeed, 0.01f, 1.0f, val -> config.sliderFadeInSpeed = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addCheckbox("Card Transition", config.sliderCardTransitionEnabled, val -> config.sliderCardTransitionEnabled = val, leftX, yPos);
        yPos += ENTRY_HEIGHT;
        addFloatSlider("Transition Speed", config.sliderCardTransitionSpeed, 0.01f, 1.0f, val -> config.sliderCardTransitionSpeed = val, leftX, yPos);

        this.contentHeight = yPos + ENTRY_HEIGHT;

        createNavigationButtons();

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            config.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private void addResetButton(String label, int y, Runnable onPress) {
        int rightX = this.width / 2 + 10;
        Button resetBtn = Button.builder(Component.literal(label), btn -> onPress.run())
                .bounds(rightX, y, 80, 20)
                .build();
        this.resetButtons.add(resetBtn);
        this.resetButtonBaseY.add(y);
        this.addRenderableWidget(resetBtn);
    }

    private void addCheckbox(String label, boolean initial, BooleanCallback callback, int x, int y) {
        Checkbox checkbox = Checkbox.builder(Component.literal(label), this.font)
                .selected(initial)
                .onValueChange((cb, val) -> {
                    callback.onChanged(val);
                    config.save();
                })
                .pos(x, y)
                .build();
        this.configWidgets.add(checkbox);
        this.widgetBaseY.add(y);
        this.widgetLabels.add(null);
        this.addRenderableWidget(checkbox);
    }

    private void addIntSlider(String label, int initial, int min, int max, IntCallback callback, int x, int y) {
        IntSlider slider = new IntSlider(x, y, 310, 20, label, initial, min, max, callback);
        this.configWidgets.add(slider);
        this.widgetBaseY.add(y);
        this.widgetLabels.add(null);
        this.addRenderableWidget(slider);

        EditBox inputBox = new EditBox(this.font, x + 315, y, 85, 20, Component.literal(label));
        inputBox.setValue(String.valueOf(initial));
        inputBox.setResponder(val -> {
            try {
                int intValue = Integer.parseInt(val);
                if (intValue >= min && intValue <= max) {
                    slider.setSliderValue(intValue);
                    callback.onChanged(intValue);
                    config.save();
                }
            } catch (NumberFormatException e) {
            }
        });
        inputBox.setFilter(str -> str.matches("-?\\d*"));
        this.configWidgets.add(inputBox);
        this.widgetBaseY.add(y);
        this.widgetLabels.add(null);
        this.addRenderableWidget(inputBox);
    }

    private void addFloatSlider(String label, float initial, float min, float max, FloatCallback callback, int x, int y) {
        FloatSlider slider = new FloatSlider(x, y, 310, 20, label, initial, min, max, callback);
        this.configWidgets.add(slider);
        this.widgetBaseY.add(y);
        this.widgetLabels.add(null);
        this.addRenderableWidget(slider);

        EditBox inputBox = new EditBox(this.font, x + 315, y, 85, 20, Component.literal(label));
        inputBox.setValue(String.format("%.2f", initial));
        inputBox.setResponder(val -> {
            try {
                float floatValue = Float.parseFloat(val);
                if (floatValue >= min && floatValue <= max) {
                    slider.setSliderValue(floatValue);
                    callback.onChanged(floatValue);
                    config.save();
                }
            } catch (NumberFormatException e) {
            }
        });
        inputBox.setFilter(str -> str.matches("-?\\d*\\.?\\d*"));
        this.configWidgets.add(inputBox);
        this.widgetBaseY.add(y);
        this.widgetLabels.add(null);
        this.addRenderableWidget(inputBox);
    }

    private void addTextField(String label, String hint, String initial, StringCallback callback, int x, int y) {
        EditBox textField = new EditBox(this.font, x, y, 400, 20, Component.literal(label));
        textField.setMaxLength(512);
        textField.setValue(initial == null ? "" : initial);
        textField.setHint(Component.literal(hint));
        textField.setResponder(val -> {
            callback.onChanged(val);
            config.save();
        });

        this.configWidgets.add(textField);
        this.widgetBaseY.add(y);
        this.widgetLabels.add(label);
        this.addRenderableWidget(textField);
    }

    private void addSection(String name, int yPos) {
        this.sectionNames.add(name);
        this.sectionPositions.add(yPos);
    }

    private void createNavigationButtons() {
        if (sectionNames.isEmpty()) {
            return;
        }

        int navX = this.width - NAV_BUTTON_WIDTH - 15 - 8;
        int startY = 45;
        int buttonSpacing = NAV_BUTTON_HEIGHT + NAV_PADDING;

        for (int i = 0; i < sectionNames.size(); i++) {
            final int sectionIndex = i;
            String sectionName = sectionNames.get(i);
            int buttonY = startY + i * buttonSpacing;

            Button navButton = Button.builder(Component.literal(sectionName), btn -> {
                scrollToSection(sectionIndex);
            }).bounds(navX, buttonY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT).build();

            this.navigationButtons.add(navButton);
            this.addRenderableWidget(navButton);
        }
    }

    private void scrollToSection(int sectionIndex) {
        if (sectionIndex < 0 || sectionIndex >= sectionPositions.size()) {
            return;
        }

        int targetY = sectionPositions.get(sectionIndex);
        int maxScroll = Math.max(0, this.contentHeight - (this.height - 100));

        this.scrollOffset = Mth.clamp(targetY - 50, 0, maxScroll);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int leftX = this.width / 2 - 200;

        for (int i = 0; i < this.configWidgets.size(); i++) {
            AbstractWidget widget = this.configWidgets.get(i);
            int baseY = this.widgetBaseY.get(i);
            widget.setY(baseY - scrollOffset);
        }

        for (int i = 0; i < this.resetButtons.size(); i++) {
            Button btn = this.resetButtons.get(i);
            int baseY = this.resetButtonBaseY.get(i);
            btn.setY(baseY - scrollOffset);
        }

        graphics.enableScissor(0, 45, this.width, this.height - 55);

        for (int i = 0; i < sectionNames.size(); i++) {
            int sectionY = sectionPositions.get(i) - scrollOffset;
            if (sectionY >= 32 && sectionY < this.height - 55) {
                graphics.drawString(this.font, sectionNames.get(i), leftX, sectionY + 6, 0xFFAA00, false);
            }
        }

        for (int i = 0; i < this.configWidgets.size(); i++) {
            String label = this.widgetLabels.get(i);
            if (label != null) {
                AbstractWidget widget = this.configWidgets.get(i);
                int baseY = this.widgetBaseY.get(i);
                int renderY = baseY - scrollOffset;
                if (renderY >= 32 && renderY < this.height - 55) {
                    graphics.drawString(this.font, label + ": ", leftX, renderY + 6, 0xFFFFFF, false);
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.disableScissor();

        int sbMax = Math.max(0, this.contentHeight - (this.height - 100));
        if (sbMax > 0) {
            int sbX = this.width - 8;
            int sbTop = 45;
            int sbBottom = this.height - 55;
            int sbH = sbBottom - sbTop;
            int thumbH = Math.max(20, sbH * (this.height - 100) / this.contentHeight);
            int thumbY = sbTop + (sbH - thumbH) * scrollOffset / sbMax;
            graphics.fill(sbX, sbTop, sbX + 6, sbBottom, 0x44FFFFFF);
            graphics.fill(sbX, thumbY, sbX + 6, thumbY + thumbH, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, this.contentHeight - (this.height - 100));
        this.scrollOffset = Mth.clamp(this.scrollOffset - (int)(scrollY * 10), 0, maxScroll);
        return true;
    }

    @Override
    public void onClose() {
        config.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min;
        private final int max;
        private final IntCallback callback;

        public IntSlider(int x, int y, int width, int height, String label, int initial, int min, int max, IntCallback callback) {
            super(x, y, width, height, Component.literal(label + ": " + initial), (double)(initial - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.callback = callback;
        }

        @Override
        protected void updateMessage() {
            int value = min + (int)(this.value * (max - min));
            this.setMessage(Component.literal(label + ": " + value));
        }

        @Override
        protected void applyValue() {
            int value = min + (int)(this.value * (max - min));
            callback.onChanged(value);
            config.save();
        }

        public void setSliderValue(int newValue) {
            this.value = (double)(newValue - min) / (max - min);
            this.updateMessage();
        }
    }

    private class FloatSlider extends AbstractSliderButton {
        private final String label;
        private final float min;
        private final float max;
        private final FloatCallback callback;

        public FloatSlider(int x, int y, int width, int height, String label, float initial, float min, float max, FloatCallback callback) {
            super(x, y, width, height, Component.literal(label + ": " + String.format("%.2f", initial)), (initial - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.callback = callback;
        }

        @Override
        protected void updateMessage() {
            float value = min + (float)(this.value * (max - min));
            this.setMessage(Component.literal(label + ": " + String.format("%.2f", value)));
        }

        @Override
        protected void applyValue() {
            float value = min + (float)(this.value * (max - min));
            callback.onChanged(value);
            config.save();
        }

        public void setSliderValue(float newValue) {
            this.value = (newValue - min) / (max - min);
            this.updateMessage();
        }
    }

    @FunctionalInterface
    interface BooleanCallback {
        void onChanged(boolean value);
    }

    @FunctionalInterface
    interface IntCallback {
        void onChanged(int value);
    }

    @FunctionalInterface
    interface FloatCallback {
        void onChanged(float value);
    }

    @FunctionalInterface
    interface StringCallback {
        void onChanged(String value);
    }
}