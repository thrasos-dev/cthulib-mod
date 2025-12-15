package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BrightnessImageButton extends AbstractWidget {
    private final ResourceLocation texture;
    private final OnPress onPress;

    public BrightnessImageButton(int x, int y, int width, int height,
                                 ResourceLocation texture, OnPress onPress, Component message) {
        super(x, y, width, height, message);
        this.texture = texture;
        this.onPress = onPress;
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float brightness = this.isHovered() ? 0.6f : 1.0f;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(brightness, brightness, brightness, this.alpha);

        graphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPress.onPress(this);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(BrightnessImageButton button);
    }
}