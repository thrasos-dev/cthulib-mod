package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.gui.GuiGraphics;

public class ShineEffect {
    
    public static void renderShine(GuiGraphics graphics, int x, int y, int width, int height, float glowAnimation, float hoverAnimation) {
        float shinePos = (float)(Math.sin(glowAnimation) * 0.5 + 0.5);
        
        for (int dx = 0; dx < width; dx += 2) {
            for (int dy = 0; dy < height; dy += 2) {
                float diagonalPos = ((float)dx / width + (float)dy / height) / 2.0f;
                float distanceFromShine = Math.abs(diagonalPos - shinePos);
                
                if (distanceFromShine < 0.2f) {
                    float intensity = (1.0f - (distanceFromShine / 0.2f)) * 0.3f * hoverAnimation;
                    int shineColor = (int)(intensity * 255) << 24 | 0xFFFFFF;
                    graphics.fill(x + dx, y + dy, x + dx + 2, y + dy + 2, shineColor);
                }
            }
        }
    }
}
