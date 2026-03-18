package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.client.base.DraggableTitleScreenWidget;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectSlider extends DraggableTitleScreenWidget {
    private final List<ProjectData> allProjects;
    private List<ProjectData> filteredProjects;
    private int currentIndex = 0;
    private ProjectType currentFilter = ProjectType.ALL;
    
    private float autoSlideTimer = 0.0f;
    private float filterButtonHover = 0.0f;
    private float cardHoverAnimation = 0.0f;
    private float containerSlideIn = 0.0f;
    private float fadeIn = 0.0f;
    private float leftArrowHover = 0.0f;
    private float rightArrowHover = 0.0f;
    private float arrowPulseTimer = 0.0f;
    private float slideTransition = 0.0f;
    private int slideDirection = 0;
    
    private final int cardWidth;
    private final int cardHeight;
    private final float autoSlideInterval;
    private final float scale;
    
    private static final int BASE_FILTER_BUTTON_WIDTH = 70;
    private static final int BASE_FILTER_BUTTON_HEIGHT = 18;
    private static final int BASE_HEADER_HEIGHT = 16;
    private static final int BASE_LOGO_SIZE = 48;
    private static final int BASE_CARD_PADDING = 6;
    private static final int BASE_ARROW_SIZE = 32;
    
    private final int filterButtonWidth;
    private final int filterButtonHeight;
    private final int headerHeight;
    private final int logoSize;
    private final int cardPadding;
    private final int arrowSize;
    
    private int headerY;
    private int filterButtonX;
    private int filterButtonY;
    private int cardX;
    private int cardY;
    private int leftArrowX;
    private int leftArrowY;
    private int rightArrowX;
    private int rightArrowY;
    
    private boolean autoSlideEnabled = true;

    public ProjectSlider(int x, int y, int cardWidth, int cardHeight, boolean autoSlide, float autoSlideDelay, boolean showPreviews, float previewScale, int previewOffset, float scale) {
        super(x, y, (int)(cardWidth * scale) + (int)(100 * scale), (int)((cardHeight + 55) * scale), Component.empty(), UIElementPositionManager.ElementType.PROJECT_SLIDER);
        this.cardWidth = (int)(cardWidth * scale);
        this.cardHeight = (int)(cardHeight * scale);
        this.autoSlideEnabled = autoSlide;
        this.autoSlideInterval = autoSlideDelay * 20.0f;
        this.scale = scale;
        
        this.filterButtonWidth = (int)(BASE_FILTER_BUTTON_WIDTH * scale);
        this.filterButtonHeight = (int)(BASE_FILTER_BUTTON_HEIGHT * scale);
        this.headerHeight = (int)(BASE_HEADER_HEIGHT * scale);
        this.logoSize = (int)(BASE_LOGO_SIZE * scale);
        this.cardPadding = (int)(BASE_CARD_PADDING * scale);
        this.arrowSize = (int)(BASE_ARROW_SIZE * scale);
        
        this.allProjects = ProjectData.getCachedProjects();
        try {
            this.currentFilter = ProjectType.valueOf(CthuLibConfig.getInstance().sliderDefaultFilter.toUpperCase());
        } catch (Exception ignored) {
            this.currentFilter = ProjectType.ALL;
        }
        applyFilter();
        this.containerSlideIn = 0.0f;
        this.fadeIn = 0.0f;
        updatePositions();
    }

    private void updatePositions() {
        CthuLibConfig cfg = CthuLibConfig.getInstance();
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        if (!isDragging()) {
            float responsiveScale = cfg.sliderScale;
            if (cfg.sliderScaleWithScreen) {
                float screenScaleFactor = Math.min(screenWidth / 1920.0f, screenHeight / 1080.0f);
                responsiveScale = Math.max(cfg.sliderMinScale, Math.min(cfg.sliderMaxScale, cfg.sliderScale * screenScaleFactor));
            }

            int sliderWidthCalc = (int)(cfg.sliderCardWidth * responsiveScale);

            boolean useAnchor = cfg.sliderUseAnchor;
            int newX;
            if (useAnchor) {
                newX = (screenWidth / 2) - (this.width / 2) + cfg.sliderX;
            } else if (cfg.sliderStartFromCenterX) {
                newX = (screenWidth / 2) - (sliderWidthCalc / 2) + (int)(cfg.sliderX * responsiveScale);
            } else if (cfg.sliderStartFromLeftX) {
                newX = (int)(cfg.sliderX * responsiveScale);
            } else if (cfg.sliderStartFromRightX) {
                newX = screenWidth - sliderWidthCalc + (int)(cfg.sliderX * responsiveScale);
            } else {
                int scaledXVal = (int)(cfg.sliderX * responsiveScale);
                newX = cfg.sliderX < 0 ? screenWidth + scaledXVal : scaledXVal;
            }

            int newY;
            if (useAnchor) {
                newY = screenHeight / 4 + 48 + cfg.sliderY;
            } else if (cfg.sliderScaleWithScreen) {
                newY = cfg.sliderY < 0 ? screenHeight + (int)(cfg.sliderY * responsiveScale) : (int)(cfg.sliderY * responsiveScale);
            } else {
                newY = cfg.sliderY < 0 ? screenHeight + cfg.sliderY : cfg.sliderY;
            }

            this.setX(newX);
            this.setY(newY);
        }

        int curX = this.getX();
        int curY = this.getY();
        int centerX = curX + this.width / 2;

        headerY = curY;
        int filterButtonBaseY = curY + headerHeight + (int)(5 * scale);
        filterButtonX = centerX - filterButtonWidth / 2 + cfg.sliderFilterOffsetX;
        filterButtonY = filterButtonBaseY + cfg.sliderFilterOffsetY;
        cardX = centerX - cardWidth / 2;

        if (!cfg.sliderShowFilterButton) {
            cardY = curY + headerHeight + (int)(5 * scale);
        } else {
            cardY = filterButtonBaseY + filterButtonHeight + (int)(10 * scale);
        }
        leftArrowX = cardX - arrowSize - (int)(15 * scale);
        leftArrowY = cardY + (cardHeight - arrowSize) / 2;
        rightArrowX = cardX + cardWidth + (int)(15 * scale);
        rightArrowY = cardY + (cardHeight - arrowSize) / 2;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging()) {
            if (!UIElementPositionManager.hasMoved(UIElementPositionManager.ElementType.PROJECT_SLIDER)) {
                UIElementPositionManager.resetDragState(UIElementPositionManager.ElementType.PROJECT_SLIDER);
                return true;
            }
            CthuLibConfig cfg = CthuLibConfig.getInstance();
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            int screenX = this.getX();
            int screenY = this.getY();

            float responsiveScale = cfg.sliderScale;
            if (cfg.sliderScaleWithScreen) {
                float sf = Math.min(screenWidth / 1920.0f, screenHeight / 1080.0f);
                responsiveScale = Math.max(cfg.sliderMinScale, Math.min(cfg.sliderMaxScale, cfg.sliderScale * sf));
            }

            boolean useAnchor = cfg.sliderUseAnchor;
            float rs = responsiveScale > 0 ? responsiveScale : 1f;
            int configX;
            if (useAnchor) {
                configX = screenX - (screenWidth / 2) + (this.width / 2);
            } else if (cfg.sliderStartFromCenterX) {
                int halfCard = (int)(cfg.sliderCardWidth * responsiveScale / 2);
                int guiOffset = screenX - (screenWidth / 2) + halfCard;
                configX = (int)(guiOffset / rs);
            } else if (cfg.sliderStartFromLeftX) {
                configX = (int)(screenX / rs);
            } else if (cfg.sliderStartFromRightX) {
                int cardW = (int)(cfg.sliderCardWidth * responsiveScale);
                int guiOffset = screenX - screenWidth + cardW;
                configX = (int)(guiOffset / rs);
            } else {
                if (screenX < screenWidth / 2) {
                    configX = (int)(screenX / rs);
                } else {
                    int guiOffset = screenX - screenWidth;
                    configX = (int)(guiOffset / rs);
                }
            }

            int configY;
            if (useAnchor) {
                configY = screenY - (screenHeight / 4) - 48;
            } else if (cfg.sliderScaleWithScreen) {
                configY = screenY > screenHeight / 2 ? (int)((screenY - screenHeight) / rs) : (int)(screenY / rs);
            } else {
                configY = screenY > screenHeight / 2 ? screenY - screenHeight : screenY;
            }

            UIElementPositionManager.resetDragState(UIElementPositionManager.ElementType.PROJECT_SLIDER);
            cfg.sliderX = configX;
            cfg.sliderY = configY;
            cfg.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updatePositions();
        
        if (filteredProjects.isEmpty()) {
            return;
        }

        containerSlideIn = lerp(CthuLibConfig.getInstance().sliderEntryAnimationSpeed, containerSlideIn, 1.0f);
        fadeIn = lerp(CthuLibConfig.getInstance().sliderFadeInSpeed, fadeIn, 1.0f);
        if (!CthuLibConfig.getInstance().sliderEntryAnimationEnabled) { containerSlideIn = 1.0f; fadeIn = 1.0f; }
        
        if (slideDirection != 0) {
            CthuLibConfig animCfg = CthuLibConfig.getInstance();
            float step = animCfg.animationsEnabled && animCfg.sliderCardTransitionEnabled
                    ? animCfg.sliderCardTransitionSpeed : 1.0f;
            slideTransition = Math.min(1.0f, slideTransition + step);
            if (slideTransition >= 1.0f) {
                slideTransition = 0.0f;
                slideDirection = 0;
            }
        }

        if (autoSlideEnabled && slideDirection == 0) {
            autoSlideTimer += partialTick;
            if (autoSlideTimer >= autoSlideInterval) {
                nextCard();
                autoSlideTimer = 0.0f;
            }
        }

        int globalAlpha = (int)(fadeIn * 255);

        boolean doReveal = containerSlideIn < 1.0f && CthuLibConfig.getInstance().sliderEntryAnimationEnabled;
        if (doReveal) {
            int clipLeft = leftArrowX + arrowSize;
            int clipRight = Math.max(clipLeft + 1, clipLeft + (int)(containerSlideIn * (rightArrowX - clipLeft)));
            graphics.enableScissor(clipLeft, cardY - 100, clipRight, cardY + cardHeight + 100);
        }

        renderHeaderText(graphics, globalAlpha);
        if (CthuLibConfig.getInstance().sliderShowFilterButton) {
            renderFilterButton(graphics, mouseX, mouseY, globalAlpha);
        }
        renderCard(graphics, mouseX, mouseY, globalAlpha);
        renderArrows(graphics, mouseX, mouseY, globalAlpha);

        if (doReveal) {
            graphics.disableScissor();
        }
    }
    
    private void renderCard(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        if (filteredProjects.isEmpty()) return;
        
        float progress = slideDirection != 0 ? easeInOutQuad(slideTransition) : 0.0f;
        boolean hovered = isMouseOverCard(mouseX, mouseY);
        cardHoverAnimation = lerp(0.18f, cardHoverAnimation, hovered ? 1.0f : 0.0f);
        
        if (slideDirection != 0) {
            graphics.enableScissor(leftArrowX + arrowSize, cardY - 10, rightArrowX, cardY + cardHeight + 10);

            int slideDist = cardWidth + 50;
            
            int incomingIndex = currentIndex;
            int outgoingIndex;
            if (slideDirection > 0) {
                outgoingIndex = (currentIndex - 1 + filteredProjects.size()) % filteredProjects.size();
            } else {
                outgoingIndex = (currentIndex + 1) % filteredProjects.size();
            }
            
            ProjectData outgoingProject = filteredProjects.get(outgoingIndex);
            ProjectData incomingProject = filteredProjects.get(incomingIndex);
            
            int outgoingX = cardX - (int)(slideDirection * progress * slideDist);
            float outgoingFade = 1.0f - (progress * 0.7f);
            int outgoingAlpha = (int)(alpha * outgoingFade);
            if (outgoingAlpha > 5) {
                renderCardFully(graphics, outgoingX, cardY, outgoingAlpha, outgoingProject, false);
            }

            int incomingX = cardX + (int)(slideDirection * (1.0f - progress) * slideDist);
            renderCardWithGradientFade(graphics, incomingX, cardY, alpha, alpha, incomingProject, progress, hovered);

            graphics.disableScissor();
        } else {
            ProjectData project = filteredProjects.get(currentIndex);
            renderCardFully(graphics, cardX, cardY, alpha, project, hovered);
        }
    }
    
    private void renderCardWithGradientFade(GuiGraphics graphics, int renderX, int renderY, int baseAlpha, int transitionAlpha, ProjectData project, float progress, boolean hovered) {
        int slices = 4;
        int sliceWidth = cardWidth / slices;
        
        for (int i = 0; i < slices; i++) {
            float slicePosition = (float)i / slices;
            
            float fadeStart = slideDirection > 0 ? slicePosition : (1.0f - slicePosition);
            float revealProgress = Math.max(0, Math.min(1, (progress - fadeStart * 0.4f) / 0.6f));
            
            float sliceFade = 0.2f + (revealProgress * 0.8f);
            int sliceAlpha = (int)(baseAlpha * sliceFade);
            
            if (sliceAlpha > 5) {
                int sliceX = renderX + (i * sliceWidth);
                int sliceW = (i == slices - 1) ? (cardWidth - i * sliceWidth) : sliceWidth;
                
                graphics.enableScissor(sliceX, renderY - 20, sliceX + sliceW, renderY + cardHeight + 20);
                renderCardFully(graphics, renderX, renderY, sliceAlpha, project, hovered);
                graphics.disableScissor();
            }
        }
    }
    
    private void renderCardFully(GuiGraphics graphics, int renderX, int renderY, int transitionAlpha, ProjectData project, boolean hovered) {
        if (cardHoverAnimation > 0.01f) {
            int glowRadius = (int)(8 * cardHoverAnimation);
            for (int i = glowRadius; i > 0; i--) {
                float glowProgress = 1.0f - ((float)i / glowRadius);
                int glowAlpha = (int)(transitionAlpha * 0.15f * glowProgress * cardHoverAnimation);
                int glowColor = (glowAlpha << 24) | 0xFFDD66;
                graphics.fill(renderX - i, renderY - i, renderX + cardWidth + i, renderY - i + 1, glowColor);
                graphics.fill(renderX - i, renderY + cardHeight + i, renderX + cardWidth + i, renderY + cardHeight + i + 1, glowColor);
                graphics.fill(renderX - i, renderY - i, renderX - i + 1, renderY + cardHeight + i, glowColor);
                graphics.fill(renderX + cardWidth + i, renderY - i, renderX + cardWidth + i + 1, renderY + cardHeight + i, glowColor);
            }
        }

        int bgAlpha = (int)(transitionAlpha * 0.95f);
        int topBrightness = 0x28 + (int)(cardHoverAnimation * 0x0C);
        int bottomBrightness = 0x1C + (int)(cardHoverAnimation * 0x08);
        
        int topColor = (bgAlpha << 24) | (topBrightness * 0x010101) | 0x00050A;
        int bottomColor = (bgAlpha << 24) | (bottomBrightness * 0x010101);
        
        int gradientSteps = 12;
        int stepHeight = cardHeight / gradientSteps;
        for (int i = 0; i < gradientSteps; i++) {
            float gradientProgress = (float)i / gradientSteps;
            
            int r = (int)((topBrightness + (bottomBrightness - topBrightness) * gradientProgress));
            int g = r;
            int b = (int)(r + 5 * (1.0f - gradientProgress));
            
            int rowColor = (bgAlpha << 24) | (r << 16) | (g << 8) | b;
            int y1 = renderY + i * stepHeight;
            int y2 = (i == gradientSteps - 1) ? renderY + cardHeight : y1 + stepHeight;
            graphics.fill(renderX, y1, renderX + cardWidth, y2, rowColor);
        }
        
        int shineHeight = (int)(cardHeight * 0.3f);
        for (int i = 0; i < shineHeight; i++) {
            float shineProgress = (float)i / shineHeight;
            int shineAlpha = (int)(transitionAlpha * 0.08f * (1.0f - shineProgress));
            int shineColor = (shineAlpha << 24) | 0xFFFFFF;
            graphics.fill(renderX + 1, renderY + i + 1, renderX + cardWidth - 1, renderY + i + 2, shineColor);
        }
        
        if (cardHoverAnimation > 0.01f) {
            int glowAlpha = (int)(transitionAlpha * cardHoverAnimation * 0.5f);
            int accentColor1 = 0xFFDD66;
            int accentColor2 = 0xFF8833;
            
            int borderWidth = Math.max(2, (int)(3 * scale));
            
            for (int i = 0; i < borderWidth; i++) {
                float gradProgress = (float)i / borderWidth;
                int layerAlpha = (int)(glowAlpha * (1.0f - gradProgress * 0.5f));
                int color = (layerAlpha << 24) | accentColor1;
                graphics.fill(renderX, renderY + i, renderX + cardWidth, renderY + i + 1, color);
            }
            
            for (int i = 0; i < borderWidth; i++) {
                int layerAlpha = (int)(glowAlpha * (1.0f - (float)i / borderWidth * 0.5f));
                int color = (layerAlpha << 24) | accentColor1;
                graphics.fill(renderX + i, renderY, renderX + i + 1, renderY + cardHeight, color);
            }
            
            for (int i = 0; i < borderWidth; i++) {
                int layerAlpha = (int)(glowAlpha * (1.0f - (float)i / borderWidth * 0.5f));
                int color = (layerAlpha << 24) | accentColor2;
                graphics.fill(renderX + cardWidth - borderWidth + i, renderY, renderX + cardWidth - borderWidth + i + 1, renderY + cardHeight, color);
            }
            
            for (int i = 0; i < borderWidth; i++) {
                float gradProgress = (float)i / borderWidth;
                int layerAlpha = (int)(glowAlpha * (1.0f - gradProgress * 0.5f));
                int color = (layerAlpha << 24) | accentColor2;
                graphics.fill(renderX, renderY + cardHeight - borderWidth + i, renderX + cardWidth, renderY + cardHeight - borderWidth + i + 1, color);
            }
            
            int glowRadius = Math.min(4, (int)(4 * cardHoverAnimation));
            for (int i = 0; i < glowRadius; i++) {
                int glowLayerAlpha = (int)(transitionAlpha * cardHoverAnimation * 0.08f * (1.0f - (float)i / glowRadius));
                int glowColor = (glowLayerAlpha << 24) | accentColor1;

                graphics.fill(renderX - i, renderY - i, renderX + cardWidth + i, renderY - i + 1, glowColor);
                graphics.fill(renderX - i, renderY + cardHeight + i - 1, renderX + cardWidth + i, renderY + cardHeight + i, glowColor);
                graphics.fill(renderX - i, renderY - i, renderX - i + 1, renderY + cardHeight + i, glowColor);
                graphics.fill(renderX + cardWidth + i - 1, renderY - i, renderX + cardWidth + i, renderY + cardHeight + i, glowColor);
            }
        }

        int cornerLength = (int)(12 * scale);
        int cornerThickness = Math.max(1, (int)(2 * scale));
        int cornerAlpha = (int)(transitionAlpha * 0.7f);
        int cornerColor = (cornerAlpha << 24) | 0x4AD9FF;
        
        graphics.fill(renderX, renderY, renderX + cornerLength, renderY + cornerThickness, cornerColor);
        graphics.fill(renderX, renderY, renderX + cornerThickness, renderY + cornerLength, cornerColor);
        
        graphics.fill(renderX + cardWidth - cornerLength, renderY, renderX + cardWidth, renderY + cornerThickness, cornerColor);
        graphics.fill(renderX + cardWidth - cornerThickness, renderY, renderX + cardWidth, renderY + cornerLength, cornerColor);
        
        graphics.fill(renderX, renderY + cardHeight - cornerThickness, renderX + cornerLength, renderY + cardHeight, cornerColor);
        graphics.fill(renderX, renderY + cardHeight - cornerLength, renderX + cornerThickness, renderY + cardHeight, cornerColor);
        
        graphics.fill(renderX + cardWidth - cornerLength, renderY + cardHeight - cornerThickness, renderX + cardWidth, renderY + cardHeight, cornerColor);
        graphics.fill(renderX + cardWidth - cornerThickness, renderY + cardHeight - cornerLength, renderX + cardWidth, renderY + cardHeight, cornerColor);

        int borderAlpha = (int)(transitionAlpha * 0.5f);
        int borderColor = (borderAlpha << 24) | 0x888888;
        graphics.fill(renderX, renderY, renderX + cardWidth, renderY + 1, borderColor);
        graphics.fill(renderX, renderY + cardHeight - 1, renderX + cardWidth, renderY + cardHeight, borderColor);
        graphics.fill(renderX, renderY, renderX + 1, renderY + cardHeight, borderColor);
        graphics.fill(renderX + cardWidth - 1, renderY, renderX + cardWidth, renderY + cardHeight, borderColor);

        int logoX = renderX + cardPadding;
        int logoY = renderY + (cardHeight - logoSize) / 2;
        
        int logoBgAlpha = (int)(transitionAlpha * 0.95f);
        int logoBgColor = (logoBgAlpha << 24) | 0x1A1A1A;
        graphics.fill(logoX - 2, logoY - 2, logoX + logoSize + 2, logoY + logoSize + 2, logoBgColor);
        
        renderLogoForProject(graphics, project, logoX, logoY, logoSize, transitionAlpha);

        Font font = Minecraft.getInstance().font;
        int textX = logoX + logoSize + cardPadding;
        int textY = renderY + cardPadding;

        PoseStack pose = graphics.pose();
        
        int availableTextWidth = cardWidth - logoSize - cardPadding * 3;
        int maxTextWidth = Math.max(20, (int)(availableTextWidth / scale));
        
        String title = project.getName();
        if (font.width(title) > maxTextWidth) {
            while (font.width(title + "...") > maxTextWidth && title.length() > 1) {
                title = title.substring(0, title.length() - 1);
            }
            if (title.length() > 0) title = title + "...";
        }
        
        int titleColor = (transitionAlpha << 24) | 0xFFFFFF;
        int titleShadow = ((transitionAlpha / 2) << 24) | 0x000000;
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(font, title, (int)(textX / scale) + 2, (int)(textY / scale) + 2, titleShadow, false);
        graphics.drawString(font, title, (int)(textX / scale), (int)(textY / scale), titleColor, false);
        pose.popPose();

        int lineHeight = Math.max(3, (int)(font.lineHeight * scale));
        textY += lineHeight + Math.max(1, (int)(2 * scale));
        
        String versions = String.join(", ", project.getSupportedVersions());
        if (font.width(versions) > maxTextWidth) {
            List<String> versionList = project.getSupportedVersions();
            versions = versionList.get(0);
            if (versionList.size() > 1) {
                versions += " +" + (versionList.size() - 1);
            }
        }
        int versionColor = (transitionAlpha << 24) | 0xB8C8D8;
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(font, versions, (int)(textX / scale), (int)(textY / scale), versionColor, false);
        pose.popPose();

        textY += lineHeight + Math.max(1, (int)(1 * scale));
        String loaders = String.join(", ", project.getLoaders());
        if (font.width(loaders) > maxTextWidth) {
            List<String> loaderList = project.getLoaders();
            loaders = loaderList.get(0);
            if (loaderList.size() > 1) {
                loaders += " +" + (loaderList.size() - 1);
            }
        }
        int loaderColor = (transitionAlpha << 24) | 0x95B875;
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(font, loaders, (int)(textX / scale), (int)(textY / scale), loaderColor, false);
        pose.popPose();

        String author = project.getAuthors().isEmpty() ? "by Unknown" : "by " + project.getAuthors().get(0);
        float authorScale = Math.max(0.3f, scale * 0.9f);
        int authorWidth = (int)(font.width(author) * authorScale);
        int authorX = renderX + cardWidth - authorWidth - cardPadding;
        int authorY = renderY + cardHeight - (int)(font.lineHeight * authorScale) - cardPadding;
        int authorColor = (int)(transitionAlpha * 0.85f) << 24 | 0xA5A5A5;
        pose.pushPose();
        pose.scale(authorScale, authorScale, 1.0f);
        graphics.drawString(font, author, (int)(authorX / authorScale), (int)(authorY / authorScale), authorColor, false);
        pose.popPose();

        List<String> unmaintainedList = parseCommaSeparated(CthuLibConfig.getInstance().sliderUnmaintainedProjects);
        if (!unmaintainedList.isEmpty() && unmaintainedList.stream().anyMatch(u ->
                u.equalsIgnoreCase(project.getName()) || u.equalsIgnoreCase(project.getProjectId()))) {
            String badgeText = "UNMAINTAINED";
            int badgeW = (int)(font.width(badgeText) * scale) + (int)(6 * scale);
            int badgeH = (int)(font.lineHeight * scale) + (int)(4 * scale);
            int badgeX = renderX + cardWidth - badgeW - (int)(3 * scale);
            int badgeY = renderY + (int)(3 * scale);
            graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, ((int)(transitionAlpha * 0.85f) << 24) | 0x882222);
            pose.pushPose();
            pose.scale(scale, scale, 1.0f);
            graphics.drawString(font, badgeText, (int)((badgeX + 3 * scale) / scale), (int)((badgeY + 2 * scale) / scale), (transitionAlpha << 24) | 0xFF7777, false);
            pose.popPose();
        }

        if (hovered && cardHoverAnimation > 0.05f) {
            int hoverAlpha = (int)(cardHoverAnimation * transitionAlpha * 0.18f);
            int hoverOverlay = (hoverAlpha << 24) | 0xFFFFFF;
            graphics.fill(renderX + 1, renderY + 1, renderX + cardWidth - 1, renderY + cardHeight - 1, hoverOverlay);
        }
    }
    
    private void renderArrows(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        boolean leftHovered = isMouseOverLeftArrow(mouseX, mouseY);
        boolean rightHovered = isMouseOverRightArrow(mouseX, mouseY);
        
        leftArrowHover = lerp(0.2f, leftArrowHover, leftHovered ? 1.0f : 0.0f);
        rightArrowHover = lerp(0.2f, rightArrowHover, rightHovered ? 1.0f : 0.0f);
        arrowPulseTimer += 0.04f;
        float idlePulse = (float)(Math.sin(arrowPulseTimer) * 0.5 + 0.5);
        
        renderArrow(graphics, leftArrowX, leftArrowY, arrowSize, alpha, leftArrowHover, true, idlePulse);
        renderArrow(graphics, rightArrowX, rightArrowY, arrowSize, alpha, rightArrowHover, false, idlePulse);
    }
    
    private void renderArrow(GuiGraphics graphics, int x, int y, int size, int alpha, float hover, boolean pointLeft, float idlePulse) {
        CthuLibConfig arrowCfg = CthuLibConfig.getInstance();
        int idleColorRgb = parseHexColor(arrowCfg.sliderArrowIdleColor, 0x4ADBFF);
        int hoverColorRgb = parseHexColor(arrowCfg.sliderArrowHoverColor, 0xFFAA33);
        {
            int idleRadius = 3 + (int)(idlePulse * 3);
            for (int i = idleRadius; i > 0; i--) {
                float falloff = (float)(idleRadius - i + 1) / (idleRadius + 1);
                int gAlpha = (int)(alpha * 0.14f * falloff * (0.4f + idlePulse * 0.6f) * (1.0f - hover * 0.9f));
                if (gAlpha < 2) continue;
                int gColor = (gAlpha << 24) | idleColorRgb;
                graphics.fill(x - i, y - i, x + size + i, y - i + 1, gColor);
                graphics.fill(x - i, y + size + i - 1, x + size + i, y + size + i, gColor);
                graphics.fill(x - i, y - i, x - i + 1, y + size + i, gColor);
                graphics.fill(x + size + i - 1, y - i, x + size + i, y + size + i, gColor);
            }
        }

        if (hover > 0.01f) {
            int glowRadius = (int)(6 * hover);
            for (int i = 0; i < glowRadius; i++) {
                int glowAlpha = (int)(alpha * hover * 0.2f * (1.0f - (float)i / glowRadius));
                int glowColor = (glowAlpha << 24) | hoverColorRgb;
                graphics.fill(x - i, y - i, x + size + i, y - i + 1, glowColor);
                graphics.fill(x - i, y + size + i - 1, x + size + i, y + size + i, glowColor);
                graphics.fill(x - i, y - i, x - i + 1, y + size + i, glowColor);
                graphics.fill(x + size + i - 1, y - i, x + size + i, y + size + i, glowColor);
            }
        }
        
        int bgAlpha = (int)(alpha * (0.65f + idlePulse * 0.08f + hover * 0.2f));
        int topBrightness = 0x36 + (int)(idlePulse * 0x08) + (int)(hover * 0x15);
        int bottomBrightness = 0x28 + (int)(idlePulse * 0x06) + (int)(hover * 0x0A);
        int gradientSteps = 6;
        int stepHeight = size / gradientSteps;
        for (int i = 0; i < gradientSteps; i++) {
            float gradientProgress = (float)i / gradientSteps;
            int brightness = (int)(topBrightness + (bottomBrightness - topBrightness) * gradientProgress);
            int rowColor = (bgAlpha << 24) | (brightness * 0x010101);
            int y1 = y + i * stepHeight;
            int y2 = (i == gradientSteps - 1) ? y + size : y1 + stepHeight;
            graphics.fill(x, y1, x + size, y2, rowColor);
        }
        
        if (hover > 0.01f) {
            int glowAlpha = (int)(alpha * hover * 0.6f);
            int accentColor1 = hoverColorRgb;
            int accentColor2 = hoverColorRgb;
            int borderWidth = Math.max(1, (int)(2 * hover));
            
            for (int i = 0; i < borderWidth; i++) {
                int color = (glowAlpha << 24) | accentColor1;
                graphics.fill(x, y + i, x + size, y + i + 1, color);
            }
            
            for (int i = 0; i < borderWidth; i++) {
                int color = (glowAlpha << 24) | accentColor1;
                graphics.fill(x + i, y, x + i + 1, y + size, color);
            }
            
            for (int i = 0; i < borderWidth; i++) {
                int color = (glowAlpha << 24) | accentColor2;
                graphics.fill(x, y + size - borderWidth + i, x + size, y + size - borderWidth + i + 1, color);
            }
            
            for (int i = 0; i < borderWidth; i++) {
                int color = (glowAlpha << 24) | accentColor2;
                graphics.fill(x + size - borderWidth + i, y, x + size - borderWidth + i + 1, y + size, color);
            }
        }
        
        float idleBorderStrength = 0.50f + idlePulse * 0.30f;
        int idleBorderAlpha = (int)(alpha * idleBorderStrength * (1.0f - hover * 0.85f));
        if (idleBorderAlpha > 3) {
            int cyanBorder = (idleBorderAlpha << 24) | idleColorRgb;
            graphics.fill(x, y, x + size, y + 1, cyanBorder);
            graphics.fill(x, y + size - 1, x + size, y + size, cyanBorder);
            graphics.fill(x, y, x + 1, y + size, cyanBorder);
            graphics.fill(x + size - 1, y, x + size, y + size, cyanBorder);
        }
        
        int arrowAlpha = (int)(alpha * (0.80f + idlePulse * 0.15f + hover * 0.05f));
        int arrowPadding = size / 4;
        int centerY = y + size / 2;
        int arrowHeight = size / 2 - arrowPadding;
        
        if (pointLeft) {
            int tipX = x + arrowPadding;
            int baseX = x + size - arrowPadding;
            for (int i = 0; i < arrowHeight; i++) {
                float rowProgress = (float)i / arrowHeight;
                int colorBrightness = 0xFF - (int)(rowProgress * 0x20);
                int arrowColor = (arrowAlpha << 24) | (colorBrightness * 0x010101);
                graphics.fill(tipX + i, centerY - i, baseX, centerY - i + 1, arrowColor);
                graphics.fill(tipX + i, centerY + i, baseX, centerY + i + 1, arrowColor);
            }
        } else {
            int tipX = x + size - arrowPadding;
            int baseX = x + arrowPadding;
            for (int i = 0; i < arrowHeight; i++) {
                float rowProgress = (float)i / arrowHeight;
                int colorBrightness = 0xFF - (int)(rowProgress * 0x20);
                int arrowColor = (arrowAlpha << 24) | (colorBrightness * 0x010101);
                graphics.fill(baseX, centerY - i, tipX - i, centerY - i + 1, arrowColor);
                graphics.fill(baseX, centerY + i, tipX - i, centerY + i + 1, arrowColor);
            }
        }
    }
    
    private void renderLogoForProject(GuiGraphics graphics, ProjectData project, int x, int y, int size, int alpha) {
        String iconUrl = project.getIconUrl();
        
        if (iconUrl == null || iconUrl.isEmpty()) {
            renderPlaceholderLogo(graphics, x, y, size, alpha);
            return;
        }
        
        ResourceLocation texture = ImageCache.getOrLoadImage(iconUrl, "logos", "project_logo");
        if (texture != null) {
            graphics.blit(texture, x, y, 0, 0, size, size, size, size);
            return;
        }
        
        renderPlaceholderLogo(graphics, x, y, size, alpha);
    }
    
    private void renderPlaceholderLogo(GuiGraphics graphics, int x, int y, int size, int alpha) {
        int bgAlpha = (int)(alpha * 0.3f);
        int bgColor = (bgAlpha << 24) | 0x333333;
        graphics.fill(x, y, x + size, y + size, bgColor);
        
        int iconSize = size / 2;
        int iconX = x + (size - iconSize) / 2;
        int iconY = y + (size - iconSize) / 2;
        
        int iconAlpha = (int)(alpha * 0.6f);
        int iconColor = (iconAlpha << 24) | 0x999999;
        
        graphics.fill(iconX + iconSize / 4, iconY, iconX + iconSize * 3 / 4, iconY + iconSize / 3, iconColor);
        graphics.fill(iconX, iconY + iconSize / 2, iconX + iconSize, iconY + iconSize, iconColor);
    }

    private void renderHeaderText(GuiGraphics graphics, int alpha) {
        CthuLibConfig cfg = CthuLibConfig.getInstance();

        String rawHeader = cfg.sliderHeaderText != null ? cfg.sliderHeaderText.trim() : "";
        if (rawHeader.isEmpty()) return;
        String headerText = rawHeader;

        Font font = Minecraft.getInstance().font;
        
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        
        int scaledTextWidth = font.width(headerText);
        int centerX = this.getX() + this.width / 2;
        int scaledX = (int)((centerX / scale) - (scaledTextWidth / 2.0f));
        int scaledY = (int)(headerY / scale);
        
        int shadowAlpha1 = alpha / 3;
        int shadowAlpha2 = alpha / 5;
        graphics.drawString(font, headerText, scaledX + 2, scaledY + 2, shadowAlpha1 << 24, false);
        graphics.drawString(font, headerText, scaledX + 1, scaledY + 1, shadowAlpha2 << 24, false);
        
        int textColor = (alpha << 24) | 0xFFDD99;
        graphics.drawString(font, headerText, scaledX, scaledY, textColor, false);
        
        int glowAlpha = alpha / 6;
        int glowColor = (glowAlpha << 24) | 0xFFAA66;
        graphics.drawString(font, headerText, scaledX, scaledY - 1, glowColor, false);
        
        pose.popPose();
    }

    private void renderFilterButton(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        boolean hovered = isMouseOverFilterButton(mouseX, mouseY);
        filterButtonHover = lerp(0.2f, filterButtonHover, hovered ? 1.0f : 0.0f);
        
        if (filterButtonHover > 0.01f) {
            int glowRadius = (int)(6 * filterButtonHover);
            for (int i = 0; i < glowRadius; i++) {
                int glowAlpha = (int)(alpha * 0.15f * filterButtonHover * (1.0f - (float)i / glowRadius));
                int glowColor = (glowAlpha << 24) | 0xFFAA33;
                graphics.fill(filterButtonX - i, filterButtonY - i, 
                             filterButtonX + filterButtonWidth + i, filterButtonY - i + 1, 
                             glowColor);
                graphics.fill(filterButtonX - i, filterButtonY + filterButtonHeight + i - 1, 
                             filterButtonX + filterButtonWidth + i, filterButtonY + filterButtonHeight + i, 
                             glowColor);
                graphics.fill(filterButtonX - i, filterButtonY - i, 
                             filterButtonX - i + 1, filterButtonY + filterButtonHeight + i, 
                             glowColor);
                graphics.fill(filterButtonX + filterButtonWidth + i - 1, filterButtonY - i, 
                             filterButtonX + filterButtonWidth + i, filterButtonY + filterButtonHeight + i, 
                             glowColor);
            }
        }

        int bgAlpha = (int)(alpha * 0.8f);
        int topBrightness = 0x18 + (int)(filterButtonHover * 0x12);
        int bottomBrightness = 0x10 + (int)(filterButtonHover * 0x08);
        int gradientSteps = 4;
        int stepHeight = filterButtonHeight / gradientSteps;
        for (int i = 0; i < gradientSteps; i++) {
            float gradientProgress = (float)i / gradientSteps;
            int brightness = (int)(topBrightness + (bottomBrightness - topBrightness) * gradientProgress);
            int rowColor = (bgAlpha << 24) | (brightness * 0x010101);
            int y1 = filterButtonY + i * stepHeight;
            int y2 = (i == gradientSteps - 1) ? filterButtonY + filterButtonHeight : y1 + stepHeight;
            graphics.fill(filterButtonX, y1, filterButtonX + filterButtonWidth, y2, rowColor);
        }
        
        int tintAlpha = (int)(alpha * 0.25f);
        int tintColor = 0;
        switch (currentFilter) {
            case ALL -> tintColor = 0x33DD33;
            case MODPACK -> tintColor = 0x3399FF;
            case MOD -> tintColor = 0xFF9933;
        }
        int filterTint = (tintAlpha << 24) | tintColor;
        graphics.fill(filterButtonX, filterButtonY, filterButtonX + filterButtonWidth, 
                     filterButtonY + filterButtonHeight, filterTint);

        int borderAlpha = (int)(alpha * 0.9f);
        int accentColor1 = 0xFFDD66;
        int accentColor2 = 0xFF8833;
        int borderWidth = (int)(1 + filterButtonHover * 2.0f);
        
        for (int i = 0; i < borderWidth; i++) {
            int color = (borderAlpha << 24) | accentColor1;
            graphics.fill(filterButtonX, filterButtonY + i, 
                         filterButtonX + filterButtonWidth, filterButtonY + i + 1, color);
            graphics.fill(filterButtonX + i, filterButtonY, 
                         filterButtonX + i + 1, filterButtonY + filterButtonHeight, color);
        }
        
        for (int i = 0; i < borderWidth; i++) {
            int color = (borderAlpha << 24) | accentColor2;
            graphics.fill(filterButtonX, filterButtonY + filterButtonHeight - borderWidth + i, 
                         filterButtonX + filterButtonWidth, filterButtonY + filterButtonHeight - borderWidth + i + 1, color);
            graphics.fill(filterButtonX + filterButtonWidth - borderWidth + i, filterButtonY, 
                         filterButtonX + filterButtonWidth - borderWidth + i + 1, filterButtonY + filterButtonHeight, color);
        }

        Font font = Minecraft.getInstance().font;
        String filterText = currentFilter.getDisplayName();
        int txtWidth = (int)(font.width(filterText) * scale);
        int txtX = filterButtonX + (filterButtonWidth - txtWidth) / 2;
        int txtY = filterButtonY + (filterButtonHeight - (int)(font.lineHeight * scale)) / 2;
        
        int txtColor = (alpha << 24) | 0xFFFFFF;
        int shadowColor = ((alpha / 2) << 24) | 0x000000;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(font, filterText, (int)(txtX / scale) + 1, (int)(txtY / scale) + 1, shadowColor, false);
        graphics.drawString(font, filterText, (int)(txtX / scale), (int)(txtY / scale), txtColor, false);
        pose.popPose();
    }

    @Override
    protected boolean onWidgetClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || isDragging()) return false;
        
        if (CthuLibConfig.getInstance().sliderShowFilterButton && isMouseOverFilterButton((int)mouseX, (int)mouseY)) {
            cycleFilter();
            return true;
        }
        
        if (isMouseOverLeftArrow((int)mouseX, (int)mouseY)) {
            previousCard();
            return true;
        }
        
        if (isMouseOverRightArrow((int)mouseX, (int)mouseY)) {
            nextCard();
            return true;
        }
        
        if (isMouseOverCard((int)mouseX, (int)mouseY) && !filteredProjects.isEmpty()) {
            ProjectData project = filteredProjects.get(currentIndex);
            if (project.getProjectUrl() != null && !project.getProjectUrl().isEmpty()) {
                Util.getPlatform().openUri(project.getProjectUrl());
            }
            return true;
        }

        return false;
    }
    
    @Override
    protected void onDragUpdate() {
        updatePositions();
    }
    
    @Override
    protected String getWidgetName() {
        return "Project Slider";
    }
    
    private boolean isMouseOverWidget(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width &&
               mouseY >= this.getY() && mouseY <= this.getY() + this.height;
    }

    private void cycleFilter() {
        currentFilter = switch (currentFilter) {
            case ALL -> ProjectType.MODPACK;
            case MODPACK -> ProjectType.MOD;
            case MOD -> ProjectType.ALL;
        };
        applyFilter();
    }

    private List<String> parseCommaSeparated(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isEmpty()) return result;
        for (String s : value.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    private int parseHexColor(String hex, int fallback) {
        try {
            return (int) Long.parseLong(hex.replace("#", "").trim(), 16);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void applyFilter() {
        CthuLibConfig cfg = CthuLibConfig.getInstance();
        List<String> blacklist = parseCommaSeparated(cfg.sliderBlacklistedProjects);
        List<ProjectData> base = currentFilter == ProjectType.ALL
                ? new ArrayList<>(allProjects)
                : allProjects.stream().filter(p -> p.getType() == currentFilter).collect(Collectors.toList());
        if (blacklist.isEmpty()) {
            filteredProjects = base;
        } else {
            filteredProjects = base.stream().filter(p ->
                    blacklist.stream().noneMatch(b ->
                            b.equalsIgnoreCase(p.getName()) || b.equalsIgnoreCase(p.getProjectId())))
                    .collect(Collectors.toList());
        }
        if (currentIndex >= filteredProjects.size() && !filteredProjects.isEmpty()) {
            currentIndex = 0;
        }
    }

    private void previousCard() {
        if (filteredProjects.isEmpty()) return;
        currentIndex = (currentIndex - 1 + filteredProjects.size()) % filteredProjects.size();
        slideDirection = -1;
        slideTransition = 0.0f;
        autoSlideTimer = 0.0f;
    }

    private void nextCard() {
        if (filteredProjects.isEmpty()) return;
        currentIndex = (currentIndex + 1) % filteredProjects.size();
        slideDirection = 1;
        slideTransition = 0.0f;
        autoSlideTimer = 0.0f;
    }

    public void setAutoSlideEnabled(boolean enabled) {
        this.autoSlideEnabled = enabled;
    }

    private boolean isMouseOverFilterButton(int mouseX, int mouseY) {
        return mouseX >= filterButtonX && mouseX <= filterButtonX + filterButtonWidth &&
               mouseY >= filterButtonY && mouseY <= filterButtonY + filterButtonHeight;
    }

    private boolean isMouseOverCard(int mouseX, int mouseY) {
        return mouseX >= cardX && mouseX <= cardX + cardWidth &&
               mouseY >= cardY && mouseY <= cardY + cardHeight;
    }
    
    private boolean isMouseOverLeftArrow(int mouseX, int mouseY) {
        return mouseX >= leftArrowX && mouseX <= leftArrowX + arrowSize &&
               mouseY >= leftArrowY && mouseY <= leftArrowY + arrowSize;
    }
    
    private boolean isMouseOverRightArrow(int mouseX, int mouseY) {
        return mouseX >= rightArrowX && mouseX <= rightArrowX + arrowSize &&
               mouseY >= rightArrowY && mouseY <= rightArrowY + arrowSize;
    }
}
