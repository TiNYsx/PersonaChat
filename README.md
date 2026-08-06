# 💬 PersonaChat

[![Release](https://img.shields.io/badge/Release-v1.0-brightgreen.svg)](https://github.com/TiNYsx/PersonaChat)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-orange.svg)](https://papermc.io/)
[![Author](https://img.shields.io/badge/Author-TiNYsx-blue.svg)](https://www.spigotmc.org/members/jirwat.457182/)
[![License](https://img.shields.io/badge/License-Custom%20License-lightgrey.svg)](LICENSE)

**PersonaChat** is an advanced Persona 3D/2D Floating Chat and Profile Customization plugin for Paper/Spigot Minecraft servers. It renders stylized chat bubbles directly in the player's world with support for external resource pack fonts, custom models, 3-slice background stretching, hotbar history scrolling, and personalized cosmetic customization via interactive GUIs.

---

## 📜 License
This project is licensed under the **PersonaChat Custom License**.

### Summary:
- **You may** download, use, and share this plugin for free.
- **You may not** resell, redistribute, or modify and distribute this plugin.
- **You are allowed** to sell packs or products that depend on this plugin, provided the plugin itself is **not included** and users are directed to download it from the official GitHub repository.
- **Attribution is required** for using this plugin in your product or documentation (Credit to **TiNYsx** / **Nokhongyok**).

For full details, refer to the [LICENSE](LICENSE) file.

---

## ✨ Features

- **🎮 Persona-Style Floating Chat Displays**:
  - Displays messages in front of the player using native Minecraft `ItemDisplay` & `TextDisplay` entities with zero client-side mods needed.
  - Supports both **3D Block Heads** and **2D Flat Half-Body/Bust Avatars** (using billboard matrix transformation).
  - Mathematical layout and animation engine supporting real-time functions (`sin`, `cos`, `abs`, math curves, time offsets).

- **📦 Modular Pack Architecture (`plugins/PersonaChat/packs/*.yml`)**:
  - Seamlessly integrates with external resource pack managers (**ItemsAdder**, **Oraxen**, **Nexo**, or Default Minecraft Resource Packs).
  - **Part 1: Asset Registry**: Define custom font glyphs, unicode characters, and negative-space horizontal offsets.
  - **Part 2: Decorations**: Define custom Avatar Frames, Badges/Titles, and Dynamic Chat Backgrounds with permissions and custom model data.

- **📜 Dynamic 3-Slice Font-Image Backgrounds**:
  - Multi-line messages automatically calculate character pixel widths and expand using dynamic 3-slice backgrounds (`Top + (Middle * Lines) + Bottom`).

- **✨ Personalized Profile Cosmetics**:
  - **Avatar Frames**: Borders, halos, and custom 3D model accessories attached to the player's head.
  - **Titles & Badges**: Custom labels and font glyph prefixes before player names (e.g. `🗡 Dragon Killer |`).
  - **Message Bubbles**: Custom chat bracket designs, font wrappers, and translucent background opacities.
  - **Color Palettes**: Gradient and hex color presets for names and text.
  - **Chest GUI (`/pc menu`)**: Intuitive categorized menu to browse, preview, and equip unlocked cosmetics.

- **🔄 Standing-Still History Scrolling**:
  - Keep moving without clutter! In `STANDING_STILL` mode, chat smoothly appears when stationary.
  - Scroll through up to 50 previous messages using the hotbar scroll wheel when standing still.

- **⚡ PlaceholderAPI Support**: Full integration with PAPI placeholders across all badges, names, and messages.

---

## 📥 Installation

### Soft Dependencies
1. **[ItemsAdder](https://www.spigotmc.org/resources/%E2%9C%A8itemsadder%E2%AD%90emotes-mobs-items-armors-hud-gui-emojis-blocks-wings-hats-liquids.73355/)**, **[Oraxen](https://www.spigotmc.org/resources/%E2%98%84%EF%B8%8F-oraxen-custom-items-blocks-emotes-furniture-resourcepack-and-gui-1-18-1-21-4.72448/)**, or **[NexO](https://mcmodels.net/products/13172/nexo?srsltid=AfmBOooeUFusiEq8tfccU_E5sGRq8vCtXQFUn5MNkKESvfpLZThkB0O0)** (Optional for custom font glyphs and models)
2. **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)** (Optional)

### How to use
1. Place the `PersonaChat-1.0.jar` file in your Minecraft server's `plugins` folder.
2. Restart or reload your server.
3. Configure settings in `plugins/PersonaChat/config.yml` and custom packs in `plugins/PersonaChat/packs/`.

---

## 🕹️ Commands & Permissions

### Commands
| Command | Alias | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/personachat menu` | `/pc menu`, `/pc gui` | Opens the Profile Cosmetics GUI | `personachat.menu` |
| `/personachat toggle` | `/pc toggle` | Toggles floating chat on/off | `personachat.use` |
| `/personachat on` / `off` | `/pc on` / `/pc off` | Enables or disables floating chat | `personachat.use` |
| `/personachat equip <type> <id>` | `/pc equip` | Equips a cosmetic item | `personachat.use` |
| `/personachat unequip <type\|all>` | `/pc unequip` | Unequips a cosmetic item | `personachat.use` |
| `/personachat reload` | `/pc reload` | Reloads configuration, cosmetics, and packs | `personachat.reload` |
| `/headd [player] [2d\|3d\|clear]` | | Debug command to preview avatar display | `personachat.admin` |

### Permissions
- `personachat.use` *(Default: true)* - Allows using floating chat and equipping unlocked cosmetics.
- `personachat.menu` *(Default: true)* - Allows opening the cosmetics GUI.
- `personachat.reload` *(Default: op)* - Allows reloading configurations.
- `personachat.admin` *(Default: op)* - Administrative command access.

---

## 🎨 Modular Pack Configuration Example

Create your own pack YAML file inside `plugins/PersonaChat/packs/cyberpunk.yml`:

```yaml
pack-id: "cyberpunk"
display-name: "&#00FFFF⚡ Cyberpunk Edition"
author: "TiNYsx"
version: "1.0.0"

# ----------------------------------------------------
# Part 1: Asset Registry (Font Glyphs & Shifts)
# ----------------------------------------------------
assets:
  cyber_bg_single:
    char: "\uE010"
    prefix-shift: "%img_offset_-16%"
  cyber_bg_top:
    char: "\uE011"
    prefix-shift: "%img_offset_-16%"
  cyber_bg_mid:
    char: "\uE012"
    prefix-shift: "%img_offset_-16%"
  cyber_bg_bot:
    char: "\uE013"
    prefix-shift: "%img_offset_-16%"

# ----------------------------------------------------
# Part 2: Decorations
# ----------------------------------------------------
decorations:
  cyber_matrix_bubble:
    type: CHAT_BACKGROUND
    name: "&#00FFFF「 Cyber Matrix Bubble 」"
    permission: "personachat.pack.cyberpunk.bubble"
    description:
      - "&7Dynamic multi-line cyber box"
      - "&7with 3-slice font images."
    icon: END_CRYSTAL
    settings:
      mode: IMAGE
      single-line-asset: "cyber_bg_single"
      top-slice-asset: "cyber_bg_top"
      middle-slice-asset: "cyber_bg_mid"
      bottom-slice-asset: "cyber_bg_bot"
      format: "&#00FFFF「 &f{message} &#00FFFF」"
      background-opacity: 120

  netrunner_badge:
    type: NAMEPLATE
    name: "&#00FFFF[NetRunner] ✦"
    permission: "personachat.pack.cyberpunk.netrunner"
    description:
      - "&7Exclusive NetRunner title."
    icon: NETHER_STAR
    settings:
      format: "&#00FFFF[NetRunner] &b✦ &7|"
```

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 21** or higher
- **PowerShell** (Windows)

### Build Command
Run the included build script:
```powershell
.\build.ps1
```
The compiled jar will be created as `PersonaChat-1.0.jar`.

---

## 🤝 Contributions
Contributions are welcome! Please submit a pull request or open an issue on the official GitHub repository.

---

## 💬 Support
If you encounter issues or have questions, feel free to open an issue on the GitHub repository or contact directly via Discord: `tiny.tinysx`  
Discord Support Server: `https://discord.gg/JMdjgnzG8W`
