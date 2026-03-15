package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.cthulib.client.base.DraggableTitleScreenWidget;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;
import net.pixeldreamstudios.cthulib.util.ChangelogCache;

public class ChangelogButton extends DraggableTitleScreenWidget {
    private final OnPress onPress;
    private float glowAnimation = 0.0f;
    private final AnimatedTooltip updateTooltip;

    private final int baseWidth;
    private final int baseHeight;
    
    public ChangelogButton(int x, int y, int width, int height, String texture, OnPress onPress) {
        super(x, y, width, height, Component.literal("Changelog"), UIElementPositionManager.ElementType.CHANGELOG_BUTTON);
        this.baseWidth = width;
        this.baseHeight = height;
        this.onPress = onPress;
        this.updateTooltip = new AnimatedTooltip(Component.literal("New update available!"));
        updatePosition();
    }
    
    private void updatePosition() {
        CthuLibConfig config = CthuLibConfig.getInstance();
        int scaledW = (int)(config.changelogButtonWidth * config.changelogButtonScale);
        int scaledH = (int)(config.changelogButtonHeight * config.changelogButtonScale);
        UIElementPositionManager.PositionInfo pos = 
            UIElementPositionManager.calculateAdvancedPosition(
                UIElementPositionManager.ElementType.CHANGELOG_BUTTON,
                scaledW, scaledH,
                config.changelogButtonScaleWithScreen,
                config.changelogButtonMinScale,
                config.changelogButtonMaxScale,
                config.changelogButtonStartFromCenterX,
                config.changelogButtonStartFromCenterY,
                config.changelogButtonStartFromLeftX,
                config.changelogButtonStartFromRightX
            );
        
        this.width = pos.width;
        this.height = pos.height;
        this.setX(pos.x);
        this.setY(pos.y);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updatePosition();
        
        updateHoverAnimation(mouseX, mouseY);
        updatePressAnimation();
        updatePulseAnimation(0.04f);
        CthuLibConfig shineCfg = CthuLibConfig.getInstance();
        if (shineCfg.shineEffectEnabled) glowAnimation += shineCfg.shineAnimationSpeed * 0.05f;
        
        float pulse = (float)(Math.sin(pulseAnimation) * 0.5 + 0.5);
        
        int depthOffset = (int)(pressAnimation * 2);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int renderX = this.getX() + depthOffset;
        int renderY = this.getY() + depthOffset;
        
        renderShadow(graphics, renderX, renderY);
        renderGlow(graphics, renderX, renderY, pulse);
        renderButton(graphics, renderX, renderY);
        renderText(graphics, renderX, renderY);
        if (shineCfg.shineEffectEnabled) ShineEffect.renderShine(graphics, renderX, renderY, this.width, this.height, glowAnimation, hoverAnimation);
        renderNotificationBadge(graphics, renderX, renderY);

        if (this.isHovered()) {
            CthuLibConfig cfg = CthuLibConfig.getInstance();
            String currentVersion = ChangelogCache.getCurrentVersion();
            boolean hasUnread = cfg.showChangelogNotification && currentVersion != null
                    && !currentVersion.equals(cfg.lastReadChangelogVersion)
                    && !cfg.lastReadChangelogVersion.isEmpty();
            if (hasUnread && updateTooltip != null) {
                Minecraft mc = Minecraft.getInstance();
                updateTooltip.render(graphics, mc.font, mouseX, mouseY,
                        mc.getWindow().getGuiScaledWidth(),
                        mc.getWindow().getGuiScaledHeight());
            }
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderShadow(GuiGraphics graphics, int x, int y) {
        for (int i = 6; i >= 1; i--) {
            float layerAlpha = 0.05f + (i * 0.01f);
            int shadowColor = (int)(layerAlpha * 255) << 24;
            graphics.fill(x + i, y + i, x + this.width + i, y + this.height + i, shadowColor);
        }
    }

    private void renderGlow(GuiGraphics graphics, int x, int y, float pulse) {
        float glowIntensity = (hoverAnimation * 0.4f) + (pulse * 0.2f);
        
        for (int i = 6; i > 0; i--) {
            float layerAlpha = (glowIntensity / i) * 0.5f;
            int glowColor = (int)(layerAlpha * 255) << 24 | 0xFFAA00;
            
            graphics.fill(x - i, y - i, x + this.width + i, y - i + 1, glowColor);
            graphics.fill(x - i, y + this.height + i - 1, x + this.width + i, y + this.height + i, glowColor);
            graphics.fill(x - i, y - i, x - i + 1, y + this.height + i, glowColor);
            graphics.fill(x + this.width + i - 1, y - i, x + this.width + i, y + this.height + i, glowColor);
        }
    }

    private void renderButton(GuiGraphics graphics, int x, int y) {
        int bgColor = 0xCC000000 + (int)(hoverAnimation * 0x33) * 0x010101;
        graphics.fill(x, y, x + this.width, y + this.height, bgColor);
        
        int topGradient = (int)((0.3f + hoverAnimation * 0.2f) * 255) << 24 | 0xFFAA00;
        graphics.fillGradient(x + 2, y + 2, x + this.width - 2, y + this.height / 2, topGradient, 0);
        
        int borderColor = 0xFF666666 + (int)(hoverAnimation * 0x99) * 0x010000;
        graphics.fill(x, y, x + this.width, y + 1, borderColor);
        graphics.fill(x, y + this.height - 1, x + this.width, y + this.height, borderColor);
        graphics.fill(x, y, x + 1, y + this.height, borderColor);
        graphics.fill(x + this.width - 1, y, x + this.width, y + this.height, borderColor);
    }

    private void renderText(GuiGraphics graphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        String text = this.getMessage().getString();
        
        int textWidth = font.width(text);
        int textX = x + (this.width - textWidth) / 2;
        int textY = y + (this.height - font.lineHeight) / 2;
        
        int textColor = 0xFFFFFF + (int)(hoverAnimation * 0x66) * 0x010000;
        
        int shadowColor = 0x40000000;
        graphics.drawString(font, text, textX + 1, textY + 1, shadowColor, false);
        
        graphics.drawString(font, text, textX, textY, textColor, false);
    }
    
    private void renderNotificationBadge(GuiGraphics graphics, int x, int y) {
        CthuLibConfig config = CthuLibConfig.getInstance();
        if (!config.showChangelogNotification) {
            return;
        }
        
        String currentVersion = ChangelogCache.getCurrentVersion();
        boolean hasUnread = currentVersion != null && !currentVersion.equals(config.lastReadChangelogVersion) && !config.lastReadChangelogVersion.isEmpty();
        
        if (!hasUnread) {
            return;
        }
        
        int badgeSize = 8;
        int badgeX = x + this.width - badgeSize - 2;
        int badgeY = y + 2;
        
        float badgePulse = (float)(Math.sin(pulseAnimation * 2) * 0.3 + 0.7);
        
        graphics.fill(badgeX + 1, badgeY + 1, badgeX + badgeSize + 1, badgeY + badgeSize + 1, 0x80000000);
        
        int badgeColor = (int)(badgePulse * 255) << 24 | 0xFF3333;
        graphics.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, badgeColor);
        
        int highlightColor = (int)(badgePulse * 128) << 24 | 0xFFFFFF;
        graphics.fill(badgeX + 1, badgeY + 1, badgeX + badgeSize - 1, badgeY + 3, highlightColor);
    }

    @Override
    protected boolean onWidgetClicked(double mouseX, double mouseY, int button) {
        if (!isDragging()) {
            triggerPress();
            this.onPress.onPress(this);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging()) {
            CthuLibConfig config = CthuLibConfig.getInstance();
            UIElementPositionManager.endDragAdvanced(
                UIElementPositionManager.ElementType.CHANGELOG_BUTTON,
                "Changelog Button",
                baseWidth, baseHeight,
                config.changelogButtonScaleWithScreen,
                config.changelogButtonMinScale,
                config.changelogButtonMaxScale,
                config.changelogButtonStartFromCenterX,
                config.changelogButtonStartFromCenterY,
                config.changelogButtonStartFromLeftX,
                config.changelogButtonStartFromRightX
            );
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    protected String getWidgetName() {
        return "Changelog Button";
    }

    public interface OnPress {
        void onPress(ChangelogButton button);
    }
}
