# Liquid Glass icons (replaces "Clear glass") — design

## Intent
Kantaphon saw a TikTok of a phone whose icons look like iOS 26 "clear" Liquid Glass and wants Xe's icons to look like that. In the video:
- each icon is a clear glass block: you see the wallpaper through it, a little frosted and brighter
- every app's symbol is the same milky white, half see-through, so the whole screen reads as one set
- each block has a thick bright edge with light catching the top-left and bottom-right, and a soft shadow under it
- dock icons are the same glass

## Owner's decisions
1. Symbol colour: **milky white for every app** (no app colours).
2. **Replace** the existing "Clear glass" style (`IconStyle.GLASS_CLEAR`). It is renamed "Liquid Glass" / "Liquid Glass". The four themes that use it (Xe Blue, Ocean, Violet Glass, Liquid Azure) get the new look automatically.
3. Reach: **app icons, dock, folders and widgets**.
4. See-through method: **A, sample a pre-frosted copy of the wallpaper** (see Approaches).

## Goals
- Home, dock, drawer, folder tiles and the folder popup show the clear-glass look when the style is Liquid Glass.
- Wallpaper inside each icon lines up with the wallpaper behind it, including page parallax and tilt (depth parallax).
- Xe's own widgets get a clear-glass panel. Other apps' widgets get a clear-glass frame around them; their content stays as the app draws it.
- Smooth on a Galaxy A16 5G (API 34–36) and the OPPO, down to API 29.

## Non-goals
- No live per-icon blur.
- No change to other icon styles, shapes, Studio themes or the Icon glass (Off/Light/Lens) effect, which still layers on top.
- No redesign of Xe's settings sheets or menus (they keep the current glass).

## Anti-goals (a failure even if it works)
- Home scrolling or opening the drawer gets slower or hotter than with the old Clear glass.
- Symbols become hard to read on a bright wallpaper. White symbols need a faint dark edge or shadow, so they stay legible on white or pastel wallpapers.
- Wallpaper inside the icons visibly slides out of step with the wallpaper behind them.

## Approaches considered
- **A. Sample a frosted copy of the wallpaper (chosen).** One medium-resolution, lightly blurred and brightened copy of the current wallpaper is kept in memory. Each glass tile draws the part of it that sits behind the tile. It is cheap (one bitmap, a clipped draw per tile), works on API 29+, and follows parallax. On Live Sky the copy only refreshes when the saved picture does (every 15 min), so stars and clouds don't twinkle inside icons.
- B. Live blur per icon (Haze on every tile): truest look, but 20–30 blur passes would make the A16 lag and drain the battery. Rejected.
- C. Frosted white film with no see-through: cheapest, but not the look in the video. Rejected.

## How it works (decided)
- **Frosted backdrop.** `WallpaperController` exposes a second bitmap, `glassBackdrop`, made from the same source as `current`:
  - about 360 px wide
  - blurred a little (about radius 3 at that size)
  - brightened about 12% toward white
  - rebuilt whenever `current` changes
  - The existing 120 px `backdrop` stays for panels.
- **Mapping.** A tile finds its rectangle in the window and maps it to wallpaper coordinates the same way `GlassLens` does (`WALL_SCALE` and the current parallax offset). It then draws that crop clipped to the icon shape. The mapping is a pure function so it can be tested.
- **Tile layers**, bottom to top:
  1. soft shadow (offset down about 2 dp, blurred, black at about 25% alpha)
  2. backdrop crop
  3. white film at about 8%
  4. symbol
  5. thick bright rim (about 2× today's stroke, plus a highlight top-left and bottom-right)
  - The existing rim tint (time of day) and Icon glass sheen or lens still apply on top.
- **Milky-white symbol.**
  - Rendered from the existing grey glyph, with every pixel taken to white while keeping a little shading (about 15% of the grey's darkness) so it isn't flat.
  - Drawn at about 85% opacity.
  - Has a faint dark outline or shadow (about 18% black) for bright wallpapers.
  - New icon-cache variant so cached old Clear-glass PNGs aren't reused.
  - Apps whose icon fills the whole tile (no separable symbol) are shown as that icon, greyed to white the same way and clipped inside the glass.
- **Drawer.** The drawer sits on its own blurred backdrop. Tiles there sample `glassBackdrop` at their screen position too, so they look the same as on Home.
- **Folders.** Folder tiles use the same glass with mini white symbols. The folder popup panel uses a clear-glass panel (backdrop crop + rim) instead of the dark film.
- **Dock.** Dock icons use the tile above. The dock bar behind them becomes a lighter clear-glass bar when this style is on, so the icons don't sit on a dark slab.
- **Widgets.**
  - Xe's widgets (clock, date, battery ring, music and the rest) use the clear-glass panel when the icon style is Liquid Glass. Text and rings switch to the milky-white palette.
  - Other apps' widgets get the clear-glass panel behind them, as a frame.
- **Name and copy.** The style is called "Liquid Glass" in both languages. The description reads "Clear glass with a white symbol, like iOS" / "กระจกใสแบบ iOS สัญลักษณ์สีขาว".
- **Website.** The web demo's "clear" style (play.js) is restyled to match: white symbol, see-through tile, thick rim. The style is renamed in its style list. Both website links go in the reply as usual.

## Constraints
- minSdk 29, target 36. No new dependencies.
- Memory: the frosted backdrop is at most about 360×780 ARGB (about 1.1 MB).
- Tests: Robolectric plus Roborazzi screenshots of Home, the drawer, a folder and widgets with the new style on a busy wallpaper, reviewed by eye. Also unit tests for the mapping and the white-symbol conversion.
- Release as 1.0.15 using the usual release flow.

## Left to the builder
- Exact blur, brighten, rim and shadow numbers, tuned by looking at screenshots against the video frames.
- How tiles get their window position cheaply. For example, share one position per grid plus each cell's offset instead of a per-icon `onGloballyPositioned`, if the per-icon version is slow.
- Whether the dock bar change needs a separate toggle. Default: no, it follows the icon style.

## Builder check (done by the author)
Checked as a fresh builder would. Settled as the author's calls, marked above:
- What happens to whole-tile icons: greyed white and clipped.
- Legibility on bright wallpapers: faint dark edge.
- Cache invalidation: new variant.
- Drawer sampling: same frosted backdrop.
- Dock bar look: lighter clear bar.

Nothing left needs the owner.
