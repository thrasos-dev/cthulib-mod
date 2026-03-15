package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.client.base.DraggableTitleScreenWidget;

public class BrightnessImageButton extends DraggableTitleScreenWidget {
    private final ResourceLocation texture;
    private final OnPress onPress;
    private final int baseWidth;
    private final int baseHeight;

    public BrightnessImageButton(int x, int y, int width, int height,
                                 ResourceLocation texture, OnPress onPress, Component message,
                                 UIElementPositionManager.ElementType elementType) {
        super(x, y, width, height, message, elementType);
        this.baseWidth = width;
        this.baseHeight = height;
        this.texture = texture;
        this.onPress = onPress;
        this.setTooltip(Tooltip.create(message));
        updatePosition();
    }
    
    public BrightnessImageButton(int x, int y, int width, int height,
                                 ResourceLocation texture, OnPress onPress, Component message) {
        this(x, y, width, height, texture, onPress, message, null);
    }
    
    private void updatePosition() {
        if (elementType != null) {
            UIElementPositionManager.PositionInfo pos = 
                UIElementPositionManager.calculatePosition(elementType, baseWidth, baseHeight);
            
            this.width = pos.width;
            this.height = pos.height;
            this.setX(pos.x);
            this.setY(pos.y);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updatePosition();
        
        float brightness = this.isHovered() ? 0.6f : 1.0f;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(brightness, brightness, brightness, this.alpha);

        graphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected boolean onWidgetClicked(double mouseX, double mouseY, int button) {
        if (!isDragging()) {
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
        void onPress(BrightnessImageButton button);
    }
}