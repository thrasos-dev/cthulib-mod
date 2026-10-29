## cthulib.json

Main configuration file for CthuLib's mod screen and UI customization.

### Display Settings

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `showInTitleScreen` | boolean | `true` | Shows the button on the title screen |
| `showInSettings` | boolean | `true` | Shows the button in the options/settings screen |
| `showName` | boolean | `true` | Displays the name text in the mod screen header |
| `name` | string | `""` | Custom name to display in the mod screen header |

### Author Filter

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `authors` | string[] | `["KevzCz", "lwkysad", "steficy", "Bandit-bytes", "VeroXUniverse", "Rebel459", "Thrasos", "noodlescript", "alshanex", "starrysock", "aleganza"]` | List of mod authors to display in the mod screen |

### Button Configuration

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `buttonLogo` | string | `"cthulib:textures/gui/button_normal.png"` | Resource location of the button texture |
| `buttonTooltip` | string | `"Pixel Dream Studios"` | Tooltip text shown when hovering over the button |
| `buttonXtitleScreen` | integer | `0` | X offset for button position on title screen |
| `buttonYtitleScreen` | integer | `0` | Y offset for button position on title screen |
| `buttonXsettings` | integer | `0` | X offset for button position in settings screen |
| `buttonYsettings` | integer | `0` | Y offset for button position in settings screen |

### Mod Screen Customization

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `logoPath` | string | `"cthulib:textures/gui/pdslogo.png"` | Resource location or file path for the header logo |
| `backgroundPath` | string | `"cthulib:textures/gui/pdsbg.png"` | Resource location or file path for the background image |

### Social Links

#### Wiki Link

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `wikiLink` | string | `"https://pixeldreamstudios.net"` | URL for the wiki button |
| `wikiLogo` | string | `"cthulib:textures/gui/button_normal.png"` | Resource location for the wiki button texture |
| `wikiButtonX` | integer | `125` | X offset for wiki button position |
| `wikiButtonY` | integer | `0` | Y offset for wiki button position |

#### Discord Link

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `discordLink` | string | `"https://discord.pixeldreamstudios.net/"` | URL for the discord button |
| `discordLogo` | string | `"cthulib:textures/gui/button_christmas.png"` | Resource location for the discord button texture |
| `discordButtonX` | integer | `-125` | X offset for discord button position |
| `discordButtonY` | integer | `0` | Y offset for discord button position |

### Placement (every button)

Every placeable element has the same set of options. Replace `<prefix>` with the element's prefix:

| Element | Prefix | Position fields |
|---------|--------|-----------------|
| Mod list button (title screen) | `buttonTitleScreen` | `buttonXtitleScreen`, `buttonYtitleScreen` |
| Mod list button (settings) | `buttonSettings` | `buttonXsettings`, `buttonYsettings` |
| Promo button | `promoButton` | `promoButtonX`, `promoButtonY` |
| Changelog button | `changelogButton` | `changelogButtonX`, `changelogButtonY` |
| Project slider | `slider` | `sliderX`, `sliderY` |
| Wiki button (mods screen) | `wikiButton` | `wikiButtonX`, `wikiButtonY` |
| Discord button (mods screen) | `discordButton` | `discordButtonX`, `discordButtonY` |

| Option | Type | Description |
|--------|------|-------------|
| `<prefix>StartFromCenterX` / `CenterY` / `LeftX` / `RightX` | boolean | Measure the offset from the screen center or edge |
| `<prefix>UseAnchor` | boolean | Classic anchor: centered horizontally, below the title screen logo |
| `<prefix>Anchor` | string | FancyMenu-style anchor, see below. Overrides the flags above when set |

Both mod list buttons share `modlistButtonWidth`, `modlistButtonHeight`, `modlistButtonScale`, `modlistButtonScaleWithScreen`, `modlistButtonMinScale`, `modlistButtonMaxScale`. The other elements have their own `Width`, `Height`, `Scale`, `ScaleWithScreen`, `MinScale` and `MaxScale` (the slider uses `sliderCardWidth`, `sliderCardHeight` and `sliderScale`).

#### Anchors

* `top-left`, `top-centered`, `top-right`, `mid-left`, `mid-centered`, `mid-right`, `bottom-left`, `bottom-centered`, `bottom-right`: the position is the anchor point plus the X/Y offset, the same way FancyMenu places elements, so offsets can be copied from a FancyMenu layout.
* `element:<target>`: anchors to another button on the same screen. `<target>` is a button's translation key (`menu.singleplayer`, `menu.options`), its visible text, or another CthuLib element id (`cthulib_promo_button`). Add `@<anchor>` to pick the corner of the target, for example `element:menu.singleplayer@top-right`. The element follows the target when FancyMenu moves it.
* Leave empty (or `none`) for the old behaviour.

#### FancyMenu element ids

CthuLib buttons report fixed ids to FancyMenu, so moving them no longer changes their FancyMenu element id: `cthulib_brightness_button_title`, `cthulib_brightness_button_settings`, `cthulib_promo_button`, `cthulib_changelog_button`, `cthulib_project_slider`, `cthulib_wiki_button`, `cthulib_discord_button`.

### Example Configuration

```json
{
  "showInTitleScreen": true,
  "showInSettings":  true,
  "authors": [
    "KevzCz",
    "YourModAuthor"
  ],
  "buttonLogo": "cthulib:textures/gui/button_normal.png",
  "buttonTooltip": "My Mods",
  "buttonXtitleScreen": 0,
  "buttonYtitleScreen": 0,
  "buttonXsettings": 0,
  "buttonYsettings": 0,
  "showName":  true,
  "name": "My Mod Pack",
  "logoPath": "cthulib:textures/gui/logo.png",
  "backgroundPath": "cthulib:textures/gui/background.png",
  "wikiLink": "https://example.com/wiki",
  "wikiLogo": "cthulib:textures/gui/wiki_button.png",
  "wikiButtonX": 100,
  "wikiButtonY":  0,
  "discordLink":  "https://discord.gg/example",
  "discordLogo": "cthulib:textures/gui/discord_button.png",
  "discordButtonX": -100,
  "discordButtonY": 0
}
```

---

## promo_messages.json

Configuration for server-specific promotional messages that appear when joining servers.

### ServerPromo Object

| Field | Type | Description |
|-------|------|-------------|
| `servers` | string[] | List of server addresses (partial matches work, e.g., `"playcdu.co"` matches `"play.playcdu.co"`) |
| `messageParts` | MessagePart[] | Array of message parts that compose the complete message |

### MessagePart Object

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `text` | string | `""` | The text content to display |
| `color` | string | `""` | Minecraft color name (see color list below) |
| `bold` | boolean | `false` | Makes the text bold |
| `italic` | boolean | `false` | Makes the text italic |
| `underlined` | boolean | `false` | Underlines the text |
| `clickAction` | string | `""` | Type of click action (see action list below) |
| `clickValue` | string | `""` | Value/URL for the click action |

### Available Colors

- `black`
- `dark_blue`
- `dark_green`
- `dark_aqua`
- `dark_red`
- `dark_purple`
- `gold`
- `gray`
- `dark_gray`
- `blue`
- `green`
- `aqua`
- `red`
- `light_purple`
- `yellow`
- `white`

### Available Click Actions

| Action | Description | Example Value |
|--------|-------------|---------------|
| `open_url` | Opens a URL in the browser | `"https://example.com"` |
| `run_command` | Executes a command | `"/spawn"` |
| `suggest_command` | Suggests a command in chat | `"/tell @a "` |

### Example Configuration

```json
{
  "serverPromos": [
    {
      "servers": [
        "playcdu.co"
      ],
      "messageParts": [
        {
          "text": "Use code ",
          "color": "",
          "bold": false,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        },
        {
          "text": "PixelDream",
          "color": "light_purple",
          "bold": false,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        },
        {
          "text":  " (case-sensitive) for 30% off on the ",
          "color": "",
          "bold": false,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        },
        {
          "text": "CDU Store",
          "color": "white",
          "bold": false,
          "italic": false,
          "underlined": true,
          "clickAction": "open_url",
          "clickValue": "https://store.playcdu.co"
        },
        {
          "text":  "! ",
          "color": "",
          "bold": false,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        }
      ]
    },
    {
      "servers":  [
        "hypixel.net",
        "mc.hypixel.net"
      ],
      "messageParts": [
        {
          "text": "Welcome to ",
          "color": "yellow",
          "bold": false,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        },
        {
          "text": "Hypixel",
          "color": "gold",
          "bold": true,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        },
        {
          "text": "! Type ",
          "color": "yellow",
          "bold": false,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        },
        {
          "text": "/lobby",
          "color": "aqua",
          "bold": false,
          "italic": true,
          "underlined": false,
          "clickAction": "suggest_command",
          "clickValue": "/lobby"
        }
      ]
    }
  ]
}
```

### Creating Simple Messages

For a basic message without formatting:

```json
{
  "serverPromos": [
    {
      "servers": ["example.com"],
      "messageParts": [
        {
          "text": "Welcome to our server!",
          "color":  "green",
          "bold": false,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        }
      ]
    }
  ]
}
```

### Creating Multi-Server Messages

Target multiple servers with the same message:

```json
{
  "serverPromos": [
    {
      "servers":  [
        "lobby.example.com",
        "play.example.com",
        "hub.example.com"
      ],
      "messageParts": [
        {
          "text":  "Welcome to Example Network!",
          "color": "aqua",
          "bold": true,
          "italic": false,
          "underlined": false,
          "clickAction": "",
          "clickValue": ""
        }
      ]
    }
  ]
}
```
