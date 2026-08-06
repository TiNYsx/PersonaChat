# 💬 PersonaChat

[![Release](https://img.shields.io/badge/Release-v1.0-brightgreen.svg)](https://github.com/TiNYsx/PersonaChat)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-orange.svg)](https://papermc.io/)
[![Author](https://img.shields.io/badge/Author-TiNYsx-blue.svg)](https://www.spigotmc.org/members/jirwat.457182/)
[![License](https://img.shields.io/badge/License-Custom%20License-lightgrey.svg)](LICENSE)

**PersonaChat** is an advanced Persona 3D/2D Floating Chat and Profile Customization plugin for Paper/Spigot Minecraft servers. It renders stylized chat bubbles directly in the player's world with support for external resource pack fonts, custom models, two-layer 3-slice background stretching, synchronized avatar frames, hotbar history scrolling, and personalized cosmetic customization via interactive GUIs.

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

### 🎮 Persona-Style Floating Chat Displays
- **Native World-Space Displays**: Messages float smoothly in front of players using native Minecraft `ItemDisplay` and `TextDisplay` entities with **zero client-side mods needed**.
- **2D Half-Body & 3D Heads**: Supports **3D Block Heads** and **2D Flat Half-Body/Bust Avatars** (using billboard matrix transformations and custom shaders).
- **Mathematical Layout & Animation**: Real-time expression evaluator supporting variables (`i` = message index, `t` = time, `l` = lifetime, `r` = random seed) and functions (`sin`, `cos`, `abs`, etc.) for dynamic floating animations, offsets, and rotations.

### 🖼️ Two-Layer Display & 3-Slice Font-Image Backgrounds
- **Dual-Layer Rendering**: Spawns an independent background `TextDisplay` layer positioned directly behind the foreground text card, eliminating text-blocking background opacities and Z-fighting.
- **Dynamic 3-Slice Expansion**: Multi-line messages dynamically calculate line heights and expand using 3-slice background graphics (`Top + (Middle * Lines) + Bottom` or `Single-Line`), factoring in vertical padding (`padding-y`).
- **Zero Shatter Rendering**: Background glyphs use centered bounds to prevent artificial word-wrapping or fragmented image lines.

### 👑 Profile Picture Frames & Cosmetic System
- **Synchronized Head & Frame Rotations**: Frames match the player head's yaw, pitch, and roll in 100% lockstep using `leftRotation` matrix transforms.
- **Dual Frame Types**:
  - **2D Font-Image Frames**: Transparent `TextDisplay` frames rendering custom resource pack font glyphs or Unicode characters.
  - **3D Model Frames**: `ItemDisplay` frames rendering custom items or `custom-model-data` models (e.g. halos, visors, hats).
- **Titles & Badges**: Custom labels and font glyph prefixes before player names (e.g. `🗡 [VIP] ✦ |`).
- **Interactive Chest GUI (`/pc menu`)**: Intuitive categorized menu to browse, preview, and equip unlocked cosmetics.

### 📐 Precise Alignment & Padding Controls
- **Fine-Tuning Offsets**: Dedicated `head-vertical-offset` and `head-horizontal-offset` settings (supporting math expressions) to perfectly align avatars with chat boxes.
- **Unified Card Layout**: Merges player name and message into a seamless card with uniform width filling (`fill-line-width`) and customizable horizontal (`padding-x`) and vertical (`padding-y`) padding.
- **Layout Alignments**: Supports `LEFT` (head on left), `RIGHT` (head on right), and `CENTER` (centered card).

### 🔄 Standing-Still History Scrolling
- Keep moving without clutter! In `STANDING_STILL` mode, chat smoothly appears when stationary.
- Scroll through up to 50 previous messages using the hotbar mouse wheel when standing still.

### ⚡ Built-in Resource Pack Generator & PAPI Integration
- **Auto Resource Pack Generation**: Generates standard Minecraft resource packs with modern format compatibility (`pack_format: 84` / `min_format` / `max_format`) for 2D half-body shaders and font offsets.
- **PlaceholderAPI**: Full PAPI support across all badges, names, messages, frames, and background shifts.

---

## 📥 Installation

### Soft Dependencies
1. **[ItemsAdder](https://www.spigotmc.org/resources/%E2%9C%A8itemsadder%E2%AD%90emotes-mobs-items-armors-hud-gui-emojis-blocks-wings-hats-liquids.73355/)**, **[Oraxen](https://www.spigotmc.org/resources/%E2%98%84%EF%B8%8F-oraxen-custom-items-blocks-emotes-furniture-resourcepack-and-gui-1-18-1-21-4.72448/)**, or **[NexO](https://mcmodels.net/products/13172/nexo?srsltid=AfmBOooeUFusiEq8tfccU_E5sGRq8vCtXQFUn5MNkKESvfpLZThkB0O0)** (Optional for custom font glyphs and models)
2. **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)** (Optional for player placeholders)

### Setup
1. Place `PersonaChat-1.0.jar` into your server's `plugins/` directory.
2. Restart or reload your server.
3. Configure settings in `plugins/PersonaChat/config.yml` and add custom packs in `plugins/PersonaChat/packs/`.

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
| `/personachat reload` | `/pc reload` | Reloads configurations, cosmetics, and packs | `personachat.reload` |
| `/headd [player] [2d\|3d\|clear]` | | Debug command to preview avatar display | `personachat.admin` |

### Permissions
- `personachat.use` *(Default: true)* - Allows using floating chat and equipping unlocked cosmetics.
- `personachat.menu` *(Default: true)* - Allows opening the cosmetics GUI.
- `personachat.reload` *(Default: op)* - Allows reloading configurations.
- `personachat.admin` *(Default: op)* - Administrative command access.

---

## 🎨 Modular Pack Configuration Example

Create custom pack definitions inside `plugins/PersonaChat/packs/cyberpunk.yml`:

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
  cyber_visor:
    char: "\uE020"

# ----------------------------------------------------
# Part 2: Decorations (Frames, Badges, Backgrounds)
# ----------------------------------------------------
decorations:
  # 1. Dynamic 3-Slice Background
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
      background-opacity: 0

  # 2. Synchronized Font-Image Profile Frame
  cyber_visor_frame:
    type: PROFILE_FRAME
    name: "&#00FFFF⚡ Cyber Visor Frame"
    permission: "personachat.pack.cyberpunk.visor"
    description:
      - "&7Futuristic HUD visor attached to avatar."
    icon: BEACON
    settings:
      asset: "cyber_visor"
      scale: 1.15
      offset-y: 0.02

  # 3. Custom Name Title / Badge
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
