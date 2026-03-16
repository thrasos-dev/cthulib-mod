package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.client.base.DraggableTitleScreenWidget;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;
import net.pixeldreamstudios.cthulib.client.ShineEffect;

public class ModListButton extends DraggableTitleScreenWidget {
    private final ResourceLocation texture;
    private final OnPress onPress;
    private final int baseWidth;
    private final int baseHeight;
    private float glowAnimation = 0.0f;
    private final AnimatedTooltip tooltip;

    public ModListButton(int x, int y, int width, int height,
                                 ResourceLocation texture, OnPress onPress, Component message,
                                 UIElementPositionManager.ElementType elementType) {
        super(x, y, width, height, message, elementType);
        this.baseWidth = width;
        this.baseHeight = height;
        this.texture = texture;
        this.onPress = onPress;
        this.tooltip = new AnimatedTooltip(message);
        updatePosition();
    }
    
    public ModListButton(int x, int y, int width, int height,
                                 ResourceLocation texture, OnPress onPress, Component message) {
        this(x, y, width, height, texture, onPress, message, null);
    }
    
    private void updatePosition() {
        if (elementType != null) {
            if (elementType == UIElementPositionManager.ElementType.BRIGHTNESS_BUTTON_TITLE) {
                CthuLibConfig cfg = CthuLibConfig.getInstance();
                int scaledW = (int)(cfg.modlistButtonWidth * cfg.modlistButtonScale);
                int scaledH = (int)(cfg.modlistButtonHeight * cfg.modlistButtonScale);
                UIElementPositionManager.PositionInfo pos =
                    UIElementPositionManager.calculateAdvancedPosition(
                        elementType, scaledW, scaledH,
                        cfg.modlistButtonScaleWithScreen,
                        cfg.modlistButtonMinScale,
                        cfg.modlistButtonMaxScale,
                        false, false, false, false
                    );
                this.width = pos.width;
                this.height = pos.height;
                this.setX(pos.x);
                this.setY(pos.y);
            } else {
                UIElementPositionManager.PositionInfo pos =
                    UIElementPositionManager.calculatePosition(elementType, baseWidth, baseHeight);
                this.width = pos.width;
                this.height = pos.height;
                this.setX(pos.x);
                this.setY(pos.y);
            }
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updatePosition();

        updateHoverAnimation(mouseX, mouseY);
        updatePressAnimation();
        updatePulseAnimation(0.04f);
        CthuLibConfig cfg = CthuLibConfig.getInstance();
        if (cfg.shineEffectEnabled) glowAnimation += cfg.shineAnimationSpeed * 0.05f;

        float pulse = (float)(Math.sin(pulseAnimation) * 0.5 + 0.5);
        int depthOffset = (int)(pressAnimation * 2);

        int renderX = this.getX() + depthOffset;
        int renderY = this.getY() + depthOffset;

        renderShadow(graphics, renderX, renderY);
        renderGlow(graphics, renderX, renderY, pulse);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
        graphics.blit(texture, renderX, renderY, 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (cfg.shineEffectEnabled) {
            ShineEffect.renderShine(graphics, renderX, renderY, this.width, this.height, glowAnimation, hoverAnimation);
        }

        if (this.isHovered()) {
            Minecraft mc = Minecraft.getInstance();
            tooltip.render(graphics, mc.font, mouseX, mouseY,
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging()) {
            if (elementType == UIElementPositionManager.ElementType.BRIGHTNESS_BUTTON_TITLE) {
                CthuLibConfig config = CthuLibConfig.getInstance();
                int scaledW = (int)(config.modlistButtonWidth * config.modlistButtonScale);
                int scaledH = (int)(config.modlistButtonHeight * config.modlistButtonScale);
                UIElementPositionManager.endDragAdvanced(
                    elementType, "Modlist Button",
                    scaledW, scaledH,
                    config.modlistButtonScaleWithScreen,
                    config.modlistButtonMinScale,
                    config.modlistButtonMaxScale,
                    false, false, false, false
                );
            } else {
                UIElementPositionManager.endDrag(elementType, getWidgetName());
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
    protected String getWidgetName() {
        return "Modlist Button";
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(ModListButton button);
    }
}