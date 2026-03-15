package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModListWidget extends ObjectSelectionList<ModListWidget.ModRow> {
    private static int getItemHeight(int screenWidth) {
        if (screenWidth < 1280) return 68;
        if (screenWidth < 1920) return 76;
        return 84;
    }

    private static int getIconSize(int screenWidth) {
        if (screenWidth < 1280) return 32;
        if (screenWidth < 1920) return 38;
        return 44;
    }

    private static float getTextScale(int screenWidth) {
        if (screenWidth < 1280) return 0.75f;
        if (screenWidth < 1920) return 0.85f;
        return 0.95f;
    }

    private final int ITEM_HEIGHT;
    private final int PADDING = 12;
    private final int ICON_SIZE;
    private final int COLUMN_GAP = 20;
    private final int BUTTON_GAP = 6;
    private final float TEXT_SCALE;
    private static final float ANIMATION_SPEED = 0.12f;

    private final Map<String, ResourceLocation> iconCache = new ConcurrentHashMap<>();
    private final Map<String, Float> iconLoadProgress = new ConcurrentHashMap<>();
    private ModData selectedMod = null;
    private ModsScreen screen;

    public ModListWidget(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height - top, top, getItemHeight(width));
        this.ITEM_HEIGHT = getItemHeight(width);
        this.ICON_SIZE = getIconSize(width);
        this.TEXT_SCALE = getTextScale(width);
    }

    public void setScreen(ModsScreen screen) {
        this.screen = screen;
    }

    public void setMods(List<ModData> mods) {
        this.clearEntries();
        for (ModData mod : mods) {
            iconLoadProgress.put(mod.modId, 0f);
            loadIconAsync(mod);
        }
        for (int i = 0; i < mods.size(); i += 2) {
            ModData left = mods.get(i);
            ModData right = (i + 1 < mods.size()) ? mods.get(i + 1) : null;
            this.addEntry(new ModRow(left, right, iconCache, this));
        }
    }

    private void loadIconAsync(ModData mod) {
        if (iconCache.containsKey(mod.modId)) {
            iconLoadProgress.put(mod.modId, 1f);
            return;
        }

        ModIconLoader.loadIcon(mod, new ModIconLoader.IconCallback() {
            @Override
            public void onIconLoaded(ResourceLocation icon) {
                iconCache.put(mod.modId, icon);
                iconLoadProgress.put(mod.modId, 1f);
            }

            @Override
            public void onLoadFailed() {
                iconLoadProgress.put(mod.modId, -1f);
            }
        });
    }

    public void setSelectedMod(ModData mod) {
        this.selectedMod = mod;

        if (screen != null && mod != null) {
            screen.showModDetail(mod);
        }
    }

    public ModData getSelectedMod() {
        return selectedMod;
    }

    public void tickAnimations() {
        for (Map.Entry<String, Float> entry : iconLoadProgress.entrySet()) {
            if (entry.getValue() >= 0f && entry.getValue() < 1f) {
                entry.setValue(Math.min(1f, entry.getValue() + 0.05f));
            }
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(1040, this.width - 28);
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 8;
    }

    @Override
    public int getRowLeft() {
        return this.getX() + (this.width - getRowWidth()) / 2;
    }

    @Override
    protected void renderSelection(GuiGraphics guiGraphics, int top, int width, int height, int outerColor, int innerColor) {}

    public class ModRow extends ObjectSelectionList.Entry<ModRow> {
        private final ModData leftMod;
        private final ModData rightMod;
        private final Map<String, ResourceLocation> icons;
        private final ModListWidget parent;

        private float leftHoverProgress = 0f;
        private float rightHoverProgress = 0f;
        private float leftSelectProgress = 0f;
        private float rightSelectProgress = 0f;
        private final Map<String, Float> buttonHoverStates = new HashMap<>();

        public ModRow(ModData left, ModData right, Map<String, ResourceLocation> icons, ModListWidget parent) {
            this.leftMod = left;
            this.rightMod = right;
            this.icons = icons;
            this.parent = parent;
        }

        @Override
        public void render(GuiGraphics gfx, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            parent.tickAnimations();

            int colWidth = (width - COLUMN_GAP) / 2;

            boolean leftHover = isHoveringCard(mouseX, mouseY, left, colWidth, top, height);
            boolean rightHover = rightMod != null && isHoveringCard(mouseX, mouseY, left + colWidth + COLUMN_GAP, colWidth, top, height);

            leftHoverProgress = Mth.lerp(ANIMATION_SPEED, leftHoverProgress, leftHover ? 1f : 0f);
            rightHoverProgress = Mth.lerp(ANIMATION_SPEED, rightHoverProgress, rightHover ? 1f : 0f);

            boolean leftSelected = parent.getSelectedMod() == leftMod;
            boolean rightSelected = rightMod != null && parent.getSelectedMod() == rightMod;

            leftSelectProgress = Mth.lerp(ANIMATION_SPEED * 0.7f, leftSelectProgress, leftSelected ? 1f : 0f);
            rightSelectProgress = Mth.lerp(ANIMATION_SPEED * 0.7f, rightSelectProgress, rightSelected ? 1f : 0f);

            renderModCard(gfx, leftMod, left, top, colWidth, height, mouseX, mouseY, leftHoverProgress, leftSelectProgress);
            if (rightMod != null) {
                renderModCard(gfx, rightMod, left + colWidth + COLUMN_GAP, top, colWidth, height, mouseX, mouseY, rightHoverProgress, rightSelectProgress);
            }
        }

        private boolean isHoveringCard(int mx, int my, int x, int w, int y, int h) {
            int cardL = x + 5;
            int cardR = x + w - 5;
            int cardT = y + 3;
            int cardB = y + h - 3;
            return mx >= cardL && mx <= cardR && my >= cardT && my <= cardB;
        }

        private void renderModCard(GuiGraphics gfx, ModData mod, int x, int y, int w, int h,
                                   int mx, int my, float hoverProgress, float selectProgress) {
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;

            int cardL = x + 5;
            int cardT = y + 3;
            int cardR = x + w - 5;
            int cardB = y + h - 3;
            int cardW = cardR - cardL;
            int cardH = cardB - cardT;

            float totalProgress = Math.max(hoverProgress, selectProgress);
            float pulseEffect = (float)(Math.sin(System.currentTimeMillis() / 500.0) * 0.5 + 0.5);

            for (int i = 0; i < 4; i++) {
                float shadowAlpha = (0.05f - i * 0.01f) * (0.7f + totalProgress * 0.3f);
                int shadowOffset = i + (int)(totalProgress * 1.5f);
                gfx.fill(cardL + shadowOffset, cardT + shadowOffset,
                        cardR + shadowOffset, cardB + shadowOffset,
                        ((int)(shadowAlpha * 255) << 24));
            }

            int bgTop = selectProgress > 0.5f ?
                    lerpColor(0xF02D2D2D, 0xF0606060, selectProgress) :
                    lerpColor(0xF02D2D2D, 0xF0383838, hoverProgress);
            int bgBot = selectProgress > 0.5f ?
                    lerpColor(0xF01D1D1D, 0xF0505050, selectProgress) :
                    lerpColor(0xF01D1D1D, 0xF0282828, hoverProgress);

            gfx.fillGradient(cardL, cardT, cardR, cardB, bgTop, bgBot);

            int borderColor = lerpColor(0xFF404040, 0xFF00FFFF, totalProgress);
            if (selectProgress > 0.9f) {
                borderColor = lerpColor(borderColor, 0xFF00DDFF, pulseEffect * 0.3f);
            }
            gfx.renderOutline(cardL, cardT, cardW, cardH, borderColor);

            if (totalProgress > 0.2f) {
                int glowAlpha = (int)(totalProgress * 50);
                gfx.renderOutline(cardL + 1, cardT + 1, cardW - 2, cardH - 2, (glowAlpha << 24) | 0x00FFFF);
            }

            if (totalProgress > 0.1f) {
                int accentWidth = (int)((cardW - 4) * totalProgress);
                int accentAlpha = (int)((selectProgress > 0.5f ?   0xDD : 0xAA) * totalProgress);
                gfx.fillGradient(cardL + 2, cardT + 2, cardL + 2 + accentWidth, cardT + 5,
                        (accentAlpha << 24) | 0x00FFFF, (accentAlpha << 24) | 0x0099DD);
            }

            int iconX = cardL + PADDING;
            int iconY = cardT + PADDING;

            Float loadProgress = iconLoadProgress.get(mod.modId);
            if (loadProgress == null) loadProgress = 0f;

            ResourceLocation icon = icons.get(mod.modId);
            if (icon != null && loadProgress >= 1f) {
                int glowSize = (int)(2 + totalProgress * 2);
                int glowAlpha = (int)((0x30 + totalProgress * 0x30) * loadProgress);
                int glowColor = selectProgress > 0.5f ?   0x00FFFF : 0x0088CC;
                gfx.fill(iconX - glowSize, iconY - glowSize,
                        iconX + ICON_SIZE + glowSize, iconY + ICON_SIZE + glowSize,
                        (glowAlpha << 24) | glowColor);

                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1f, 1f, 1f, loadProgress);
                gfx.blit(icon, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

                int iconBorderColor = selectProgress > 0.5f ?
                        lerpColor(0xFF00FFFF, 0xFF00DDFF, pulseEffect * 0.4f) :
                        lerpColor(0xFF666666, 0xFF00CCFF, totalProgress);
                gfx.renderOutline(iconX, iconY, ICON_SIZE, ICON_SIZE, iconBorderColor);
            } else {
                gfx.fillGradient(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE,
                        0xFF5A5A5A, 0xFF333333);
                gfx.renderOutline(iconX, iconY, ICON_SIZE, ICON_SIZE, 0xFF707070);

                String letter = mod.name.substring(0, Math.min(1, mod.name.length())).toUpperCase();
                int lw = font.width(letter);
                int letterColor = selectProgress > 0.5f || hoverProgress > 0.5f ?   0x00FFFF :   0xEEEEEE;
                gfx.drawString(font, letter,
                        iconX + (ICON_SIZE - lw) / 2,
                        iconY + (ICON_SIZE - font.lineHeight) / 2,
                        letterColor, true);
            }

            String versionText = "v" + mod.version;
            gfx.pose().pushPose();
            gfx.pose().scale(0.48f, 0.48f, 1.0f);
            int versionWidth = font.width(versionText);
            int versionX = (int)((iconX + (ICON_SIZE - versionWidth * 0.48f) / 2) / 0.48f);
            int versionY = (int)((iconY + ICON_SIZE + 1) / 0.48f);

            int badgeWidth = versionWidth + 6;
            gfx.fill(versionX - 3, versionY - 1, versionX + badgeWidth - 3, versionY + font.lineHeight + 1, 0x88000000);
            gfx.renderOutline(versionX - 3, versionY - 1, badgeWidth, font.lineHeight + 2, 0x33FFFFFF);

            int versionColor = lerpColor(0xBBBBBB, 0xEEEEEE, totalProgress);
            gfx.drawString(font, versionText, versionX, versionY, versionColor, false);
            gfx.pose().popPose();
            int textX = iconX + ICON_SIZE + PADDING;
            int textY = cardT + PADDING + 2;
            int textW = cardW - ICON_SIZE - (PADDING * 2) - 8;

            String name = truncate(font, mod.name, (int)(textW / TEXT_SCALE));

            gfx.pose().pushPose();
            gfx.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
            int scaledTextX = (int)(textX / TEXT_SCALE);
            int scaledTextY = (int)(textY / TEXT_SCALE);

            if (totalProgress > 0.3f) {
                int glowAlpha = (int)(totalProgress * 80);
                gfx.drawString(font, name, scaledTextX, scaledTextY, glowAlpha << 24 | 0x00AAFF, false);
            }

            gfx.drawString(font, name, scaledTextX + 2, scaledTextY + 2, 0xBB000000, false);
            gfx.drawString(font, name, scaledTextX + 1, scaledTextY + 1, 0xDD000000, false);

            int nameColor = selectProgress > 0.5f ?
                    lerpColor(0x00FFFF, 0x00DDFF, pulseEffect * 0.3f) :
                    lerpColor(0xFFFFFF, 0xDDFFFF, hoverProgress);
            gfx.drawString(font, name, scaledTextX, scaledTextY, nameColor, false);
            gfx.pose().popPose();

            textY += (int)(13 * TEXT_SCALE);
            gfx.pose().pushPose();
            float authorScale = TEXT_SCALE * 0.85f;
            gfx.pose().scale(authorScale, authorScale, 1.0f);
            int authorX = (int)(textX / authorScale);
            int authorY = (int)(textY / authorScale);

            String authorIcon = selectProgress > 0.5f ?   "◆" : "✦";
            String authorText = authorIcon + " " + mod.author;
            int authorColor = lerpColor(0xCCCCCC, 0xEEEEEE, totalProgress * 0.6f);
            gfx.drawString(font, truncate(font, authorText, (int)(textW / authorScale)), authorX, authorY, authorColor, false);
            gfx.pose().popPose();

            renderLinkButtons(gfx, font, mod, cardL, cardR, cardB, cardW, mx, my,
                    hoverProgress, selectProgress, pulseEffect);
        }

        private void renderLinkButtons(GuiGraphics gfx, Font font, ModData mod, int cardLeft, int cardRight,
                                       int cardBottom, int cardWidth, int mx, int my,
                                       float cardHover, float cardSelect, float pulse) {
            if (mod.links.isEmpty()) return;

            float buttonTextScale = cardWidth < 300 ? 0.55f : (cardWidth < 400 ? 0.62f : 0.68f);
            int buttonHeight = cardWidth < 300 ? 13 : (cardWidth < 400 ? 15 : 17);
            int buttonPadding = cardWidth < 300 ? 8 : (cardWidth < 400 ? 10 : 12);

            List<Map.Entry<String, String>> linkList = new ArrayList<>(mod.links.entrySet());

            int totalWidth = 0;
            List<Integer> buttonWidths = new ArrayList<>();
            for (Map.Entry<String, String> link : linkList) {
                String label = getLinkLabel(link.getKey());
                int scaledLabelWidth = (int)(font.width(label) * buttonTextScale);
                int btnW = scaledLabelWidth + buttonPadding;
                buttonWidths.add(btnW);
                totalWidth += btnW;
                if (linkList.indexOf(link) < linkList.size() - 1) {
                    totalWidth += BUTTON_GAP;
                }
            }

            int buttonY = cardBottom - buttonHeight - 6;
            int buttonX = cardRight - PADDING - totalWidth;

            if (buttonX < cardLeft + PADDING) {
                buttonX = cardLeft + PADDING;
                int availableWidth = cardRight - cardLeft - (PADDING * 2);
                if (totalWidth > availableWidth) {
                    int usedWidth = 0;
                    int maxButtons = 0;
                    for (int btnW : buttonWidths) {
                        if (usedWidth + btnW + (maxButtons > 0 ? BUTTON_GAP : 0) > availableWidth) break;
                        usedWidth += btnW + (maxButtons > 0 ?   BUTTON_GAP : 0);
                        maxButtons++;
                    }
                    if (maxButtons < linkList.size()) {
                        linkList = linkList.subList(0, maxButtons);
                        buttonWidths = buttonWidths.subList(0, maxButtons);
                    }
                }
            }

            for (int i = 0; i < linkList.size(); i++) {
                Map.Entry<String, String> link = linkList.get(i);
                int btnW = buttonWidths.get(i);

                String buttonId = mod.modId + "_" + link.getKey();
                String label = getLinkLabel(link.getKey());

                boolean btnHover = mx >= buttonX && mx <= buttonX + btnW &&
                        my >= buttonY && my <= buttonY + buttonHeight;

                Float currentHover = buttonHoverStates.get(buttonId);
                if (currentHover == null) currentHover = 0f;
                currentHover = Mth.lerp(ANIMATION_SPEED * 1.5f, currentHover, btnHover ?   1f : 0f);
                buttonHoverStates.put(buttonId, currentHover);

                int shadowAlpha = (int)((0x50 + currentHover * 0x30));
                gfx.fill(buttonX + 1, buttonY + 1, buttonX + btnW + 1, buttonY + buttonHeight + 1, shadowAlpha << 24);

                int btnTop = btnHover ?
                        lerpColor(0xFF00EEFF, 0xFF00CCFF, pulse * 0.4f) :
                        lerpColor(0xFF005588, 0xFF007799, cardHover * 0.5f);
                int btnBot = btnHover ?
                        lerpColor(0xFF00AADD, 0xFF0088BB, pulse * 0.4f) :
                        lerpColor(0xFF003355, 0xFF005577, cardHover * 0.5f);

                gfx.fillGradient(buttonX, buttonY, buttonX + btnW, buttonY + buttonHeight, btnTop, btnBot);

                int btnBorder = btnHover ?  0xFF00FFFF : lerpColor(0xFF006699, 0xFF00AACC, cardHover);
                gfx.renderOutline(buttonX, buttonY, btnW, buttonHeight, btnBorder);

                if (currentHover > 0.3f) {
                    int innerGlow = (int)(currentHover * 80);
                    gfx.renderOutline(buttonX + 1, buttonY + 1, btnW - 2, buttonHeight - 2, (innerGlow << 24) | 0x00FFFF);
                }

                if (btnHover) {
                    gfx.fillGradient(buttonX + 2, buttonY + 1, buttonX + btnW - 2, buttonY + 3,
                            0x5500FFFF, 0x1100FFFF);
                }

                gfx.pose().pushPose();
                gfx.pose().scale(buttonTextScale, buttonTextScale, 1.0f);
                int textPosX = (int)((buttonX + buttonPadding / 2) / buttonTextScale);
                int textPosY = (int)((buttonY + (buttonHeight - font.lineHeight * buttonTextScale) / 2) / buttonTextScale);

                gfx.drawString(font, label, textPosX + 1, textPosY + 1, 0xAA000000, false);
                gfx.drawString(font, label, textPosX, textPosY, 0xFFFFFF, false);
                gfx.pose().popPose();

                buttonX += btnW + BUTTON_GAP;
            }
        }

        private String getLinkLabel(String linkType) {
            return switch (linkType) {
                case "homepage" -> "🏠 Home";
                case "sources" -> "📦 Source";
                case "issues" -> "🐛 Issues";
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
            return sb + dots;
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

        @Override
        public Component getNarration() {
            return Component.literal(leftMod.name + (rightMod != null ? " and " + rightMod.name :   ""));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                int rowLeft = parent.getRowLeft();
                int rowWidth = parent.getRowWidth();
                int colWidth = (rowWidth - COLUMN_GAP) / 2;

                if (checkCardClick(mouseX, mouseY, rowLeft, colWidth, leftMod)) return true;
                if (rightMod != null) {
                    int rightCardX = rowLeft + colWidth + COLUMN_GAP;
                    return checkCardClick(mouseX, mouseY, rightCardX, colWidth, rightMod);
                }
            }
            return false;
        }

        private boolean checkCardClick(double mouseX, double mouseY, int cardX, int cardW, ModData mod) {
            int cardL = cardX + 5;
            int cardR = cardX + cardW - 5;

            if (mouseX < cardL || mouseX > cardR) return false;

            Font font = minecraft.font;

            int rowIndex = 0;
            for (ModRow row : parent.children()) {
                if (row == this) break;
                rowIndex++;
            }

            int rowTop = parent.getY() + parent.headerHeight + (rowIndex * ITEM_HEIGHT) - (int)parent.getScrollAmount();
            int cardT = rowTop + 3;
            int cardB = rowTop + ITEM_HEIGHT - 3;
            int buttonHeight = cardW < 300 ? 13 : (cardW < 400 ? 15 : 17);
            int buttonY = cardB - buttonHeight - 6;

            float buttonTextScale = cardW < 300 ? 0.55f : (cardW < 400 ? 0.62f : 0.68f);
            int buttonPadding = cardW < 300 ? 8 : (cardW < 400 ? 10 : 12);

            List<Map.Entry<String, String>> linkList = new ArrayList<>(mod.links.entrySet());
            int totalWidth = 0;
            List<Integer> buttonWidths = new ArrayList<>();

            for (Map.Entry<String, String> link : linkList) {
                String label = getLinkLabel(link.getKey());
                int scaledLabelWidth = (int)(font.width(label) * buttonTextScale);
                int btnW = scaledLabelWidth + buttonPadding;
                buttonWidths.add(btnW);
                totalWidth += btnW;
                if (linkList.indexOf(link) < linkList.size() - 1) {
                    totalWidth += BUTTON_GAP;
                }
            }

            int buttonX = cardR - PADDING - totalWidth;
            if (buttonX < cardL + PADDING) {
                buttonX = cardL + PADDING;
            }

            for (int i = 0; i < linkList.size() && i < buttonWidths.size(); i++) {
                int btnW = buttonWidths.get(i);
                if (buttonX + btnW > cardR - PADDING) break;

                if (mouseX >= buttonX && mouseX <= buttonX + btnW &&
                        mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                    Util.getPlatform().openUri(linkList.get(i).getValue());
                    return true;
                }
                buttonX += btnW + BUTTON_GAP;
            }

            parent.setSelectedMod(mod);
            return true;
        }
    }
}