package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModsScreen extends Screen {
    private final Screen parentScreen;
    private ModListWidget modList;
    private DynamicTexture logoTex;
    private ResourceLocation logoLoc;
    private DynamicTexture wikiLogoTex;
    private ResourceLocation wikiLogoLoc;
    private DynamicTexture discordLogoTex;
    private ResourceLocation discordLogoLoc;
    private DynamicTexture backgroundTex;
    private ResourceLocation backgroundLoc;
    private int logoW, logoH;
    private List<ModData> mods;
    private ModDetailPanel detailPanel;
    private int closeButtonX, closeButtonY;
    private static final int CLOSE_BUTTON_SIZE = 20;
    private static final int CLOSE_BUTTON_PADDING = 6;

    private static final int LOGO_MAX_WITH_NAME = 40;
    private static final int LOGO_MAX_WITHOUT_NAME = 64;
    private static final int HEADER_HEIGHT = 78;
    private static final int MAX_TEXTURE_SIZE = 2048;
    
    // Static cache to prevent re-loading mod data on every screen open
    private static List<ModData> cachedMods = null;
    private static ModListWidget cachedModList = null;
    private static int lastScreenWidth = -1;
    private static int lastScreenHeight = -1;
    
    /**
     * Call this to invalidate cache when mods change or config changes
     */
    public static void invalidateCache() {
        cachedMods = null;
        cachedModList = null;
        lastScreenWidth = -1;
        lastScreenHeight = -1;
    }

    public ModsScreen(Screen parentScreen) {
        super(Component.literal("Mods"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        CthuLibConfig cfg = CthuLibConfig.get();

        // Preload mod data only if not already cached
        if (!ModCollector.isDataReady()) {
            ModCollector.preloadModData();
        }

        // Only recreate mod list on first init, otherwise just reuse
        boolean isFirstInit = (lastScreenWidth == -1 && lastScreenHeight == -1);
        
        // Update tracked screen size
        lastScreenWidth = width;
        lastScreenHeight = height;

        // Reuse cached mod data
        if (cachedMods == null) {
            cachedMods = ModCollector.getModsByConfiguredAuthors();
        }
        mods = cachedMods;

        int listTop = 45;

        boolean hasHeader = (!cfg.logoPath.isEmpty() || (cfg.showName && !cfg.name.isEmpty()));

        if (hasHeader) {
            listTop = HEADER_HEIGHT;
            if (!cfg.logoPath.isEmpty()) {
                loadLogoAsync();
            }
        }

        if (!cfg.backgroundPath.isEmpty()) {
            loadBackgroundAsync();
        }

        // Reuse cached mod list widget - create only once
        if (cachedModList == null || isFirstInit) {
            cachedModList = new ModListWidget(minecraft, width, height, listTop, height, 68);
            cachedModList.setMods(mods);
        }
        modList = cachedModList;
        addWidget(modList);
        modList.setScreen(this);
        
        closeButtonX = width - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PADDING;
        closeButtonY = CLOSE_BUTTON_PADDING;

        if (!   cfg.wikiLink.isEmpty()) {
            int defaultWikiX = 6;
            int defaultWikiY = hasHeader ? 50 : 6;
            int wikiX = defaultWikiX + cfg.wikiButtonX;
            int wikiY = defaultWikiY + cfg.wikiButtonY;

            if (! cfg.wikiLogo.isEmpty()) {
                loadButtonLogoAsync(cfg.wikiLogo, wikiX, wikiY, cfg.wikiLink, "Wiki");
            } else {
                addRenderableWidget(Button.builder(Component.literal("📚 Wiki"),
                                b -> Util.getPlatform().openUri(cfg.wikiLink))
                        .bounds(wikiX, wikiY, 80, 20).build());
            }
        }

        if (! cfg.discordLink.isEmpty()) {
            int defaultDiscordX = width - 26;
            int defaultDiscordY = hasHeader ? 50 : 6;
            int discordX = defaultDiscordX + cfg.discordButtonX;
            int discordY = defaultDiscordY + cfg.discordButtonY;

            if (!    cfg.discordLogo.isEmpty()) {
                loadButtonLogoAsync(cfg.discordLogo, discordX, discordY, cfg.discordLink, "Discord");
            } else {
                addRenderableWidget(Button.builder(Component.literal("💬 Discord"),
                                b -> Util.getPlatform().openUri(cfg.discordLink))
                        .bounds(discordX, discordY, 80, 20).build());
            }
        }

        detailPanel = new ModDetailPanel(this);
    }

    private void loadButtonLogoAsync(String path, int x, int y, String link, String label) {
        CompletableFuture.supplyAsync(() -> {
            try {
                ResourceLocation rl = ResourceLocation.parse(path);
                InputStream is = minecraft.getResourceManager().getResource(rl).orElseThrow().open();
                BufferedImage img = ImageIO.read(is);
                is.close();
                return img;
            } catch (Exception e) {
                return null;
            }
        }).thenAcceptAsync(img -> {
            if (img != null) {
                NativeImage ni =
                        new NativeImage(img.getWidth(), img.getHeight(), true);

                for (int iy = 0; iy < img.getHeight(); iy++) {
                    for (int ix = 0; ix < img.getWidth(); ix++) {
                        int argb = img.getRGB(ix, iy);
                        ni.setPixelRGBA(ix, iy,
                                ((argb & 0xFF000000)) |
                                        ((argb & 0xFF) << 16) |
                                        ((argb & 0xFF00)) |
                                        ((argb & 0xFF0000) >> 16));
                    }
                }

                DynamicTexture dt = new DynamicTexture(ni);
                ResourceLocation loc = minecraft.getTextureManager().register("cthulib_button_" + path.hashCode(), dt);

                if (label.equals("Wiki")) {
                    wikiLogoLoc = loc;
                    wikiLogoTex = dt;
                } else if (label.equals("Discord")) {
                    discordLogoLoc = loc;
                    discordLogoTex = dt;
                }

                addRenderableWidget(new BrightnessImageButton(x, y, 20, 20,
                        loc,
                        b -> Util.getPlatform().openUri(link),
                        Component.literal(label)));
            } else {
                String emoji = label.equals("Wiki") ? "📚" : "💬";
                addRenderableWidget(Button.builder(Component.literal(emoji + " " + label),
                                b -> Util.getPlatform().openUri(link))
                        .bounds(x, y, 80, 20).build());
            }
        }, minecraft);
    }

    private void loadLogoAsync() {
        CthuLibConfig cfg = CthuLibConfig.get();
        if (cfg.logoPath.isEmpty()) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage img = null;

                try {
                    ResourceLocation rl = ResourceLocation.parse(cfg.logoPath);
                    try (InputStream is = minecraft.getResourceManager().getResource(rl).orElseThrow().open()) {
                        img = ImageIO.read(is);
                    }
                } catch (Exception e) {
                    File f = new File(minecraft.gameDirectory, cfg.logoPath);
                    if (f.exists()) {
                        img = ImageIO.read(f);
                    }
                }

                return img;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(img -> {
            if (img == null) return;

            int maxSize = (cfg.showName && !  cfg.name.isEmpty())
                    ?  LOGO_MAX_WITH_NAME
                    :    LOGO_MAX_WITHOUT_NAME;

            float scale = Math.min(1f, (float) maxSize / img.getHeight());
            logoW = (int) (img.getWidth() * scale);
            logoH = (int) (img.getHeight() * scale);

            NativeImage ni =
                    new NativeImage(img.getWidth(), img.getHeight(), true);

            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    ni.setPixelRGBA(x, y,
                            ((argb & 0xFF000000)) |
                                    ((argb & 0xFF) << 16) |
                                    ((argb & 0xFF00)) |
                                    ((argb & 0xFF0000) >> 16));
                }
            }

            logoTex = new DynamicTexture(ni);
            logoLoc = minecraft.getTextureManager().register("cthulib_logo", logoTex);
        }, minecraft);
    }

    private void loadBackgroundAsync() {
        CthuLibConfig cfg = CthuLibConfig.get();
        if (cfg.backgroundPath.isEmpty()) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage img = null;

                try {
                    ResourceLocation rl = ResourceLocation.parse(cfg.backgroundPath);
                    try (InputStream is = minecraft.getResourceManager().getResource(rl).orElseThrow().open()) {
                        img = ImageIO.read(is);
                    }
                } catch (Exception e) {
                    File f = new File(minecraft.gameDirectory, cfg.backgroundPath);
                    if (f.exists()) {
                        img = ImageIO.read(f);
                    }
                }

                if (img == null) return null;

                int originalW = img.getWidth();
                int originalH = img.getHeight();

                if (originalW > MAX_TEXTURE_SIZE || originalH > MAX_TEXTURE_SIZE) {
                    float scale = Math.min((float) MAX_TEXTURE_SIZE / originalW, (float) MAX_TEXTURE_SIZE / originalH);
                    int newW = (int) (originalW * scale);
                    int newH = (int) (originalH * scale);

                    BufferedImage scaledImg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = scaledImg.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(img, 0, 0, newW, newH, null);
                    g2d.dispose();
                    img = scaledImg;
                }

                return img;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(img -> {
            if (img == null) return;

            NativeImage ni =
                    new NativeImage(img.getWidth(), img.getHeight(), true);

            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    ni.setPixelRGBA(x, y,
                            ((argb & 0xFF000000)) |
                                    ((argb & 0xFF) << 16) |
                                    ((argb & 0xFF00)) |
                                    ((argb & 0xFF0000) >> 16));
                }
            }

            backgroundTex = new DynamicTexture(ni);
            backgroundLoc = minecraft.getTextureManager().register("cthulib_background", backgroundTex);
        }, minecraft);
    }

    public void showModDetail(ModData mod) {
        detailPanel.setMod(mod);
    }

    public void closeModDetail() {
        detailPanel.setMod(null);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (backgroundLoc != null && backgroundTex != null && backgroundTex.getPixels() != null) {
            RenderSystem.setShaderTexture(0, backgroundLoc);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int texWidth = backgroundTex.getPixels().getWidth();
            int texHeight = backgroundTex.getPixels().getHeight();

            float scaleX = (float) width / texWidth;
            float scaleY = (float) height / texHeight;
            float scale = Math.max(scaleX, scaleY);

            int scaledWidth = (int) (texWidth * scale);
            int scaledHeight = (int) (texHeight * scale);
            int offsetX = (width - scaledWidth) / 2;
            int offsetY = (height - scaledHeight) / 2;

            gfx.pose().pushPose();
            gfx.pose().translate(offsetX, offsetY, 0);
            gfx.pose().scale(scale, scale, 1.0f);

            gfx.blit(backgroundLoc, 0, 0, 0, 0, texWidth, texHeight, texWidth, texHeight);

            gfx.pose().popPose();

            RenderSystem.disableBlend();
        } else {
            super.renderBackground(gfx, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        super.render(gfx, mx, my, delta);

        if (!   detailPanel.isVisible()) {
            modList.render(gfx, mx, my, delta);
        } else {
            modList.render(gfx, -1, -1, delta);
        }

        CthuLibConfig cfg = CthuLibConfig.get();
        boolean hasHeader = (! cfg.logoPath.isEmpty() || (cfg.showName && !cfg.name.isEmpty()));

        if (hasHeader) {
            int cx = width / 2;
            int cy = 12;

            if (! cfg.logoPath.isEmpty() && logoTex != null && logoLoc != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                gfx.blit(logoLoc, cx - logoW / 2, cy, 0, 0, logoW, logoH, logoW, logoH);
                cy += logoH + 4;
            }

            if (cfg.showName && !  cfg.name.isEmpty()) {
                gfx.drawCenteredString(font,
                        Component.literal(cfg.name).withStyle(Style.EMPTY.withBold(true)),
                        cx, cy, 0xFFFFFF);
            }
        } else {
            gfx.drawCenteredString(font,
                    Component.literal("Installed Mods").withStyle(Style.EMPTY.withBold(true)),
                    width / 2, 16, 0xFFFFFF);

            int lw = 70;
            gfx.fill(width / 2 - lw / 2, 27, width / 2 + lw / 2, 28, 0xFF00FFFF);
        }

        if (! detailPanel.isVisible()) {
            renderCloseButton(gfx, mx, my);
        }

        if (detailPanel.isVisible()) {
            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 1000);
            detailPanel.render(gfx, mx, my, delta);
            gfx.pose().popPose();
        }
    }

    private void renderCloseButton(GuiGraphics gfx, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= closeButtonX && mouseX <= closeButtonX + CLOSE_BUTTON_SIZE &&
                mouseY >= closeButtonY && mouseY <= closeButtonY + CLOSE_BUTTON_SIZE;

        int bgColor = isHovered ? 0xFFFF5555 : 0xFF994444;
        gfx.fill(closeButtonX, closeButtonY, closeButtonX + CLOSE_BUTTON_SIZE, closeButtonY + CLOSE_BUTTON_SIZE, bgColor);

        int borderColor = isHovered ? 0xFFFF8888 : 0xFFAA5555;
        gfx.renderOutline(closeButtonX, closeButtonY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, borderColor);

        if (isHovered) {
            gfx.renderOutline(closeButtonX + 1, closeButtonY + 1, CLOSE_BUTTON_SIZE - 2, CLOSE_BUTTON_SIZE - 2, 0x4400FFFF);
        }

        gfx.drawString(font, "X", closeButtonX + 7, closeButtonY + 6, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (! detailPanel.isVisible() && button == 0) {
            if (mouseX >= closeButtonX && mouseX <= closeButtonX + CLOSE_BUTTON_SIZE &&
                    mouseY >= closeButtonY && mouseY <= closeButtonY + CLOSE_BUTTON_SIZE) {
                this.onClose();
                return true;
            }
        }

        if (detailPanel.isVisible()) {
            return detailPanel.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (detailPanel.isVisible()) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (detailPanel.isVisible()) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        if (logoTex != null && logoLoc != null) {
            minecraft.getTextureManager().release(logoLoc);
            logoTex.close();
        }
        if (wikiLogoLoc != null && wikiLogoTex != null) {
            minecraft.getTextureManager().release(wikiLogoLoc);
            wikiLogoTex.close();
        }
        if (discordLogoLoc != null && discordLogoTex != null) {
            minecraft.getTextureManager().release(discordLogoLoc);
            discordLogoTex.close();
        }
        if (backgroundTex != null && backgroundLoc != null) {
            minecraft.getTextureManager().release(backgroundLoc);
            backgroundTex.close();
        }
        minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}