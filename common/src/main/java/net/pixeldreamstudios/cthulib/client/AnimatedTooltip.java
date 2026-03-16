package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimatedTooltip {
    private final Component message;
    private final List<Star> stars = new ArrayList<>();
    private float shineOffset = 0.0f;
    private float smoothX = Float.NaN;
    private float smoothY = Float.NaN;
    private final Random random = new Random();

    private static class Star {
        float x, y;
        float alpha;
        float speed;
        float phase;

        Star(float x, float y, float speed, float phase) {
            this.x = x;
            this.y = y;
            this.alpha = 0.0f;
            this.speed = speed;
            this.phase = phase;
        }
    }

    public AnimatedTooltip(Component message) {
        this.message = message;
        for (int i = 0; i < 15; i++) {
            stars.add(new Star(
                    random.nextFloat(),
                    random.nextFloat(),
                    0.5f + random.nextFloat() * 1.5f,
                    random.nextFloat() * (float)Math.PI * 2
            ));
        }
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (message == null) return;

        String text = message.getString();
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;

        int padding = 8;
        int tooltipWidth = textWidth + padding * 2;
        int tooltipHeight = textHeight + padding * 2;

        if (Float.isNaN(smoothX)) {
            smoothX = mouseX;
            smoothY = mouseY;
        } else {
            smoothX = Mth.lerp(0.3f, smoothX, mouseX);
            smoothY = Mth.lerp(0.3f, smoothY, mouseY);
        }

        int tooltipX = (int)smoothX + 12;
        int tooltipY = (int)smoothY - 12;

        if (tooltipX + tooltipWidth > screenWidth - 4) {
            tooltipX = screenWidth - tooltipWidth - 4;
        }
        if (tooltipY + tooltipHeight > screenHeight - 4) {
            tooltipY = screenHeight - tooltipHeight - 4;
        }
        if (tooltipX < 4) tooltipX = 4;
        if (tooltipY < 4) tooltipY = 4;

        shineOffset += 0.02f;
        if (shineOffset > 1.0f) shineOffset = 0.0f;

        for (Star star : stars) {
            star.phase += star.speed * 0.05f;
            star.alpha = (float)Math.sin(star.phase) * 0.5f + 0.5f;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 400.0f);

        renderAnimatedBackground(graphics, tooltipX, tooltipY, tooltipWidth, tooltipHeight);

        renderStars(graphics, tooltipX, tooltipY, tooltipWidth, tooltipHeight);

        renderShine(graphics, tooltipX, tooltipY, tooltipWidth, tooltipHeight);

        graphics.drawString(font, text, tooltipX + padding, tooltipY + padding, 0xFFFFFFFF, true);

        graphics.pose().popPose();
    }

    private void renderAnimatedBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        for (int i = 3; i > 0; i--) {
            int glowAlpha = 40 - (i * 10);
            int glowColor = (glowAlpha << 24) | 0x6A3F9E;
            graphics.fill(x - i, y - i, x + width + i, y - i + 1, glowColor);
            graphics.fill(x - i, y + height + i - 1, x + width + i, y + height + i, glowColor);
            graphics.fill(x - i, y - i, x - i + 1, y + height + i, glowColor);
            graphics.fill(x + width + i - 1, y - i, x + width + i, y + height + i, glowColor);
        }

        int steps = 20;
        int stepHeight = height / steps;
        for (int i = 0; i < steps; i++) {
            float progress = (float)i / steps;

            int r = (int)(106 * (1.0f - progress * 0.5f));
            int g = (int)(63 * (1.0f - progress * 0.6f));
            int b = (int)(158 + (progress * 50));

            float wave = (float)Math.sin((progress * 4 + shineOffset * 2) * Math.PI) * 0.1f;
            r = Mth.clamp((int)(r * (1.0f + wave)), 0, 255);
            g = Mth.clamp((int)(g * (1.0f + wave)), 0, 255);
            b = Mth.clamp((int)(b * (1.0f + wave)), 0, 255);

            int color = 0xF0000000 | (r << 16) | (g << 8) | b;

            int stepY = y + i * stepHeight;
            int nextStepY = (i == steps - 1) ? y + height : y + (i + 1) * stepHeight;
            graphics.fill(x, stepY, x + width, nextStepY, color);
        }

        graphics.fill(x, y, x + width, y + 1, 0xFF8B4FB3);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF4A2969);
        graphics.fill(x, y, x + 1, y + height, 0xFF7A3F9E);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF4A2969);
    }

    private void renderStars(GuiGraphics graphics, int x, int y, int width, int height) {
        for (Star star : stars) {
            int starX = x + (int)(star.x * width);
            int starY = y + (int)(star.y * height);

            int starAlpha = (int)(star.alpha * 200);
            if (starAlpha < 5) continue;

            int starColor = (starAlpha << 24) | 0xFFFFFF;

            graphics.fill(starX, starY, starX + 1, starY + 1, starColor);

            int armAlpha = starAlpha / 2;
            int armColor = (armAlpha << 24) | 0xFFFFAA;
            graphics.fill(starX - 1, starY, starX, starY + 1, armColor);
            graphics.fill(starX + 1, starY, starX + 2, starY + 1, armColor);
            graphics.fill(starX, starY - 1, starX + 1, starY, armColor);
            graphics.fill(starX, starY + 1, starX + 1, starY + 2, armColor);
        }
    }

    private void renderShine(GuiGraphics graphics, int x, int y, int width, int height) {
        int shinePos = (int)(shineOffset * (width + height));
        int shineWidth = 30;

        for (int i = 0; i < shineWidth; i++) {
            float shineFade = 1.0f - ((float)Math.abs(i - shineWidth / 2) / (shineWidth / 2));
            int shineAlpha = (int)(shineFade * 40);
            int shineColor = (shineAlpha << 24) | 0xFFFFFF;

            for (int py = 0; py < height; py++) {
                int px = shinePos - py + i - shineWidth / 2;
                if (px >= 0 && px < width) {
                    graphics.fill(x + px, y + py, x + px + 1, y + py + 1, shineColor);
                }
            }
        }
    }

    public Component getMessage() {
        return message;
    }
}