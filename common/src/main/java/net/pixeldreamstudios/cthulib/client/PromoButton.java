package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.client.base.DraggableTitleScreenWidget;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

public class PromoButton extends DraggableTitleScreenWidget {
    private final OnPress onPress;
    private final ResourceLocation fallbackTexture;
    private final String imageUrl;
    private ResourceLocation texture;
    private boolean imageLoadAttempted = false;
    private float glowAnimation = 0.0f;
    private final AnimatedTooltip animatedTooltip;
    
    private final int baseWidth;
    private final int baseHeight;
    
    public PromoButton(int x, int y, int width, int height, String imageUrl, ResourceLocation fallbackTexture,
                       OnPress onPress, Component message,
                       boolean enableScaling, float minScale, float maxScale,
                       boolean startFromCenterX, boolean startFromCenterY,
                       boolean startFromLeftX, boolean startFromRightX) {
        super(x, y, width, height, message, UIElementPositionManager.ElementType.PROMO_BUTTON);
        this.baseWidth = width;
        this.baseHeight = height;
        this.imageUrl = imageUrl;
        this.fallbackTexture = fallbackTexture;
        this.texture = fallbackTexture;
        this.onPress = onPress;
        this.animatedTooltip = new AnimatedTooltip(message);
        updatePosition();
    }
    
    private void updatePosition() {
        CthuLibConfig config = CthuLibConfig.getInstance();
        int scaledW = (int)(config.promoButtonWidth * config.promoButtonScale);
        int scaledH = (int)(config.promoButtonHeight * config.promoButtonScale);
        UIElementPositionManager.PositionInfo pos = 
            UIElementPositionManager.calculateAdvancedPosition(
                UIElementPositionManager.ElementType.PROMO_BUTTON,
                scaledW, scaledH,
                config.promoButtonScaleWithScreen,
                config.promoButtonMinScale,
                config.promoButtonMaxScale,
                config.promoButtonStartFromCenterX,
                config.promoButtonStartFromCenterY,
                config.promoButtonStartFromLeftX,
                config.promoButtonStartFromRightX,
                config.promoButtonUseAnchor
            );
        
        this.width = pos.width;
        this.height = pos.height;
        this.setX(pos.x);
        this.setY(pos.y);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updatePosition();
        if (!imageLoadAttempted && imageUrl != null && !imageUrl.isEmpty()) {
            imageLoadAttempted = true;
            loadImageFromUrl();
        }

        updateHoverAnimation(mouseX, mouseY);
        CthuLibConfig shineCfg = CthuLibConfig.getInstance();
        if (shineCfg.shineEffectEnabled) glowAnimation += shineCfg.shineAnimationSpeed * 0.05f;

        float brightness = this.isHovered() ? 0.6f : 1.0f;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(brightness, brightness, brightness, this.alpha);

        graphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);

        if (shineCfg.shineEffectEnabled) ShineEffect.renderShine(graphics, this.getX(), this.getY(), this.width, this.height, glowAnimation, hoverAnimation);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (this.isHovered() && animatedTooltip != null) {
            animatedTooltip.render(graphics, Minecraft.getInstance().font, mouseX, mouseY,
                    Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                    Minecraft.getInstance().getWindow().getGuiScaledHeight());
        }
    }
    private void loadImageFromUrl() {
        Thread loadThread = new Thread(() -> {
            try {
                URI uri = new URI(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                try (InputStream in = connection.getInputStream()) {
                    BufferedImage bufferedImage = ImageIO.read(in);
                    if (bufferedImage != null) {
                        NativeImage nativeImage = convertToNativeImage(bufferedImage);
                        Minecraft.getInstance().execute(() -> {
                            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                            this.texture = Minecraft.getInstance().getTextureManager()
                                .register("promo_button_dynamic", dynamicTexture);
                        });
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                this.texture = fallbackTexture;
            }
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private NativeImage convertToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, true);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }

        return nativeImage;
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging()) {
            CthuLibConfig config = CthuLibConfig.getInstance();
            int scaledW = (int)(config.promoButtonWidth * config.promoButtonScale);
            int scaledH = (int)(config.promoButtonHeight * config.promoButtonScale);
            UIElementPositionManager.endDragAdvanced(
                UIElementPositionManager.ElementType.PROMO_BUTTON,
                "Promo Button",
                scaledW, scaledH,
                config.promoButtonScaleWithScreen,
                config.promoButtonMinScale,
                config.promoButtonMaxScale,
                config.promoButtonStartFromCenterX,
                config.promoButtonStartFromCenterY,
                config.promoButtonStartFromLeftX,
                config.promoButtonStartFromRightX,
                config.promoButtonUseAnchor
            );
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    protected String getWidgetName() {
        return "Promo Button";
    }
    
    public void updatePositionExternal() {
        updatePosition();
    }
    
    public void renderAnimatedTooltip(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (this.isHovered() && animatedTooltip != null) {
            animatedTooltip.render(graphics, Minecraft.getInstance().font, mouseX, mouseY, screenWidth, screenHeight);
        }
    }

    public static String getPromoUrlWithFallback() {
        return "";
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(PromoButton button);
    }
}
