package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ModDetailPanel {
    private final Screen screen;
    private ModData currentMod;
    private ResourceLocation modIcon;
    private int x, y, width, height;
    private float openProgress = 0f;
    private float iconRotation = 0f;
    private int padding;
    private int iconSize;
    private int titleBarHeight;
    private int dividerPadding;

    public ModDetailPanel(Screen screen) {
        this.screen = screen;
    }

    public void setMod(ModData mod) {
        this.currentMod = mod;
        if (mod != null) {
            calculateBounds();
            loadModIcon();
            openProgress = 0f;
        } else {
            modIcon = null;
            openProgress = 0f;
        }
    }

    private void loadModIcon() {
        if (currentMod == null) return;

        ModIconLoader.loadIcon(currentMod, new ModIconLoader.IconCallback() {
            @Override
            public void onIconLoaded(ResourceLocation icon) {
                modIcon = icon;
            }

            @Override
            public void onLoadFailed() {
                modIcon = null;
            }
        });
    }

    public boolean isVisible() {
        return currentMod != null;
    }

    private void calculateBounds() {
        int screenWidth = screen.width;
        int screenHeight = screen.height;

        if (screenWidth < 1280) {
            this.width = Math.min(550, screenWidth - 40);
            this.padding = 12;
            this.iconSize = 48;
            this.titleBarHeight = 28;
            this.dividerPadding = 8;
        } else if (screenWidth < 1920) {
            this.width = Math.min(650, screenWidth - 50);
            this.padding = 16;
            this.iconSize = 56;
            this.titleBarHeight = 30;
            this.dividerPadding = 10;
        } else {
            this.width = Math.min(750, screenWidth - 60);
            this.padding = 20;
            this.iconSize = 64;
            this.titleBarHeight = 32;
            this.dividerPadding = 12;
        }

        this.height = calculateContentHeight(screenWidth, screenHeight);

        this.x = (screenWidth - this.width) / 2;
        this.y = (screenHeight - this.height) / 2;
    }

    private int calculateContentHeight(int screenWidth, int screenHeight) {
        if (currentMod == null) {
            if (screenWidth < 1280) return Math.min(400, screenHeight - 40);
            if (screenWidth < 1920) return Math.min(450, screenHeight - 50);
            return Math.min(550, screenHeight - 60);
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int totalHeight = titleBarHeight + 10;

        int leftHeight = 10 + iconSize + 15 + (22 * 3) + 20;

        int rightColumnWidth = (width * 2 / 3) - padding - dividerPadding - 4;
        int rightHeight = 0;

        rightHeight += 14 + 14;

        int descBoxHeight = 42;
        rightHeight += descBoxHeight + 18;

        if (!  currentMod.links.isEmpty()) {
            rightHeight += 14 + 14;
            int linkCount = Math.min(currentMod.links.size(), 6);
            rightHeight += (linkCount * 22) + ((linkCount - 1) * 6) + 15;
        }

        int contentHeight = Math.max(leftHeight, rightHeight);
        totalHeight += contentHeight + padding;

        int maxHeight;
        if (screenWidth < 1280) {
            maxHeight = Math.min(400, screenHeight - 40);
        } else if (screenWidth < 1920) {
            maxHeight = Math.min(450, screenHeight - 50);
        } else {
            maxHeight = Math.min(550, screenHeight - 60);
        }

        return Math.min(totalHeight, maxHeight);
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        if (currentMod == null) return;

        openProgress = Mth.lerp(0.15f, openProgress, 1f);
        iconRotation = (iconRotation + 0.5f) % 360f;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int overlayAlpha = (int)(openProgress * 128);
        gfx.fill(0, 0, screen.width, screen.height, (overlayAlpha << 24));

        gfx.pose().pushPose();
        float scale = 0.7f + (openProgress * 0.3f);
        gfx.pose().translate(x + width / 2f, y + height / 2f, 0);
        gfx.pose().scale(scale, scale, 1f);
        gfx.pose().translate(-(x + width / 2f), -(y + height / 2f), 0);

        for (int i = 0; i < 8; i++) {
            float shadowAlpha = (0.08f - i * 0.01f) * openProgress;
            int offset = i * 2;
            gfx.fill(x + offset, y + offset, x + width + offset, y + height + offset,
                    ((int)(shadowAlpha * 255) << 24));
        }

        gfx.fillGradient(x, y, x + width, y + height, 0xF0202020, 0xF0151515);

        int glowColor = (int)(openProgress * 255) << 24 | 0x00FFFF;
        for (int i = 0; i < 3; i++) {
            int alpha = (int)((0.4f - i * 0.13f) * openProgress * 255);
            gfx.renderOutline(x - i, y - i, width + i * 2, height + i * 2, alpha << 24 | 0x00FFFF);
        }
        gfx.renderOutline(x, y, width, height, 0xFF00FFFF);

        gfx.fillGradient(x + 2, y + 2, x + width - 2, y + titleBarHeight,
                0xFF006699, 0xFF004477);

        float pulseEffect = (float)(Math.sin(System.currentTimeMillis() / 400.0) * 0.5 + 0.5);
        int accentAlpha = (int)((0x80 + pulseEffect * 0x40) * openProgress);
        gfx.fillGradient(x + 2, y + 2, x + width - 2, y + 6,
                (accentAlpha << 24) | 0x00FFFF, (accentAlpha << 24) | 0x0099DD);

        Component titleComp = Component.literal(currentMod.name).withStyle(Style.EMPTY.withBold(true));
        int titleWidth = font.width(titleComp);
        int titleX = x + width / 2 - titleWidth / 2;
        int titleY = y + (titleBarHeight - font.lineHeight) / 2 + 2;

        int glowAlpha = (int)(pulseEffect * 100 * openProgress);
        gfx.drawString(font, titleComp, titleX, titleY, glowAlpha << 24 | 0x00AAFF, false);
        gfx.drawString(font, titleComp, titleX, titleY, 0xFFFFFF, false);

        int closeX = x + width - 26;
        int closeY = y + 6;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 18 &&
                mouseY >= closeY && mouseY <= closeY + 18;

        int closeBg = closeHover ? 0xFFFF5555 : 0xFF994444;
        gfx.fill(closeX, closeY, closeX + 18, closeY + 18, closeBg);
        gfx.renderOutline(closeX, closeY, 18, 18, closeHover ? 0xFFFF8888 : 0xFFAA5555);

        gfx.drawString(font, "X", closeX + 6, closeY + 5, 0xFFFFFF);

        int contentTop = y + titleBarHeight + 10;
        int contentLeft = x + padding;
        int contentWidth = width - (padding * 2);
        int contentHeight = height - titleBarHeight - padding - 10;

        int leftColumnWidth = (width / 3) - padding - dividerPadding;
        int rightColumnWidth = (width * 2 / 3) - padding - dividerPadding;

        int dividerX = contentLeft + leftColumnWidth + dividerPadding;
        gfx.fillGradient(dividerX - 1, contentTop, dividerX + 1, contentTop + contentHeight,
                0x00FFFFFF, 0x88FFFFFF);
        gfx.fillGradient(dividerX - 1, contentTop + contentHeight / 2, dividerX + 1, contentTop + contentHeight,
                0x88FFFFFF, 0x00FFFFFF);

        renderLeftColumn(gfx, font, contentLeft, contentTop, leftColumnWidth, contentHeight);

        int rightColumnX = dividerX + dividerPadding;
        renderRightColumn(gfx, font, rightColumnX, contentTop,
                rightColumnWidth, contentHeight, mouseX, mouseY);

        gfx.pose().popPose();
    }

    private void renderLeftColumn(GuiGraphics gfx, Font font, int x, int y, int columnWidth, int columnHeight) {
        int iconX = x + columnWidth / 2 - iconSize / 2;
        int iconY = y + 10;

        float glowSize = 4 + (float)(Math.sin(System.currentTimeMillis() / 300.0) * 2);
        int glowAlpha = (int)((0.5f + Math.sin(System.currentTimeMillis() / 500.0) * 0.3f) * 255);
        gfx.fill((int)(iconX - glowSize), (int)(iconY - glowSize),
                (int)(iconX + iconSize + glowSize), (int)(iconY + iconSize + glowSize),
                (glowAlpha << 24) | 0x00AAFF);

        if (modIcon != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, openProgress);
            gfx.blit(modIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            float pulseEffect = (float)(Math.sin(System.currentTimeMillis() / 400.0) * 0.5 + 0.5);
            int borderColor = lerpColor(0xFF00CCFF, 0xFF00FFFF, pulseEffect);
            gfx.renderOutline(iconX, iconY, iconSize, iconSize, borderColor);
        } else {
            gfx.fillGradient(iconX, iconY, iconX + iconSize, iconY + iconSize,
                    0xFF5A5A5A, 0xFF333333);
            gfx.renderOutline(iconX, iconY, iconSize, iconSize, 0xFF00CCFF);

            String letter = currentMod.name.substring(0, Math.min(1, currentMod.name.length())).toUpperCase();
            int lw = font.width(letter);
            gfx.drawString(font, letter,
                    iconX + (iconSize - lw) / 2,
                    iconY + (iconSize - font.lineHeight) / 2 + 1,
                    0xEEEEEE, true);
        }

        int infoY = iconY + iconSize + 15;
        int infoSpacing = 22;

        renderInfoField(gfx, font, "Mod ID", currentMod.modId, x, infoY, columnWidth, true);
        infoY += infoSpacing;

        renderInfoField(gfx, font, "Version", currentMod.version, x, infoY, columnWidth, false);
        infoY += infoSpacing;

        renderInfoField(gfx, font, "Author", currentMod.author, x, infoY, columnWidth, false);
    }

    private void renderInfoField(GuiGraphics gfx, Font font, String label, String value,
                                 int x, int y, int width, boolean italic) {
        gfx.pose().pushPose();
        gfx.pose().scale(0.75f, 0.75f, 1f);
        int labelX = (int)(x / 0.75f);
        int labelY = (int)(y / 0.75f);
        gfx.drawString(font, Component.literal(label.toUpperCase()).withStyle(Style.EMPTY.withBold(true)),
                labelX, labelY, 0x00AAFF, false);
        gfx.pose().popPose();

        String displayValue = truncate(font, value, width - 4);
        Component valueComp = Component.literal(displayValue);
        if (italic) {
            valueComp = Component.literal(displayValue).withStyle(Style.EMPTY.withItalic(true));
        }

        gfx.drawString(font, valueComp, x + 2, y + 10, 0xEEEEEE, false);
    }

    private void renderRightColumn(GuiGraphics gfx, Font font, int x, int y, int columnWidth,
                                   int columnHeight, int mouseX, int mouseY) {
        gfx.drawString(font, Component.literal("DESCRIPTION").withStyle(Style.EMPTY.withBold(true)),
                x, y, 0x00FFFF, false);
        y += 14;

        int descBoxWidth = columnWidth - 4;
        int descBoxHeight = 42;

        gfx.fill(x - 2, y - 2, x + descBoxWidth + 2, y + descBoxHeight + 2, 0x44FFFFFF);
        gfx.fill(x, y, x + descBoxWidth, y + descBoxHeight, 0xFF1A1A1A);

        if (currentMod.description != null && ! currentMod.description.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(Component.literal(currentMod.description), descBoxWidth - 12);

            int descY = y + 6;
            int lineCount = 0;
            for (FormattedCharSequence line :  lines) {
                if (lineCount >= 3) break;
                gfx.drawString(font, line, x + 6, descY, 0xCCCCCC);
                descY += 10;
                lineCount++;
            }
        } else {
            gfx.drawString(font, "No description available.", x + 6, y + 6, 0x888888);
        }

        y += descBoxHeight + 18;


        if (!currentMod.links.isEmpty()) {
            gfx.drawString(font, Component.literal("LINKS").withStyle(Style.EMPTY.withBold(true)),
                    x, y, 0x00FFFF, false);
            y += 14;

            int buttonHeight = 22;
            int buttonSpacing = 6;
            int columnSpacing = 8;
            int maxLinks = 6;


            List<Map.Entry<String, String>> linkList = new ArrayList<>();
            int count = 0;
            for (Map.Entry<String, String> link : currentMod.links.entrySet()) {
                if (count >= maxLinks) break;
                linkList.add(link);
                count++;
            }


            int numColumns = (linkList.size() + 1) / 2;
            int availableWidth = columnWidth - 4;
            int columnButtonWidth = (availableWidth - ((numColumns - 1) * columnSpacing)) / numColumns;


            int currentColumn = 0;
            int currentRow = 0;
            int startY = y;

            for (int i = 0; i < linkList.size(); i++) {
                Map.Entry<String, String> link = linkList.get(i);

                int btnX = x + (currentColumn * (columnButtonWidth + columnSpacing));
                int btnY = startY + (currentRow * (buttonHeight + buttonSpacing));
                int btnW = columnButtonWidth;
                int btnH = buttonHeight;

                String label = getLinkLabel(link.getKey());
                boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW &&
                        mouseY >= btnY && mouseY <= btnY + btnH;

                float pulseEffect = (float)(Math.sin(System.currentTimeMillis() / 500.0) * 0.5 + 0.5);

                if (btnHover) {
                    int hoverColor1 = lerpColor(0xFF00DDFF, 0xFF00FFFF, pulseEffect);
                    int hoverColor2 = lerpColor(0xFF0099CC, 0xFF00BBDD, pulseEffect);
                    gfx.fillGradient(btnX, btnY, btnX + btnW, btnY + btnH, hoverColor1, hoverColor2);
                    gfx.fill(btnX - 1, btnY - 1, btnX + btnW + 1, btnY + btnH + 1, 0x4400FFFF);
                } else {
                    gfx.fillGradient(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF005588, 0xFF003355);
                }

                gfx.renderOutline(btnX, btnY, btnW, btnH, btnHover ? 0xFF00FFFF : 0xFF006699);

                if (btnHover) {
                    gfx.fillGradient(btnX + 2, btnY + 2, btnX + btnW - 2, btnY + 5, 0x6600FFFF, 0x0000FFFF);
                }

                String displayLabel = truncate(font, label, btnW - 16);
                int textWidth = font.width(displayLabel);
                int textX = btnX + (btnW - textWidth) / 2;
                gfx.drawString(font, displayLabel, textX, btnY + 7, 0xFFFFFF, false);

                currentRow++;
                if (currentRow >= 2) {
                    currentRow = 0;
                    currentColumn++;
                }
            }
        }
    }

    private String getLinkLabel(String linkType) {
        return switch (linkType) {
            case "homepage" -> "🏠 Homepage";
            case "sources" -> "📦 Source Code";
            case "issues" -> "🐛 Issue Tracker";
            default -> "🔗 " + linkType;
        };
    }

    private String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String dots = "...";
        int dotsW = font.width(dots);
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c);
            if (font.width(sb.toString()) + dotsW > maxWidth) {
                sb.setLength(Math.max(0, sb.length() - 1));
                break;
            }
        }
        return sb.toString() + dots;
    }

    private int lerpColor(int color1, int color2, float progress) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * progress);
        int r = (int)(r1 + (r2 - r1) * progress);
        int g = (int)(g1 + (g2 - g1) * progress);
        int b = (int)(b1 + (b2 - b1) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (! isVisible() || openProgress < 0.9f) return false;

        int closeX = x + width - 26;
        int closeY = y + 6;
        if (mouseX >= closeX && mouseX <= closeX + 18 &&
                mouseY >= closeY && mouseY <= closeY + 18) {
            setMod(null);
            return true;
        }

        if (currentMod != null && ! currentMod.links.isEmpty()) {
            int contentLeft = x + padding;
            int leftColumnWidth = (width / 3) - padding - dividerPadding;
            int dividerX = contentLeft + leftColumnWidth + dividerPadding;
            int rightColumnX = dividerX + dividerPadding;
            int rightColumnWidth = (width * 2 / 3) - padding - dividerPadding;

            int contentTop = y + titleBarHeight + 10;

            Font font = Minecraft.getInstance().font;
            int descBoxWidth = rightColumnWidth - 4;
            int descBoxHeight = 80;

            if (currentMod.description != null && ! currentMod.description.isEmpty()) {
                List<FormattedCharSequence> lines = font.split(
                        Component.literal(currentMod.description),
                        descBoxWidth - 12
                );
                int descLines = lines.size();
                descBoxHeight = Math.max(80, Math.min(descLines * 10 + 12, 120));
            }

            int linksStartY = contentTop + 14 + descBoxHeight + 18 + 14;

            List<Map.Entry<String, String>> linkList = new ArrayList<>();
            int count = 0;
            int maxLinks = 6;
            for (Map.Entry<String, String> link : currentMod.links.entrySet()) {
                if (count >= maxLinks) break;
                linkList.add(link);
                count++;
            }

            int buttonHeight = 22;
            int buttonSpacing = 6;
            int columnSpacing = 8;
            int numColumns = (linkList.size() + 1) / 2;
            int availableWidth = rightColumnWidth - 4;
            int columnButtonWidth = (availableWidth - ((numColumns - 1) * columnSpacing)) / numColumns;

            int currentColumn = 0;
            int currentRow = 0;

            for (int i = 0; i < linkList.size(); i++) {
                Map.Entry<String, String> link = linkList.get(i);

                int btnX = rightColumnX + (currentColumn * (columnButtonWidth + columnSpacing));
                int btnY = linksStartY + (currentRow * (buttonHeight + buttonSpacing));
                int btnW = columnButtonWidth;
                int btnH = buttonHeight;

                if (mouseX >= btnX && mouseX <= btnX + btnW &&
                        mouseY >= btnY && mouseY <= btnY + btnH) {
                    Util.getPlatform().openUri(link.getValue());
                    return true;
                }

                currentRow++;
                if (currentRow >= 2) {
                    currentRow = 0;
                    currentColumn++;
                }
            }
        }

        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            setMod(null);
            return true;
        }

        return true;
    }
}