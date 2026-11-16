package net.pixeldreamstudios.cthulib.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;
import net.pixeldreamstudios.cthulib.util.CthuLibLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class ModsScreen extends Screen {
    private final Screen parentScreen;
    private ModListWidget modList;
    private ResourceLocation logoLoc;
    private ResourceLocation wikiLogoLoc;
    private ResourceLocation discordLogoLoc;
    private int backgroundWidth;
    private int backgroundHeight;
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
    private static final String BUTTON_LOGO_KEY = "mods_screen_button_";
    private static final String LOGO_KEY = "mods_screen_logo_";
    private static final String BACKGROUND_KEY = "mods_screen_background_";
    private static List<ModData> cachedMods = null;
    private static ModListWidget cachedModList = null;
    private static int lastScreenWidth = -1;
    private static int lastScreenHeight = -1;
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

        if (!ModCollector.isDataReady()) {
            ModCollector.preloadModData();
        }
        boolean isFirstInit = (lastScreenWidth == -1 && lastScreenHeight == -1);
        
        lastScreenWidth = width;
        lastScreenHeight = height;

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
            boolean wikiPlaced = !Placements.WIKI.spec(cfg).legacy();
            int wikiX = defaultWikiX + cfg.wikiButtonX;
            int wikiY = defaultWikiY + cfg.wikiButtonY;

            if (! cfg.wikiLogo.isEmpty()) {
                loadButtonLogoAsync(cfg.wikiLogo, wikiX, wikiY, cfg.wikiLink, "Wiki", wikiPlaced ? UIElementPositionManager.ElementType.WIKI_BUTTON : null);
            } else {
                addRenderableWidget(Button.builder(Component.literal("📚 Wiki"),
                                b -> Util.getPlatform().openUri(cfg.wikiLink))
                        .bounds(wikiX, wikiY, 80, 20).build());
            }
        }

        if (! cfg.discordLink.isEmpty()) {
            int defaultDiscordX = width - 26;
            int defaultDiscordY = hasHeader ? 50 : 6;
            boolean discordPlaced = !Placements.DISCORD.spec(cfg).legacy();
            int discordX = defaultDiscordX + cfg.discordButtonX;
            int discordY = defaultDiscordY + cfg.discordButtonY;

            if (!    cfg.discordLogo.isEmpty()) {
                loadButtonLogoAsync(cfg.discordLogo, discordX, discordY, cfg.discordLink, "Discord", discordPlaced ? UIElementPositionManager.ElementType.DISCORD_BUTTON : null);
            } else {
                addRenderableWidget(Button.builder(Component.literal("💬 Discord"),
                                b -> Util.getPlatform().openUri(cfg.discordLink))
                        .bounds(discordX, discordY, 80, 20).build());
            }
        }

        detailPanel = new ModDetailPanel(this);
    }

    private void loadButtonLogoAsync(String path, int x, int y, String link, String label, UIElementPositionManager.ElementType placed) {
        String key = BUTTON_LOGO_KEY + path;
        Util.ioPool().execute(() -> {
            Optional<byte[]> bytes = readImageBytes(path, false);
            if (bytes.isEmpty()) {
                minecraft.execute(() -> addTextLinkButton(x, y, link, label));
                return;
            }
            RawImageTextures.load(key, RawImageTextures.locationFor(key), bytes.get(), loaded -> {
                if (loaded.isPresent()) {
                    addLogoLinkButton(loaded.get().location(), x, y, link, label, placed);
                } else {
                    addTextLinkButton(x, y, link, label);
                }
            });
        });
    }

    private void addLogoLinkButton(ResourceLocation loc, int x, int y, String link, String label, UIElementPositionManager.ElementType placed) {
        if (label.equals("Wiki")) {
            wikiLogoLoc = loc;
        } else if (label.equals("Discord")) {
            discordLogoLoc = loc;
        }

        UIElementPositionManager.ElementType kind = label.equals("Wiki") ? UIElementPositionManager.ElementType.WIKI_BUTTON
                : UIElementPositionManager.ElementType.DISCORD_BUTTON;
        Placement.Spec size = kind.access().spec(CthuLibConfig.getInstance());
        ModListButton linkButton = new ModListButton(x, y, size.width(), size.height(),
                loc,
                b -> Util.getPlatform().openUri(link),
                Component.literal(label),
                placed);
        linkButton.fancyMenuId(kind.fancyMenuId());
        addRenderableWidget(linkButton);
    }

    private void addTextLinkButton(int x, int y, String link, String label) {
        String emoji = label.equals("Wiki") ? "📚" : "💬";
        addRenderableWidget(Button.builder(Component.literal(emoji + " " + label),
                        b -> Util.getPlatform().openUri(link))
                .bounds(x, y, 80, 20).build());
    }

    private void loadLogoAsync() {
        CthuLibConfig cfg = CthuLibConfig.get();
        if (cfg.logoPath.isEmpty()) return;

        String key = LOGO_KEY + cfg.logoPath;
        int maxSize = (cfg.showName && !cfg.name.isEmpty()) ? LOGO_MAX_WITH_NAME : LOGO_MAX_WITHOUT_NAME;
        Util.ioPool().execute(() -> readImageBytes(cfg.logoPath, true).ifPresent(bytes ->
                RawImageTextures.load(key, RawImageTextures.locationFor(key), bytes, loaded -> loaded.ifPresent(result -> {
                    float scale = Math.min(1f, (float) maxSize / result.height());
                    logoW = (int) (result.width() * scale);
                    logoH = (int) (result.height() * scale);
                    logoLoc = result.location();
                }))));
    }

    private void loadBackgroundAsync() {
        CthuLibConfig cfg = CthuLibConfig.get();
        if (cfg.backgroundPath.isEmpty()) return;

        String key = BACKGROUND_KEY + cfg.backgroundPath;
        Util.ioPool().execute(() -> readImageBytes(cfg.backgroundPath, true).ifPresent(bytes ->
                RawImageTextures.load(key, RawImageTextures.locationFor(key), bytes, MAX_TEXTURE_SIZE, loaded -> loaded.ifPresent(result -> {
                    backgroundWidth = result.width();
                    backgroundHeight = result.height();
                    backgroundLoc = result.location();
                }))));
    }

    private Optional<byte[]> readImageBytes(String path, boolean allowGameFile) {
        ResourceLocation location = ResourceLocation.tryParse(path);
        if (location != null) {
            try (InputStream in = minecraft.getResourceManager().open(location)) {
                return Optional.of(in.readAllBytes());
            } catch (IOException e) {
                if (!allowGameFile) {
                    CthuLibLogger.error("Could not read image " + path + ": " + e.getMessage());
                }
            }
        }
        if (allowGameFile) {
            File file = new File(minecraft.gameDirectory, path);
            if (file.exists()) {
                try {
                    return Optional.of(Files.readAllBytes(file.toPath()));
                } catch (IOException e) {
                    CthuLibLogger.error("Could not read image " + path + ": " + e.getMessage());
                }
            }
        }
        return Optional.empty();
    }

    public void showModDetail(ModData mod) {
        detailPanel.setMod(mod);
    }

    public void closeModDetail() {
        detailPanel.setMod(null);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        ResourceLocation background = RawImageTextures.resolve(backgroundLoc);
        if (background != null) {
            RenderSystem.setShaderTexture(0, background);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int texWidth = backgroundWidth;
            int texHeight = backgroundHeight;

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

            gfx.blit(background, 0, 0, 0, 0, texWidth, texHeight, texWidth, texHeight);

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

            ResourceLocation logo = RawImageTextures.resolve(logoLoc);
            if (! cfg.logoPath.isEmpty() && logo != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                gfx.blit(logo, cx - logoW / 2, cy, 0, 0, logoW, logoH, logoW, logoH);
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
        RawImageTextures.release(logoLoc);
        RawImageTextures.release(wikiLogoLoc);
        RawImageTextures.release(discordLogoLoc);
        RawImageTextures.release(backgroundLoc);
        minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}