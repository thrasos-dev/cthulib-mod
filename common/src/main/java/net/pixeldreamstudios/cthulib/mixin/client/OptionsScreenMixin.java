package net.pixeldreamstudios.cthulib.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.cthulib.client.BrightnessImageButton;
import net.pixeldreamstudios.cthulib.client.ModsScreen;
import net.pixeldreamstudios.cthulib.client.UIElementPositionManager;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        CthuLibConfig cfg = CthuLibConfig.get();

        if (! cfg.showInSettings) {
            return;
        }

        int x, y;
        if (cfg.buttonXsettings == 0 && cfg.buttonYsettings == 0) {
            x = this.width - 26;
            y = 6;
        } else {
            x = cfg.buttonXsettings;
            y = cfg.buttonYsettings;
        }

        ResourceLocation buttonLocation;
        try {
            String texturePath = (cfg.brightnessButtonTexture != null && !cfg.brightnessButtonTexture.isEmpty())
                ? cfg.brightnessButtonTexture : cfg.buttonLogo;
            buttonLocation = ResourceLocation.parse(texturePath);
        } catch (Exception e) {
            buttonLocation = ResourceLocation.fromNamespaceAndPath("cthulib", "textures/gui/button.png");
        }

        BrightnessImageButton button = new BrightnessImageButton(
                x, y, 20, 20,
                buttonLocation,
                b -> Minecraft.getInstance().setScreen(new ModsScreen(this)),
                Component.literal(cfg.buttonTooltip),
                UIElementPositionManager.ElementType.BRIGHTNESS_BUTTON_SETTINGS
        );

        this.addRenderableWidget(button);
    }
}