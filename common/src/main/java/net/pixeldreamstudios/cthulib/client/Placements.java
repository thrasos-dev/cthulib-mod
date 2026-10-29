package net.pixeldreamstudios.cthulib.client;

import net.pixeldreamstudios.cthulib.client.Placement.Access;
import net.pixeldreamstudios.cthulib.client.Placement.BoolField;
import net.pixeldreamstudios.cthulib.client.Placement.FloatField;
import net.pixeldreamstudios.cthulib.client.Placement.IntField;
import net.pixeldreamstudios.cthulib.client.Placement.TextField;

public final class Placements {

    private static final IntField MODLIST_WIDTH = new IntField(c -> c.modlistButtonWidth, (c, v) -> c.modlistButtonWidth = v);
    private static final IntField MODLIST_HEIGHT = new IntField(c -> c.modlistButtonHeight, (c, v) -> c.modlistButtonHeight = v);
    private static final FloatField MODLIST_SCALE = new FloatField(c -> c.modlistButtonScale, (c, v) -> c.modlistButtonScale = v);
    private static final BoolField MODLIST_SCALE_WITH_SCREEN = new BoolField(c -> c.modlistButtonScaleWithScreen, (c, v) -> c.modlistButtonScaleWithScreen = v);
    private static final FloatField MODLIST_MIN_SCALE = new FloatField(c -> c.modlistButtonMinScale, (c, v) -> c.modlistButtonMinScale = v);
    private static final FloatField MODLIST_MAX_SCALE = new FloatField(c -> c.modlistButtonMaxScale, (c, v) -> c.modlistButtonMaxScale = v);

    public static final Access MODLIST_TITLE = new Access(
            new IntField(c -> c.buttonXtitleScreen, (c, v) -> c.buttonXtitleScreen = v),
            new IntField(c -> c.buttonYtitleScreen, (c, v) -> c.buttonYtitleScreen = v),
            MODLIST_WIDTH, MODLIST_HEIGHT, MODLIST_SCALE, MODLIST_SCALE_WITH_SCREEN, MODLIST_MIN_SCALE, MODLIST_MAX_SCALE,
            new BoolField(c -> c.modlistButtonUseAnchor, (c, v) -> c.modlistButtonUseAnchor = v),
            new BoolField(c -> c.buttonTitleScreenStartFromCenterX, (c, v) -> c.buttonTitleScreenStartFromCenterX = v),
            new BoolField(c -> c.buttonTitleScreenStartFromCenterY, (c, v) -> c.buttonTitleScreenStartFromCenterY = v),
            new BoolField(c -> c.buttonTitleScreenStartFromLeftX, (c, v) -> c.buttonTitleScreenStartFromLeftX = v),
            new BoolField(c -> c.buttonTitleScreenStartFromRightX, (c, v) -> c.buttonTitleScreenStartFromRightX = v),
            new TextField(c -> c.buttonTitleScreenAnchor, (c, v) -> c.buttonTitleScreenAnchor = v));

    public static final Access MODLIST_SETTINGS = new Access(
            new IntField(c -> c.buttonXsettings, (c, v) -> c.buttonXsettings = v),
            new IntField(c -> c.buttonYsettings, (c, v) -> c.buttonYsettings = v),
            MODLIST_WIDTH, MODLIST_HEIGHT, MODLIST_SCALE, MODLIST_SCALE_WITH_SCREEN, MODLIST_MIN_SCALE, MODLIST_MAX_SCALE,
            new BoolField(c -> c.buttonSettingsUseAnchor, (c, v) -> c.buttonSettingsUseAnchor = v),
            new BoolField(c -> c.buttonSettingsStartFromCenterX, (c, v) -> c.buttonSettingsStartFromCenterX = v),
            new BoolField(c -> c.buttonSettingsStartFromCenterY, (c, v) -> c.buttonSettingsStartFromCenterY = v),
            new BoolField(c -> c.buttonSettingsStartFromLeftX, (c, v) -> c.buttonSettingsStartFromLeftX = v),
            new BoolField(c -> c.buttonSettingsStartFromRightX, (c, v) -> c.buttonSettingsStartFromRightX = v),
            new TextField(c -> c.buttonSettingsAnchor, (c, v) -> c.buttonSettingsAnchor = v));

    public static final Access PROMO = new Access(
            new IntField(c -> c.promoButtonX, (c, v) -> c.promoButtonX = v),
            new IntField(c -> c.promoButtonY, (c, v) -> c.promoButtonY = v),
            new IntField(c -> c.promoButtonWidth, (c, v) -> c.promoButtonWidth = v),
            new IntField(c -> c.promoButtonHeight, (c, v) -> c.promoButtonHeight = v),
            new FloatField(c -> c.promoButtonScale, (c, v) -> c.promoButtonScale = v),
            new BoolField(c -> c.promoButtonScaleWithScreen, (c, v) -> c.promoButtonScaleWithScreen = v),
            new FloatField(c -> c.promoButtonMinScale, (c, v) -> c.promoButtonMinScale = v),
            new FloatField(c -> c.promoButtonMaxScale, (c, v) -> c.promoButtonMaxScale = v),
            new BoolField(c -> c.promoButtonUseAnchor, (c, v) -> c.promoButtonUseAnchor = v),
            new BoolField(c -> c.promoButtonStartFromCenterX, (c, v) -> c.promoButtonStartFromCenterX = v),
            new BoolField(c -> c.promoButtonStartFromCenterY, (c, v) -> c.promoButtonStartFromCenterY = v),
            new BoolField(c -> c.promoButtonStartFromLeftX, (c, v) -> c.promoButtonStartFromLeftX = v),
            new BoolField(c -> c.promoButtonStartFromRightX, (c, v) -> c.promoButtonStartFromRightX = v),
            new TextField(c -> c.promoButtonAnchor, (c, v) -> c.promoButtonAnchor = v));

    public static final Access CHANGELOG = new Access(
            new IntField(c -> c.changelogButtonX, (c, v) -> c.changelogButtonX = v),
            new IntField(c -> c.changelogButtonY, (c, v) -> c.changelogButtonY = v),
            new IntField(c -> c.changelogButtonWidth, (c, v) -> c.changelogButtonWidth = v),
            new IntField(c -> c.changelogButtonHeight, (c, v) -> c.changelogButtonHeight = v),
            new FloatField(c -> c.changelogButtonScale, (c, v) -> c.changelogButtonScale = v),
            new BoolField(c -> c.changelogButtonScaleWithScreen, (c, v) -> c.changelogButtonScaleWithScreen = v),
            new FloatField(c -> c.changelogButtonMinScale, (c, v) -> c.changelogButtonMinScale = v),
            new FloatField(c -> c.changelogButtonMaxScale, (c, v) -> c.changelogButtonMaxScale = v),
            new BoolField(c -> c.changelogButtonUseAnchor, (c, v) -> c.changelogButtonUseAnchor = v),
            new BoolField(c -> c.changelogButtonStartFromCenterX, (c, v) -> c.changelogButtonStartFromCenterX = v),
            new BoolField(c -> c.changelogButtonStartFromCenterY, (c, v) -> c.changelogButtonStartFromCenterY = v),
            new BoolField(c -> c.changelogButtonStartFromLeftX, (c, v) -> c.changelogButtonStartFromLeftX = v),
            new BoolField(c -> c.changelogButtonStartFromRightX, (c, v) -> c.changelogButtonStartFromRightX = v),
            new TextField(c -> c.changelogButtonAnchor, (c, v) -> c.changelogButtonAnchor = v));

    public static final Access SLIDER = new Access(
            new IntField(c -> c.sliderX, (c, v) -> c.sliderX = v),
            new IntField(c -> c.sliderY, (c, v) -> c.sliderY = v),
            new IntField(c -> c.sliderCardWidth, (c, v) -> c.sliderCardWidth = v),
            new IntField(c -> c.sliderCardHeight, (c, v) -> c.sliderCardHeight = v),
            new FloatField(c -> c.sliderScale, (c, v) -> c.sliderScale = v),
            new BoolField(c -> c.sliderScaleWithScreen, (c, v) -> c.sliderScaleWithScreen = v),
            new FloatField(c -> c.sliderMinScale, (c, v) -> c.sliderMinScale = v),
            new FloatField(c -> c.sliderMaxScale, (c, v) -> c.sliderMaxScale = v),
            new BoolField(c -> c.sliderUseAnchor, (c, v) -> c.sliderUseAnchor = v),
            new BoolField(c -> c.sliderStartFromCenterX, (c, v) -> c.sliderStartFromCenterX = v),
            new BoolField(c -> c.sliderStartFromCenterY, (c, v) -> c.sliderStartFromCenterY = v),
            new BoolField(c -> c.sliderStartFromLeftX, (c, v) -> c.sliderStartFromLeftX = v),
            new BoolField(c -> c.sliderStartFromRightX, (c, v) -> c.sliderStartFromRightX = v),
            new TextField(c -> c.sliderAnchor, (c, v) -> c.sliderAnchor = v));

    public static final Access WIKI = new Access(
            new IntField(c -> c.wikiButtonX, (c, v) -> c.wikiButtonX = v),
            new IntField(c -> c.wikiButtonY, (c, v) -> c.wikiButtonY = v),
            new IntField(c -> c.wikiButtonWidth, (c, v) -> c.wikiButtonWidth = v),
            new IntField(c -> c.wikiButtonHeight, (c, v) -> c.wikiButtonHeight = v),
            new FloatField(c -> c.wikiButtonScale, (c, v) -> c.wikiButtonScale = v),
            new BoolField(c -> c.wikiButtonScaleWithScreen, (c, v) -> c.wikiButtonScaleWithScreen = v),
            new FloatField(c -> c.wikiButtonMinScale, (c, v) -> c.wikiButtonMinScale = v),
            new FloatField(c -> c.wikiButtonMaxScale, (c, v) -> c.wikiButtonMaxScale = v),
            new BoolField(c -> c.wikiButtonUseAnchor, (c, v) -> c.wikiButtonUseAnchor = v),
            new BoolField(c -> c.wikiButtonStartFromCenterX, (c, v) -> c.wikiButtonStartFromCenterX = v),
            new BoolField(c -> c.wikiButtonStartFromCenterY, (c, v) -> c.wikiButtonStartFromCenterY = v),
            new BoolField(c -> c.wikiButtonStartFromLeftX, (c, v) -> c.wikiButtonStartFromLeftX = v),
            new BoolField(c -> c.wikiButtonStartFromRightX, (c, v) -> c.wikiButtonStartFromRightX = v),
            new TextField(c -> c.wikiButtonAnchor, (c, v) -> c.wikiButtonAnchor = v));

    public static final Access DISCORD = new Access(
            new IntField(c -> c.discordButtonX, (c, v) -> c.discordButtonX = v),
            new IntField(c -> c.discordButtonY, (c, v) -> c.discordButtonY = v),
            new IntField(c -> c.discordButtonWidth, (c, v) -> c.discordButtonWidth = v),
            new IntField(c -> c.discordButtonHeight, (c, v) -> c.discordButtonHeight = v),
            new FloatField(c -> c.discordButtonScale, (c, v) -> c.discordButtonScale = v),
            new BoolField(c -> c.discordButtonScaleWithScreen, (c, v) -> c.discordButtonScaleWithScreen = v),
            new FloatField(c -> c.discordButtonMinScale, (c, v) -> c.discordButtonMinScale = v),
            new FloatField(c -> c.discordButtonMaxScale, (c, v) -> c.discordButtonMaxScale = v),
            new BoolField(c -> c.discordButtonUseAnchor, (c, v) -> c.discordButtonUseAnchor = v),
            new BoolField(c -> c.discordButtonStartFromCenterX, (c, v) -> c.discordButtonStartFromCenterX = v),
            new BoolField(c -> c.discordButtonStartFromCenterY, (c, v) -> c.discordButtonStartFromCenterY = v),
            new BoolField(c -> c.discordButtonStartFromLeftX, (c, v) -> c.discordButtonStartFromLeftX = v),
            new BoolField(c -> c.discordButtonStartFromRightX, (c, v) -> c.discordButtonStartFromRightX = v),
            new TextField(c -> c.discordButtonAnchor, (c, v) -> c.discordButtonAnchor = v));

    private Placements() {
    }
}
