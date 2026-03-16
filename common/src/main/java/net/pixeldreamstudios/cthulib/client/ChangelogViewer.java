package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;
import net.pixeldreamstudios.cthulib.util.ChangelogCache;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangelogViewer extends Screen {
    
    private final Screen parent;
    private final String changelog;
    private List<MarkdownLine> lines = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int PADDING = 30;
    private static final int LINE_HEIGHT = 12;
    private static final int IMAGE_MAX_WIDTH = 540;
    private static final int IMAGE_SPACING = 10;
    private float panelOpenAnimation = 0.0f;
    private final Map<String, ResourceLocation> imageCache = new HashMap<>();
    private final Map<String, ImageData> imageDimensions = new HashMap<>();
    private final Map<String, ImageClickArea> imageClickAreas = new HashMap<>();
    private final Map<String, Boolean> downloadingImages = new HashMap<>();
    private boolean isDraggingScrollbar = false;
    private int scrollbarX = 0;
    private int scrollbarY = 0;
    private int scrollbarHeight = 0;
    private boolean isDraggingPanel = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private int panelX = 0;
    private int panelY = 0;
    private int panelWidth = 0;
    private int panelHeight = 0;

    public ChangelogViewer(Screen parent) {
        super(Component.literal("Changelog"));
        this.parent = parent;
        this.minecraft = Minecraft.getInstance();
        
        String projectId = ChangelogCache.getProjectId();
        String version = ChangelogCache.getCurrentVersion();
        
        if (projectId == null) {
            projectId = "532141";
        }
        if (version == null) {
            version = "0.3.2";
        }
        
        this.changelog = ChangelogCache.getChangelog(projectId, version);
    }
    
    public void resetAnimation() {
        panelOpenAnimation = 0.0f;
    }

    @Override
    protected void init() {
        super.init();
        
        if (lines == null || lines.isEmpty()) {
            parseMarkdown();
        }
        
        if (parent instanceof TitleScreen) {
        } else {
            addRenderableWidget(Button.builder(Component.literal("Close"),
                    button -> this.minecraft.setScreen(parent))
                    .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                    .build());
        }
        
        calculateMaxScroll();
    }

    private void parseMarkdown() {
        lines = new ArrayList<>();
        String[] rawLines = changelog.split("\n");
        
        CthuLibConfig cfg = CthuLibConfig.getInstance();
        int maxWidth = cfg.changelogPanelWidth - PADDING * 2 - 20;
        
        for (String line : rawLines) {
            MarkdownLine parsed = parseMarkdownLine(line);
            
            if (parsed.text.trim().isEmpty()) {
                lines.add(parsed);
                continue;
            }
            
            List<MarkdownLine> wrappedLines = wrapLine(parsed, maxWidth);
            lines.addAll(wrappedLines);
        }
    }
    
    private List<MarkdownLine> wrapLine(MarkdownLine line, int maxWidth) {
        List<MarkdownLine> result = new ArrayList<>();
        
        if (line.style == MarkdownStyle.HEADING1 || line.style == MarkdownStyle.HEADING2 ||
            line.style == MarkdownStyle.HEADING3 || line.style == MarkdownStyle.EMPTY ||
            line.style == MarkdownStyle.IMAGE) {
            result.add(line);
            return result;
        }
        
        String text = line.text;
        if (this.font.width(text) <= maxWidth) {
            result.add(line);
            return result;
        }
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int lineIndex = 0;
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    MarkdownLine wrappedLine = new MarkdownLine(currentLine.toString(), line.style);
                    wrappedLine.wrapIndex = lineIndex++;
                    result.add(wrappedLine);
                    currentLine = new StringBuilder(word);
                } else {
                    MarkdownLine wrappedLine = new MarkdownLine(word, line.style);
                    wrappedLine.wrapIndex = lineIndex++;
                    result.add(wrappedLine);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            MarkdownLine wrappedLine = new MarkdownLine(currentLine.toString(), line.style);
            wrappedLine.wrapIndex = lineIndex;
            result.add(wrappedLine);
        }
        
        int totalWraps = result.size();
        for (MarkdownLine wrappedLine : result) {
            wrappedLine.totalWraps = totalWraps;
        }
        
        return result;
    }

    private MarkdownLine parseMarkdownLine(String line) {

        if (line.matches(".*\\[!\\[[^\\]]*\\]\\([^)]+\\)\\]\\([^)]+\\).*")) {
            String imageUrl = line.replaceAll(".*\\[!\\[[^\\]]*\\]\\(([^)]+)\\)\\]\\([^)]+\\).*", "$1");
            String linkUrl = line.replaceAll(".*\\[!\\[[^\\]]*\\]\\([^)]+\\)\\]\\(([^)]+)\\).*", "$1");
            return new MarkdownLine("", MarkdownStyle.IMAGE, imageUrl, linkUrl);
        }

        if (line.matches(".*!\\[[^\\]]*\\]\\([^)]+\\).*")) {
            String imageUrl = line.replaceAll(".*!\\[[^\\]]*\\]\\(([^)]+)\\).*", "$1");
            return new MarkdownLine("", MarkdownStyle.IMAGE, imageUrl, null);
        }

        MarkdownStyle style = MarkdownStyle.NORMAL;
        String textToClean = line;
        
        if (line.startsWith("# ")) {
            style = MarkdownStyle.HEADING1;
            textToClean = line.substring(2);
        } else if (line.startsWith("## ")) {
            style = MarkdownStyle.HEADING2;
            textToClean = line.substring(3);
        } else if (line.startsWith("### ")) {
            style = MarkdownStyle.HEADING3;
            textToClean = line.substring(4);
        } else if (line.startsWith("+ ")) {
            style = MarkdownStyle.SUB_BULLET;
            textToClean = line.substring(2);
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            style = MarkdownStyle.BULLET;
            textToClean = line.substring(2);
        } else if (line.startsWith("> ")) {
            style = MarkdownStyle.QUOTE;
            textToClean = line.substring(2);
        } else if (line.trim().startsWith("```")) {
            style = MarkdownStyle.CODE_BLOCK;
        } else if (line.matches(".*`[^`]+`.*")) {
            style = MarkdownStyle.INLINE_CODE;
        } else if (line.matches(".*\\\\*\\\\*[^*]+\\\\*\\\\*.*")) {
            style = MarkdownStyle.BOLD;
        } else if (line.matches(".*\\\\*[^*]+\\\\*.*") || line.matches(".*_[^_]+_.*")) {
            style = MarkdownStyle.ITALIC;
        } else if (line.trim().isEmpty()) {
            style = MarkdownStyle.EMPTY;
        }
        
        String cleanedText = textToClean.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        
        return new MarkdownLine(cleanedText, style, null, null);
    }

    private void calculateMaxScroll() {
        CthuLibConfig config = CthuLibConfig.getInstance();
        int screenHeight = this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : this.height;
        int baseHeight = Math.min(config.changelogPanelHeight, screenHeight - 60);
        int viewportHeight = baseHeight - 70;
        
        int contentHeight = PADDING * 2;
        int lineCount = lines.size();
        for (int i = 0; i < lineCount; i++) {
            MarkdownLine line = lines.get(i);
            boolean isLastLine = (i == lineCount - 1);
            
            if (line.style == MarkdownStyle.IMAGE && line.imageUrl != null) {
                int imageHeight = getImageHeight(line);
                if (isLastLine) {
                    contentHeight += imageHeight - IMAGE_SPACING;
                } else {
                    contentHeight += imageHeight;
                }
            } else {
                contentHeight += LINE_HEIGHT;
                if (line.style == MarkdownStyle.HEADING1 || line.style == MarkdownStyle.HEADING2) {
                    contentHeight += 6;
                }
            }
        }
        
        maxScroll = Math.max(0, contentHeight - viewportHeight);
    }
    
    private int getImageHeight(MarkdownLine line) {
        ImageData imgData = imageDimensions.get(line.imageUrl);
        CthuLibConfig cfg = CthuLibConfig.getInstance();
        int maxWidth = cfg.changelogPanelWidth - PADDING * 2 - 20;
        
        if (imgData != null) {
            int imgWidth = Math.min(IMAGE_MAX_WIDTH, Math.min(maxWidth, imgData.width));
            float aspectRatio = (float) imgData.height / imgData.width;
            int imgHeight = (int) (imgWidth * aspectRatio);
            return imgHeight + IMAGE_SPACING;
        } else {
            int imgWidth = Math.min(IMAGE_MAX_WIDTH, maxWidth);
            int imgHeight = (int) (imgWidth * 0.3f);
            return imgHeight + IMAGE_SPACING;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderChangelogPanel(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    public void renderAsOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        
        int backdropAlpha = (int)(220 * Math.min(1.0f, panelOpenAnimation));
        graphics.fill(0, 0, this.minecraft.getWindow().getGuiScaledWidth(), 
                     this.minecraft.getWindow().getGuiScaledHeight(), 
                     backdropAlpha << 24);
        
        renderChangelogPanel(graphics, mouseX, mouseY, partialTick);
        
        graphics.pose().popPose();
    }
    
    private void renderChangelogPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        
        panelOpenAnimation = Math.min(1.0f, panelOpenAnimation + 0.08f);
        float easeProgress = easeOutCubic(panelOpenAnimation);
        
        int screenWidth = this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : this.width;
        int screenHeight = this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : this.height;
        
        CthuLibConfig config = CthuLibConfig.getInstance();
        int baseWidth = Math.min(config.changelogPanelWidth, screenWidth - 100);
        int baseHeight = Math.min(config.changelogPanelHeight, screenHeight - 60);
        
        panelWidth = (int)(baseWidth * easeProgress);
        panelHeight = (int)(baseHeight * easeProgress);
        
        if (config.changelogPanelX >= 0 && config.changelogPanelY >= 0) {
            panelX = Math.max(0, Math.min(screenWidth - panelWidth, config.changelogPanelX));
            panelY = Math.max(0, Math.min(screenHeight - panelHeight, config.changelogPanelY));
        } else {
            panelX = (screenWidth - panelWidth) / 2;
            panelY = (screenHeight - panelHeight) / 2;
        }
        
        int shadowLayers = 8;
        for (int i = 0; i < shadowLayers; i++) {
            float shadowProgress = (float)i / shadowLayers;
            int shadowAlpha = (int)(100 * (1.0f - shadowProgress) * easeProgress);
            int offset = i * 2;
            graphics.fill(panelX - offset, panelY + offset, 
                         panelX + panelWidth + offset, panelY + panelHeight + offset, 
                         shadowAlpha << 24);
        }
        
        int bgAlpha = (int)(245 * easeProgress);
        int gradientSteps = 10;
        int stepHeight = panelHeight / gradientSteps;
        for (int i = 0; i < gradientSteps; i++) {
            float gradProgress = (float)i / gradientSteps;
            int brightness = (int)(0x18 + (0x22 - 0x18) * gradProgress);
            int bgColor = (bgAlpha << 24) | (brightness * 0x010101);
            int y1 = panelY + i * stepHeight;
            int y2 = (i == gradientSteps - 1) ? panelY + panelHeight : y1 + stepHeight;
            graphics.fill(panelX, y1, panelX + panelWidth, y2, bgColor);
        }
        
        int borderAlpha = (int)(230 * easeProgress);
        int accentColor1 = (borderAlpha << 24) | 0xFFDD66;
        int accentColor2 = (borderAlpha << 24) | 0xFF8833;
        int borderWidth = 3;
        
        for (int i = 0; i < borderWidth; i++) {
            graphics.fill(panelX, panelY + i, panelX + panelWidth, panelY + i + 1, accentColor1);
            graphics.fill(panelX + i, panelY, panelX + i + 1, panelY + panelHeight, accentColor1);
        }
        
        for (int i = 0; i < borderWidth; i++) {
            graphics.fill(panelX, panelY + panelHeight - borderWidth + i, 
                         panelX + panelWidth, panelY + panelHeight - borderWidth + i + 1, accentColor2);
            graphics.fill(panelX + panelWidth - borderWidth + i, panelY, 
                         panelX + panelWidth - borderWidth + i + 1, panelY + panelHeight, accentColor2);
        }
        
        int titleY = panelY + 15;
        int titleWidth = this.font.width(this.title);
        int titleX = (panelX + panelWidth / 2) - (titleWidth / 2);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        
        graphics.drawString(this.font, this.title, titleX + 2, titleY + 2, (int)(100 * easeProgress) << 24, false);
        graphics.drawString(this.font, this.title, titleX, titleY - 1, ((int)(80 * easeProgress) << 24) | 0xFFAA66, false);
        graphics.drawString(this.font, this.title, titleX, titleY, ((int)(255 * easeProgress) << 24) | 0xFFDD99, false);
        
        pose.popPose();
        
        int contentY = panelY + 45;
        int contentHeight = panelHeight - 70;
        graphics.enableScissor(panelX + PADDING, contentY, panelX + panelWidth - PADDING, contentY + contentHeight);
        
        int y = contentY + 5 - scrollOffset;
        boolean inCodeBlock = false;
        
        int textAlpha = (int)(255 * easeProgress);
        
        if (lines == null) {
            return;
        }
        
        for (MarkdownLine line : lines) {
            if (line.style == MarkdownStyle.CODE_BLOCK) {
                inCodeBlock = !inCodeBlock;
                continue;
            }
            
            if (line.style == MarkdownStyle.IMAGE) {
                int imageHeight = getImageHeight(line);
                if (y + imageHeight >= contentY && y < contentY + contentHeight) {
                    y += renderImage(graphics, line, panelX + PADDING, panelWidth - PADDING * 2, y, textAlpha);
                } else {
                    y += imageHeight;
                }
                continue;
            }
            
            if (y + LINE_HEIGHT >= contentY && y < contentY + contentHeight) {
                renderMarkdownLine(graphics, line, panelX + PADDING, y, inCodeBlock, textAlpha);
            }
            
            y += LINE_HEIGHT;
            if (line.style == MarkdownStyle.HEADING1 || line.style == MarkdownStyle.HEADING2) {
                y += 6;
            }
        }
        
        graphics.disableScissor();

        if (maxScroll > 0) {
            renderScrollbar(graphics, panelX + panelWidth - 15, contentY, contentHeight, textAlpha);
        }
    }

    private void renderMarkdownLine(GuiGraphics graphics, MarkdownLine line, int x, int y, boolean inCodeBlock, int baseAlpha) {
        int color = 0xFFFFFF;
        String text = line.text;
        int offsetX = 0;
        
        switch (line.style) {
            case HEADING1:
                color = 0xFFFF55;
                graphics.drawString(this.font, text, x + 1, y + 1, (baseAlpha / 3) << 24, false);
                graphics.drawString(this.font, text, x, y, (baseAlpha << 24) | color, false);
                return;
            case HEADING2:
                color = 0xFFDD33;
                graphics.drawString(this.font, text, x + 1, y + 1, (baseAlpha / 3) << 24, false);
                graphics.drawString(this.font, text, x, y, (baseAlpha << 24) | color, false);
                return;
            case HEADING3:
                color = 0xFFBB11;
                graphics.drawString(this.font, text, x, y, (baseAlpha << 24) | color, false);
                return;
            case BULLET:
                double middleLine = (line.totalWraps - 1) / 2.0;
                if (Math.abs(line.wrapIndex - middleLine) < 0.01) {
                    graphics.drawString(this.font, "•", x, y, (baseAlpha << 24) | 0xFFAA66, false);
                }
                offsetX = 10;
                color = 0xDDDDDD;
                break;
            case SUB_BULLET:
                double subMiddleLine = (line.totalWraps - 1) / 2.0;
                if (Math.abs(line.wrapIndex - subMiddleLine) < 0.01) {
                    graphics.drawString(this.font, "◦", x + 10, y, (baseAlpha << 24) | 0xFFAA66, false);
                }
                offsetX = 24;
                color = 0xCCCCCC;
                break;
            case QUOTE:
                int quoteAlpha = (int)(baseAlpha * 0.6f);
                graphics.fill(x, y, x + 3, y + LINE_HEIGHT - 2, (quoteAlpha << 24) | 0xFFAA66);
                offsetX = 10;
                color = 0xBBBBBB;
                break;
            case INLINE_CODE:
                text = text.replaceAll("`([^`]+)`", "$1");
                color = 0x66FFDD;
                int codeWidth = this.font.width(text) + 6;
                graphics.fill(x, y - 1, x + codeWidth, y + LINE_HEIGHT - 1, (baseAlpha / 4) << 24);
                graphics.fill(x, y - 1, x + codeWidth, y, ((baseAlpha / 2) << 24) | 0x33AA88);
                offsetX = 3;
                break;
            case BOLD:
                text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
                color = 0xFFFFFF;
                graphics.drawString(this.font, text, x + offsetX + 1, y + 1, (baseAlpha / 4) << 24, false);
                graphics.drawString(this.font, text, x + offsetX, y, (baseAlpha << 24) | color, false);
                return;
            case ITALIC:
                text = text.replaceAll("[*_]([^*_]+)[*_]", "$1");
                color = 0xDDDDDD;
                break;
            case EMPTY:
                return;
            default:
                if (inCodeBlock) {
                    color = 0x66FFDD;
                    int codeLineWidth = this.font.width(text) + 6;
                    graphics.fill(x, y - 1, x + codeLineWidth, y + LINE_HEIGHT - 1, (baseAlpha / 4) << 24);
                    graphics.fill(x, y - 1, x + codeLineWidth, y, ((baseAlpha / 2) << 24) | 0x33AA88);
                    offsetX = 3;
                } else {
                    color = 0xDDDDDD;
                }
                break;
        }
        
        graphics.drawString(this.font, text, x + offsetX, y, (baseAlpha << 24) | color, false);
    }

    private void renderScrollbar(GuiGraphics graphics, int scrollbarX, int contentY, int contentHeight, int baseAlpha) {
        this.scrollbarX = scrollbarX;
        this.scrollbarY = contentY;
        this.scrollbarHeight = contentHeight;
        
        int trackAlpha = (int)(baseAlpha * 0.3f);
        graphics.fill(scrollbarX, contentY, scrollbarX + 6, contentY + contentHeight, (trackAlpha << 24) | 0x444444);
        
        float scrollPercent = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
        int totalContentHeight = contentHeight + maxScroll;
        int thumbHeight = Math.max(30, (int) ((float) contentHeight / totalContentHeight * contentHeight));
        int thumbY = contentY + (int) ((contentHeight - thumbHeight) * scrollPercent);
        
        int gradSteps = 5;
        int stepHeight = thumbHeight / gradSteps;
        for (int i = 0; i < gradSteps; i++) {
            float gradProgress = (float)i / gradSteps;
            int brightness = isDraggingScrollbar ? 
                (int)(0xAA + (0xCC - 0xAA) * gradProgress) : 
                (int)(0x88 + (0xAA - 0x88) * gradProgress);
            int thumbColor = (baseAlpha << 24) | (brightness * 0x010101);
            int y1 = thumbY + i * stepHeight;
            int y2 = (i == gradSteps - 1) ? thumbY + thumbHeight : y1 + stepHeight;
            graphics.fill(scrollbarX, y1, scrollbarX + 6, y2, thumbColor);
        }
        
        int accentAlpha = (int)(baseAlpha * (isDraggingScrollbar ? 0.9f : 0.6f));
        graphics.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + 1, (accentAlpha << 24) | 0xFFDD66);
        graphics.fill(scrollbarX, thumbY + thumbHeight - 1, scrollbarX + 6, thumbY + thumbHeight, (accentAlpha << 24) | 0xFF8833);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return handleScroll(scrollY);
    }
    
    public boolean handleScroll(double scrollY) {
        if (maxScroll > 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * 10));
            return true;
        }
        return false;
    }
    
    public boolean isClickOutsidePanel(double mouseX, double mouseY) {
        return mouseX < panelX || mouseX > panelX + panelWidth ||
               mouseY < panelY || mouseY > panelY + panelHeight;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && DevModeManager.shouldAllowDragging()) {
            if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                mouseY >= panelY && mouseY <= panelY + 40) {
                isDraggingPanel = true;
                dragOffsetX = mouseX - panelX;
                dragOffsetY = mouseY - panelY;
                return true;
            }
        }
        
        if (maxScroll > 0 && mouseX >= scrollbarX && mouseX <= scrollbarX + 6 &&
            mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            isDraggingScrollbar = true;
            return true;
        }
        
        for (Map.Entry<String, ImageClickArea> entry : imageClickAreas.entrySet()) {
            ImageClickArea area = entry.getValue();
            if (area.linkUrl != null && mouseX >= area.x && mouseX <= area.x + area.width &&
                mouseY >= area.y && mouseY <= area.y + area.height) {
                Util.getPlatform().openUri(area.linkUrl);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingPanel && button == 0) {
            isDraggingPanel = false;
            CthuLibConfig config = CthuLibConfig.getInstance();
            config.changelogPanelX = panelX;
            config.changelogPanelY = panelY;
            config.save();
            
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("Changelog Panel Position Saved: " + panelX + ", " + panelY), 
                    true
                );
            }
            return true;
        }
        
        if (isDraggingScrollbar && button == 0) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingPanel && button == 0) {
            int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
            
            panelX = Math.max(0, Math.min(screenWidth - panelWidth, (int)(mouseX - dragOffsetX)));
            panelY = Math.max(0, Math.min(screenHeight - panelHeight, (int)(mouseY - dragOffsetY)));
            return true;
        }
        
        if (isDraggingScrollbar && maxScroll > 0 && button == 0) {
            int contentY = panelY + 45;
            int contentHeight = panelHeight - 70;
            float scrollPercent = Math.max(0, Math.min(1, (float)(mouseY - contentY) / contentHeight));
            scrollOffset = Math.max(0, Math.min(maxScroll, (int)(scrollPercent * maxScroll)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
    
    private static float easeOutCubic(float t) {
        return 1.0f - (float)Math.pow(1.0f - t, 3.0);
    }
    
    private int renderImage(GuiGraphics graphics, MarkdownLine line, int x, int maxWidth, int y, int alpha) {
        if (line.imageUrl == null) return LINE_HEIGHT;
        
        try {
            ResourceLocation texture = getOrLoadTexture(line.imageUrl);
            if (texture == null) {
                graphics.drawString(this.font, "[Loading image...]", x, y, (alpha << 24) | 0x888888, false);
                return LINE_HEIGHT + IMAGE_SPACING;
            }
            
            ImageData imgData = imageDimensions.get(line.imageUrl);
            int imgWidth, imgHeight;
            
            if (imgData != null) {
                float aspectRatio = (float) imgData.height / imgData.width;
                imgWidth = Math.min(IMAGE_MAX_WIDTH, Math.min(maxWidth - 20, imgData.width));
                imgHeight = (int) (imgWidth * aspectRatio);
            } else {
                imgWidth = Math.min(IMAGE_MAX_WIDTH, maxWidth - 20);
                imgHeight = (int) (imgWidth * 0.3f);
            }
            
            graphics.blit(texture, x, y, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
            
            if (line.linkUrl != null) {
                imageClickAreas.put(line.imageUrl, new ImageClickArea(x, y, imgWidth, imgHeight, line.linkUrl));
                
                int borderAlpha = alpha / 2;
                graphics.fill(x, y, x + imgWidth, y + 1, (borderAlpha << 24) | 0xFFFFFF);
                graphics.fill(x, y + imgHeight - 1, x + imgWidth, y + imgHeight, (borderAlpha << 24) | 0xFFFFFF);
                graphics.fill(x, y, x + 1, y + imgHeight, (borderAlpha << 24) | 0xFFFFFF);
                graphics.fill(x + imgWidth - 1, y, x + imgWidth, y + imgHeight, (borderAlpha << 24) | 0xFFFFFF);
            }
            
            return imgHeight + IMAGE_SPACING;
        } catch (Exception e) {
            graphics.drawString(this.font, "[Image load error]", x, y, (alpha << 24) | 0xFF0000, false);
            return LINE_HEIGHT + IMAGE_SPACING;
        }
    }
    
    private ResourceLocation getOrLoadTexture(String url) {
        if (imageCache.containsKey(url)) {
            return imageCache.get(url);
        }
        
        String hash = String.valueOf(url.hashCode()).replace("-", "_");
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("cthulib", "textures/changelog/" + hash + ".png");
        
        File cacheDir = new File(minecraft.gameDirectory, "cthulib_cache/images");
        cacheDir.mkdirs();
        File cacheFile = new File(cacheDir, hash + ".png");
        
        synchronized (downloadingImages) {
            if (downloadingImages.getOrDefault(url, false)) {
                return null;
            }
        }

        if (cacheFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(cacheFile);
                BufferedImage bufferedImage = ImageIO.read(fis);
                fis.close();

                if (bufferedImage != null) {
                    imageDimensions.put(url, new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight()));

                    NativeImage nativeImage = convertBufferedImageToNativeImage(bufferedImage);

                    DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                    minecraft.getTextureManager().register(location, dynamicTexture);
                    imageCache.put(url, location);
                    return location;
                } else {
                    cacheFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
                cacheFile.delete();
            }
        }

        synchronized (downloadingImages) {
            if (downloadingImages.getOrDefault(url, false)) {
                return null;
            }
            downloadingImages.put(url, true);
        }
        
        Util.ioPool().execute(() -> {
            try {
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.connect();
                
                if (connection.getResponseCode() != 200) {
                    synchronized (downloadingImages) {
                        downloadingImages.put(url, false);
                    }
                    return;
                }
                
                File tempFile = new File(cacheDir, hash + "_" + System.currentTimeMillis() + ".tmp");
                InputStream inputStream = connection.getInputStream();
                Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                inputStream.close();
                
                BufferedImage bufferedImage = null;
                try {
                    bufferedImage = ImageIO.read(tempFile);
                } catch (Exception e) {
                }
                
                tempFile.delete();
                
                if (bufferedImage == null) {
                    synchronized (downloadingImages) {
                        downloadingImages.put(url, false);
                    }
                    return;
                }
                
                final int originalWidth = bufferedImage.getWidth();
 final int originalHeight = bufferedImage.getHeight();
                imageDimensions.put(url, new ImageData(originalWidth, originalHeight));
                
                FileOutputStream fos = new FileOutputStream(cacheFile);
                boolean success = ImageIO.write(bufferedImage, "png", fos);
                fos.close();
                
                if (!success) {
                    cacheFile.delete();
                    synchronized (downloadingImages) {
                        downloadingImages.put(url, false);
                    }
                    return;
                }
                
                minecraft.execute(() -> {
                    try {
                        FileInputStream fis = new FileInputStream(cacheFile);
                        NativeImage nativeImage = NativeImage.read(fis);
                        fis.close();
                        
                        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                        minecraft.getTextureManager().register(location, dynamicTexture);
                        imageCache.put(url, location);
                        calculateMaxScroll();
                        synchronized (downloadingImages) {
                            downloadingImages.put(url, false);
                        }
                    } catch (Exception e) {
                        cacheFile.delete();
                        synchronized (downloadingImages) {
                            downloadingImages.put(url, false);
                        }
                    }
                });
            } catch (Exception e) {
                synchronized (downloadingImages) {
                    downloadingImages.put(url, false);
                }
            }
        });
        
        return null;
    }

    private static NativeImage convertBufferedImageToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, false);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                nativeImage.setPixelRGBA(x, y, convertARGBtoABGR(argb));
            }
        }
        
        return nativeImage;
    }

    private static int convertARGBtoABGR(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static class ImageData {
        final int width;
        final int height;
        
        ImageData(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    
    private static class MarkdownLine {
        String text;
        MarkdownStyle style;
        String imageUrl;
        String linkUrl;
        int wrapIndex = 0;
        int totalWraps = 1;

        MarkdownLine(String text, MarkdownStyle style) {
            this(text, style, null, null);
        }
        
        MarkdownLine(String text, MarkdownStyle style, String imageUrl, String linkUrl) {
            this.text = text;
            this.style = style;
            this.imageUrl = imageUrl;
            this.linkUrl = linkUrl;
        }
    }
    
    private static class ImageClickArea {
        int x, y, width, height;
        String linkUrl;
        
        ImageClickArea(int x, int y, int width, int height, String linkUrl) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.linkUrl = linkUrl;
        }
    }

    private enum MarkdownStyle {
        NORMAL,
        HEADING1,
        HEADING2,
        HEADING3,
        BULLET,
        SUB_BULLET,
        QUOTE,
        CODE_BLOCK,
        INLINE_CODE,
        BOLD,
        ITALIC,
        EMPTY,
        IMAGE
    }
}
