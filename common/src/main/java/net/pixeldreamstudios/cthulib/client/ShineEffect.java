package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public class ShineEffect {

    private static final int CELL = 2;
    private static final float BAND = 0.2f;
    private static final float STRENGTH = 0.3f;
    private static final int MAX_ALPHA = 255;
    private static final int ALPHA_SHIFT = 24;
    private static final int WHITE = 0xFFFFFF;

    public static void renderShine(GuiGraphics graphics, int x, int y, int width, int height, float glowAnimation, float hoverAnimation) {
        if (hoverAnimation <= 0.0f || width <= 0 || height <= 0) {
            return;
        }
        float shinePos = (float)(Math.sin(glowAnimation) * 0.5 + 0.5);
        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());

        for (int dx = 0; dx < width; dx += CELL) {
            for (int dy = 0; dy < height; dy += CELL) {
                float diagonalPos = ((float)dx / width + (float)dy / height) / 2.0f;
                float distanceFromShine = Math.abs(diagonalPos - shinePos);

                if (distanceFromShine < BAND) {
                    float intensity = (1.0f - (distanceFromShine / BAND)) * STRENGTH * hoverAnimation;
                    int alpha = (int)(intensity * MAX_ALPHA);
                    if (alpha <= 0) {
                        continue;
                    }
                    int shineColor = alpha << ALPHA_SHIFT | WHITE;
                    float left = x + dx;
                    float top = y + dy;
                    float right = left + CELL;
                    float bottom = top + CELL;
                    buffer.addVertex(pose, left, top, 0.0f).setColor(shineColor);
                    buffer.addVertex(pose, left, bottom, 0.0f).setColor(shineColor);
                    buffer.addVertex(pose, right, bottom, 0.0f).setColor(shineColor);
                    buffer.addVertex(pose, right, top, 0.0f).setColor(shineColor);
                }
            }
        }
        graphics.flush();
    }
}
