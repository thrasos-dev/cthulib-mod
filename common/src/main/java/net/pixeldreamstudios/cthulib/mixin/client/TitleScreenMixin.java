package net.pixeldreamstudios.cthulib.mixin.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.client.ModListButton;
import net.pixeldreamstudios.cthulib.client.ChangelogButton;
import net.pixeldreamstudios.cthulib.client.ChangelogViewer;
import net.pixeldreamstudios.cthulib.client.DevModeManager;
import net.pixeldreamstudios.cthulib.client.ModsScreen;
import net.pixeldreamstudios.cthulib.client.ProjectSlider;
import net.pixeldreamstudios.cthulib.client.PromoButton;
import net.pixeldreamstudios.cthulib.client.QuickConfigPanel;
import net.pixeldreamstudios.cthulib.client.UIElementPositionManager;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;
import net.pixeldreamstudios.cthulib.util.ChangelogCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    
    @Unique
    private ChangelogViewer cthulib$changelogOverlay = null;
    @Unique
    private boolean cthulib$changelogOpen = false;
    @Unique
    private QuickConfigPanel cthulib$quickConfigPanel = null;
    @Unique
    private boolean cthulib$pendingRebuild = false;
    @Unique
    private static ModListButton cthulib$cachedBrightnessButton = null;
    @Unique
    private static PromoButton cthulib$cachedPromoButton = null;
    @Unique
    private static ProjectSlider cthulib$cachedProjectSlider = null;
    @Unique
    private static ChangelogButton cthulib$cachedChangelogButton = null;
    @Unique
    private static int cthulib$lastScreenWidth = -1;
    @Unique
    private static int cthulib$lastScreenHeight = -1;
    @Unique
    private static void cthulib$invalidateCache() {
        cthulib$cachedBrightnessButton = null;
        cthulib$cachedPromoButton = null;
        cthulib$cachedProjectSlider = null;
        cthulib$cachedChangelogButton = null;
    }

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (DevModeManager.checkDevModeKeybind()) {
            DevModeManager.toggleDevMode();
        }
        
        CthuLibConfig cfg = CthuLibConfig.get();

        if (!cfg.showInTitleScreen) {
            return;
        }
        
        boolean needsRecreate = cthulib$lastScreenWidth == -1 && cthulib$lastScreenHeight == -1;

        if (CthuLibConfig.needsCacheInvalidation()) {
            cthulib$invalidateCache();
            CthuLibConfig.clearCacheInvalidationFlag();
            needsRecreate = true;
        }
        
        cthulib$lastScreenWidth = this.width;
        cthulib$lastScreenHeight = this.height;

        if (cfg.showModlistButton) {
            if (cthulib$cachedBrightnessButton == null || needsRecreate) {
                int x, y;
                if (cfg.buttonXtitleScreen == 0 && cfg.buttonYtitleScreen == 0) {
                    x = this.width / 2 - 100 + 205;
                    y = this.height / 4 + 48 + 49;
                } else {
                    x = cfg.buttonXtitleScreen;
                    y = cfg.buttonYtitleScreen;
                }

                ResourceLocation buttonLocation;
                try {
                    String texturePath = (cfg.brightnessButtonTexture != null && !cfg.brightnessButtonTexture.isEmpty())
                        ? cfg.brightnessButtonTexture : cfg.buttonLogo;
                    buttonLocation = ResourceLocation.parse(texturePath);
                } catch (Exception e) {
                    buttonLocation = ResourceLocation.fromNamespaceAndPath("cthulib", "textures/gui/button.png");
                }

                cthulib$cachedBrightnessButton = new ModListButton(
                        x, y, cfg.modlistButtonWidth, cfg.modlistButtonHeight,
                        buttonLocation,
                        b -> Minecraft.getInstance().setScreen(new ModsScreen(this)),
                        Component.literal(cfg.buttonTooltip),
                        UIElementPositionManager.ElementType.BRIGHTNESS_BUTTON_TITLE
                );
            } else {
                int x, y;
                if (cfg.buttonXtitleScreen == 0 && cfg.buttonYtitleScreen == 0) {
                    x = this.width / 2 - 100 + 205;
                    y = this.height / 4 + 48 + 49;
                } else {
                    x = cfg.buttonXtitleScreen;
                    y = cfg.buttonYtitleScreen;
                }
                cthulib$cachedBrightnessButton.setX(x);
                cthulib$cachedBrightnessButton.setY(y);
            }
            this.addRenderableWidget(cthulib$cachedBrightnessButton);
        }
        
        if (cfg.showPromoButton) {
            if (cthulib$cachedPromoButton == null || needsRecreate) {
                String promoImageUrl = PromoButton.getPromoUrlWithFallback();
                
                ResourceLocation promoTexture;
                try {
                    promoTexture = ResourceLocation.parse(cfg.promoButtonTexture);
                } catch (Exception e) {
                    promoTexture = ResourceLocation.fromNamespaceAndPath("cthulib", "textures/gui/promo_button.png");
                }
                
                cthulib$cachedPromoButton = new PromoButton(
                        cfg.promoButtonX,
                        cfg.promoButtonY, 
                        cfg.promoButtonWidth,
                        cfg.promoButtonHeight,
                        promoImageUrl,
                        promoTexture,
                        b -> {
                            if (cfg.promoButtonUrl != null && !cfg.promoButtonUrl.isEmpty()) {
                                Util.getPlatform().openUri(cfg.promoButtonUrl);
                            }
                        },
                        Component.literal(cfg.promoButtonTooltip),
                        cfg.promoButtonScaleWithScreen,
                        cfg.promoButtonMinScale,
                        cfg.promoButtonMaxScale,
                        cfg.promoButtonStartFromCenterX,
                        cfg.promoButtonStartFromCenterY,
                        cfg.promoButtonStartFromLeftX,
                        cfg.promoButtonStartFromRightX
                );
            } else {
                cthulib$cachedPromoButton.updatePositionExternal();
            }
            this.addRenderableWidget(cthulib$cachedPromoButton);
        }

        if (cfg.showProjectSlider) {
            if (cthulib$cachedProjectSlider == null || needsRecreate) {
                float responsiveScale = cfg.sliderScale;
                if (cfg.sliderScaleWithScreen) {
                    float screenScale = Math.min(this.width / 1920.0f, this.height / 1080.0f);
                    responsiveScale = Math.max(cfg.sliderMinScale, Math.min(cfg.sliderMaxScale, cfg.sliderScale * screenScale));
                }

                int sliderWidth = (int)(cfg.sliderCardWidth * responsiveScale);
                int fullSliderWidth = sliderWidth + (int)(100 * responsiveScale);

                int sliderX;
                if (cfg.sliderUseAnchor) {
                    sliderX = (this.width / 2) - (fullSliderWidth / 2) + cfg.sliderX;
                } else if (cfg.sliderStartFromCenterX) {
                    sliderX = (this.width / 2) - (sliderWidth / 2) + (int)(cfg.sliderX * responsiveScale);
                } else if (cfg.sliderStartFromLeftX) {
                    sliderX = (int)(cfg.sliderX * responsiveScale);
                } else if (cfg.sliderStartFromRightX) {
                    sliderX = this.width - sliderWidth + (int)(cfg.sliderX * responsiveScale);
                } else {
                    int scaledX = (int)(cfg.sliderX * responsiveScale);
                    sliderX = cfg.sliderX < 0 ? this.width + scaledX : scaledX;
                }

                int sliderY;
                if (cfg.sliderUseAnchor) {
                    sliderY = this.height / 4 + 48 + cfg.sliderY;
                } else if (cfg.sliderScaleWithScreen) {
                    sliderY = cfg.sliderY < 0 ? this.height + (int)(cfg.sliderY * responsiveScale) : (int)(cfg.sliderY * responsiveScale);
                } else {
                    sliderY = cfg.sliderY < 0 ? this.height + cfg.sliderY : cfg.sliderY;
                }

                cthulib$cachedProjectSlider = new ProjectSlider(sliderX, sliderY, cfg.sliderCardWidth, cfg.sliderCardHeight, 
                        cfg.sliderAutoSlide, cfg.sliderAutoSlideDelay, cfg.sliderShowPreviews, cfg.sliderPreviewScale, cfg.sliderPreviewOffset, responsiveScale);
            }
            this.addRenderableWidget(cthulib$cachedProjectSlider);
        }

        String projectId = ChangelogCache.getProjectId();
        if (cfg.showChangelogButton && projectId != null && !projectId.isEmpty()) {
            if (cthulib$cachedChangelogButton == null || needsRecreate) {
                int changelogButtonX = cfg.changelogButtonX < 0 ? this.width + cfg.changelogButtonX : cfg.changelogButtonX;
                int changelogButtonY = cfg.changelogButtonY < 0 ? this.height + cfg.changelogButtonY : cfg.changelogButtonY;
                
                cthulib$cachedChangelogButton = new ChangelogButton(changelogButtonX, changelogButtonY, 
                        cfg.changelogButtonWidth, cfg.changelogButtonHeight, cfg.changelogButtonTexture,
                        btn -> cthulib$toggleChangelogOverlay());
            } else {
                int changelogButtonX = cfg.changelogButtonX < 0 ? this.width + cfg.changelogButtonX : cfg.changelogButtonX;
                int changelogButtonY = cfg.changelogButtonY < 0 ? this.height + cfg.changelogButtonY : cfg.changelogButtonY;
                cthulib$cachedChangelogButton.setX(changelogButtonX);
                cthulib$cachedChangelogButton.setY(changelogButtonY);
            }
            this.addRenderableWidget(cthulib$cachedChangelogButton);
        }
    }
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (cthulib$pendingRebuild) {
            cthulib$pendingRebuild = false;
            cthulib$rebuildProjectSlider();
        }
        if (DevModeManager.checkDevModeKeybind()) {
            DevModeManager.toggleDevMode();
        }
    }

    @Unique
    private void cthulib$rebuildProjectSlider() {
        if (cthulib$cachedProjectSlider != null) {
            this.removeWidget(cthulib$cachedProjectSlider);
            cthulib$cachedProjectSlider = null;
        }
        CthuLibConfig cfg = CthuLibConfig.get();
        if (!cfg.showProjectSlider) return;

        float responsiveScale = cfg.sliderScale;
        if (cfg.sliderScaleWithScreen) {
            float screenScale = Math.min(this.width / 1920.0f, this.height / 1080.0f);
            responsiveScale = Math.max(cfg.sliderMinScale, Math.min(cfg.sliderMaxScale, cfg.sliderScale * screenScale));
        }

        int sliderWidth = (int)(cfg.sliderCardWidth * responsiveScale);
        int fullSliderWidth = sliderWidth + (int)(100 * responsiveScale);
        int sliderX;
        if (cfg.sliderUseAnchor) {
            sliderX = (this.width / 2) - (fullSliderWidth / 2) + cfg.sliderX;
        } else if (cfg.sliderStartFromCenterX) {
            sliderX = (this.width / 2) - (sliderWidth / 2) + (int)(cfg.sliderX * responsiveScale);
        } else if (cfg.sliderStartFromLeftX) {
            sliderX = (int)(cfg.sliderX * responsiveScale);
        } else if (cfg.sliderStartFromRightX) {
            sliderX = this.width - sliderWidth + (int)(cfg.sliderX * responsiveScale);
        } else {
            int scaledX = (int)(cfg.sliderX * responsiveScale);
            sliderX = cfg.sliderX < 0 ? this.width + scaledX : scaledX;
        }
        int sliderY;
        if (cfg.sliderUseAnchor) {
            sliderY = this.height / 4 + 48 + cfg.sliderY;
        } else if (cfg.sliderScaleWithScreen) {
            sliderY = cfg.sliderY < 0 ? this.height + (int)(cfg.sliderY * responsiveScale) : (int)(cfg.sliderY * responsiveScale);
        } else {
            sliderY = cfg.sliderY < 0 ? this.height + cfg.sliderY : cfg.sliderY;
        }

        cthulib$cachedProjectSlider = new ProjectSlider(sliderX, sliderY, cfg.sliderCardWidth, cfg.sliderCardHeight,
                cfg.sliderAutoSlide, cfg.sliderAutoSlideDelay, cfg.sliderShowPreviews, cfg.sliderPreviewScale, cfg.sliderPreviewOffset, responsiveScale);
        this.addRenderableWidget(cthulib$cachedProjectSlider);
        CthuLibConfig.clearCacheInvalidationFlag();
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (DevModeManager.isDevModeEnabled()) {
            Font font = Minecraft.getInstance().font;
            String devText = "Dev GUI Mode";
            int textWidth = font.width(devText);
            int x = this.width - textWidth - 10;
            int y = 5;
            
            graphics.fill(x - 4, y - 2, x + textWidth + 4, y + font.lineHeight + 2, 0xCC000000);
            
            graphics.fill(x - 4, y - 2, x + textWidth + 4, y - 1, 0xFFFFDD66);
            graphics.fill(x - 4, y + font.lineHeight + 1, x + textWidth + 4, y + font.lineHeight + 2, 0xFFFFDD66);
            graphics.fill(x - 4, y - 2, x - 3, y + font.lineHeight + 2, 0xFFFFDD66);
            graphics.fill(x + textWidth + 3, y - 2, x + textWidth + 4, y + font.lineHeight + 2, 0xFFFFDD66);
            
            graphics.drawString(font, devText, x, y, 0xFFFFDD66, false);
        }
        
        if (cthulib$changelogOpen && cthulib$changelogOverlay != null) {
            cthulib$changelogOverlay.renderAsOverlay(graphics, mouseX, mouseY, partialTick);
        }
        
        if (cthulib$quickConfigPanel != null && cthulib$quickConfigPanel.isVisible()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            cthulib$quickConfigPanel.render(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (cthulib$quickConfigPanel != null && cthulib$quickConfigPanel.isVisible()) {
            if (cthulib$quickConfigPanel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (cthulib$changelogOpen && keyCode == 256) {
            cthulib$closeChangelogOverlay();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (cthulib$quickConfigPanel != null && cthulib$quickConfigPanel.isVisible()) {
            if (cthulib$quickConfigPanel.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && Screen.hasAltDown() && DevModeManager.isDevModeEnabled()) {
            CthuLibConfig cfg = CthuLibConfig.getInstance();

            if (cthulib$cachedBrightnessButton != null &&
                cthulib$cachedBrightnessButton.isMouseOver(mouseX, mouseY)) {
                cthulib$quickConfigPanel = new QuickConfigPanel("Modlist Button", (int)mouseX, (int)mouseY, null);
                cthulib$quickConfigPanel.show();
                return true;
            }

            if (cfg.showPromoButton && cthulib$cachedPromoButton != null &&
                cthulib$cachedPromoButton.isMouseOver(mouseX, mouseY)) {
                cthulib$quickConfigPanel = new QuickConfigPanel("Promo Button", (int)mouseX, (int)mouseY, null);
                cthulib$quickConfigPanel.show();
                return true;
            }
            
            if (cfg.showProjectSlider && cthulib$cachedProjectSlider != null && 
                cthulib$cachedProjectSlider.isMouseOver(mouseX, mouseY)) {
                cthulib$quickConfigPanel = new QuickConfigPanel("Project Slider", (int)mouseX, (int)mouseY, () -> cthulib$pendingRebuild = true);
                cthulib$quickConfigPanel.show();
                return true;
            }
            
            if (cfg.showChangelogButton && cthulib$cachedChangelogButton != null && 
                cthulib$cachedChangelogButton.isMouseOver(mouseX, mouseY)) {
                cthulib$quickConfigPanel = new QuickConfigPanel("Changelog Button", (int)mouseX, (int)mouseY, null);
                cthulib$quickConfigPanel.show();
                return true;
            }
        }
        
        if (cthulib$quickConfigPanel != null && cthulib$quickConfigPanel.isVisible()) {
            if (cthulib$quickConfigPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        if (cthulib$changelogOpen && cthulib$changelogOverlay != null) {
            if (cthulib$changelogOverlay.isClickOutsidePanel(mouseX, mouseY)) {
                cthulib$closeChangelogOverlay();
                return true;
            } else {
                return cthulib$changelogOverlay.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (cthulib$quickConfigPanel != null && cthulib$quickConfigPanel.isVisible()) {
            if (cthulib$quickConfigPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        
        if (cthulib$changelogOpen && cthulib$changelogOverlay != null) {
            return cthulib$changelogOverlay.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (cthulib$quickConfigPanel != null) {
            cthulib$quickConfigPanel.mouseReleased(mouseX, mouseY, button);
        }
        
        if (cthulib$changelogOpen && cthulib$changelogOverlay != null) {
            return cthulib$changelogOverlay.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (cthulib$quickConfigPanel != null && cthulib$quickConfigPanel.isVisible()) {
            if (cthulib$quickConfigPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        
        if (cthulib$changelogOpen && cthulib$changelogOverlay != null) {
            cthulib$changelogOverlay.handleScroll(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Unique
    private void cthulib$toggleChangelogOverlay() {
        if (cthulib$changelogOpen) {
            cthulib$closeChangelogOverlay();
        } else {
            cthulib$openChangelogOverlay();
        }
    }
    
    @Unique
    private void cthulib$openChangelogOverlay() {
        if (cthulib$changelogOverlay == null) {
            cthulib$changelogOverlay = new ChangelogViewer(this);
        }
        cthulib$changelogOverlay.init(Minecraft.getInstance(), this.width, this.height);
        cthulib$changelogOverlay.resetAnimation();
        cthulib$changelogOpen = true;

        String projectId = ChangelogCache.getProjectId();
        ChangelogCache.markChangelogAsRead(projectId);
    }
    
    @Unique
    private void cthulib$closeChangelogOverlay() {
        cthulib$changelogOpen = false;
    }
}