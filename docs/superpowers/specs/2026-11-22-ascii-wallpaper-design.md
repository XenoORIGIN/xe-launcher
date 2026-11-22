# ASCII wallpapers — design

Date: 2026-11-22 · Owner: Kantaphon · Status: draft for approval (builder check done)

## Intent

Kantaphon saw a lo-fi "terminal" scene (a night landscape drawn from tiny dots and dashes, one small coloured
character in front) and wants that look in Xe Launcher as wallpapers. The same renderer will later be the
background of the Termi pet widget (separate design), so it must be reusable at any size.

Reference look (from the concept clip already made, `xebi/lake/backdrop.py`): dark grey ground `#18181A`,
the picture made of a grid of small glyph cells; brighter areas use denser glyphs (`.` `:` `-` `=` and
horizontal-line stacks), dim areas are empty. Mostly grey, with colour only on highlights (lantern, sun).

## Goals

1. **One live ASCII wallpaper** ("ASCII Live") that moves and follows the real time of day: day with sun,
   golden hour/sunset warm, night with moon and twinkling stars. Water shimmers, clouds drift.
2. **Seven still ASCII wallpapers**, one per scene: Lake at night, Rain on the lake, Dawn over mountains,
   Pine forest with fireflies, City at night, Space (planet, meteor), Sea with lighthouse.
3. **Colour mode, chosen by the user** (stored setting, applies to every ASCII wallpaper):
   - `MONO` (default): grey glyphs, colour only on highlights (sun, moon glow, lamps, lit windows,
     lighthouse beam, fireflies, planet ring).
   - `SKY`: glyphs take colours from the sky — for the live one from `skyAt()` (blue day, orange/pink
     dusk, navy night); for stills from the scene's own fixed palette.
   - `ACCENT`: all glyphs in the theme accent colour (`settings.accent`), highlights stay brighter.
4. Pixel-crisp on every phone: glyph pixels are scaled with nearest-neighbour, never blurred.

## Non-goals

- An Android system live-wallpaper service for ASCII (the existing "Xe Live Sky" service stays sky-only).
  Outside Xe the phone shows a still copy that is refreshed every 15 minutes, exactly like Live Sky today.
- Turning the user's photos into ASCII (possible later).
- The Termi widget itself (next design).

## Anti-goals (would be a failure even if it works)

- Battery drain: no drawing while Xe is not on screen, while animations are off, or in power-save.
- Icons and labels hard to read: the scene stays dark overall (mean brightness well below Live Sky's day).
- Blurry or font-dependent look: no system fonts, no text drawing; glyphs are our own bitmaps.

## Constraints

- minSdk 29, Kotlin + Compose (existing app). Must run smoothly on Samsung Galaxy A16 5G and Android 11.
- Follows the existing wallpaper architecture: `WallpaperPreset` → `WallpaperRenderer.render(p, w, h)`
  for stills and the system copy; a Compose layer inside `WallpaperLayer` for live drawing; the
  15-minute refresh path currently used for Live Sky (`refreshLiveSky`, `SkyMirrorJob`, lock-screen copy).
- No new dependencies.

## Approaches considered

| | Approach | Trade-off |
|---|---|---|
| **A (chosen)** | Port the concept renderer to Kotlin: compute a brightness/tint value per cell, stamp our own glyph bitmaps into a small `IntArray`, show it as a bitmap scaled up with nearest-neighbour. Live redraw ~8 fps. | One code path for live, stills, system copy and the Termi widget; colour modes and time of day are free; tiny APK cost. Some CPU while Home is visible. |
| B | Pre-rendered video / frame sequences in the APK | Cheapest CPU, but large APK, cannot follow time or colour mode, fixed resolution. |
| C | Draw real text characters with `drawText` | Tens of thousands of text draws per frame (slow on A16), look changes with each phone's font. |

Kantaphon chose A.

## Design

### Grid and raster

- Glyph cell = 5 × 8 base pixels. *(Builder call, from the builder check)* Each base pixel is drawn as a
  **whole number** `k` of screen pixels so glyphs never come out 3 px and 4 px wide side by side:
  `k = max(2, round(drawnWidth / 360))` where `drawnWidth = screenWidth × WALL_SCALE` on Home (the 1.12
  parallax zoom) and `screenWidth` for the system copy. Columns = `ceil(drawnWidth / (5k))` (about 72–81),
  rows likewise from the height. The raster is drawn at exactly `k×` with no filtering; the parallax
  translation is rounded to whole pixels so nothing shimmers while swiping. Stills shown inside Xe use
  `FilterQuality.None` too (the existing `Image` call gets that flag for ASCII presets).
- Glyph set (index = ink, 0 empty): `.` `:` `-` `=` two-line `=` three-line `≡` four-line, plus `/` for rain
  and `*` for sparkles. Same bitmaps as the concept (`backdrop.py` `_G`), extended with `*`.
- Per cell the scene produces brightness `L ∈ 0..1` and a tint (packed ARGB or "none"). Glyph index =
  `round(L*7 + jitter)` with a fixed per-cell jitter (±0.35) for the dithered texture; `L < 0.09` → empty.
  Glyph colour = grey level `60 + 190·L` (MONO), mixed toward the tint where the cell has one.
- Ground colour `#18181A` (all modes).

### Scenes (portrait composition, all coordinates relative to the grid)

Shared parts (from the concept): sky gradient, moon/sun disc with halo and crater texture, horizontal
cloud bands from stretched fBm noise drifting with time, twinkling star cells, far ridge (bright streaked
rock + snow tips) and near ridge (dark), water that mirrors the area above the horizon with a sine wobble
plus moving shimmer streaks and a light column under the moon/sun, shore band with grass tufts.
Horizon at ~58% of the height; the sky takes the top so the clock/widgets sit on calm sky.

1. **Lake (night)** — moon upper-left, clouds, mountains, lake, shore. Live wallpaper uses this layout.
2. **Rain** — heavy clouds, no moon, `/` streaks over everything, random ripple cells on the water.
3. **Dawn** — sun disc just above the ridge, warm glow band at the horizon (tint gold/pink), mist band.
4. **Pines** — foreground pine silhouettes (stacked triangles, dark), moon, firefly sparkles `*` tinted
   yellow-green, no lake (forest floor instead).
5. **City** — skyline of rectangles of different heights, lit windows (gold highlight cells, ~30% lit),
   moon, river reflection at the bottom.
6. **Space** — dense star field, large shaded planet with a tilted ring (ring tinted), soft nebula noise,
   one meteor streak.
7. **Lighthouse** — sea with wave rows, rocky cliff on the right, lighthouse tower, beam cone (gold tint)
   sweeping left, stars.

### Live behaviour ("ASCII Live")

- Scene = Lake; look at the current minute from `skyAt(minute, sunToday())`: `stars` controls star and
  moon visibility; `sunUp` swaps moon for a sun disc (no craters, brighter halo); `orbX/orbY` places the
  disc (clamped to the sky area); sky brightness from the luminance of `top/mid` so day is brighter but
  still dark enough for icons (cap sky `L` at 0.35 by day).
- Motion (cloud drift, shimmer, star twinkle) advances with real time while drawing.
- Draws ~8 fps (`delay(125)`), only while `skyLoopRuns(animate, lifecycle)` is true. Otherwise one still
  frame per minute (like `LiveSkyLayer`).
- The system wallpaper and lock-screen copies use the existing 15-minute bucket logic; the controller and
  `SkyMirrorJob` treat `ascii_live` like `live_sky` (generalised to "time-following wallpaper ids"). The
  "use the system live wallpaper" offer sheet stays Sky-only.

### Code layout (new package `wallpaper/ascii`)

- `AsciiGlyphs.kt` — glyph bitmaps (5×8 masks), glyph index helper.
- `AsciiField.kt` — `class AsciiField(cols, rows, seed)` with precomputed noise; `fun fill(scene, t, look,
  colorMode, accent, out: CellBuffer)` writes `L` and tint per cell. One function per scene.
  *(Builder call)* Scenes are written in **relative coordinates** (fractions of the grid) so the same code
  works on any grid shape — portrait for wallpapers, wide and small for the Termi widget later. The field
  exposes the ground row it used (`groundRow`) so a character can stand on it. Tests cover one portrait
  and one landscape grid.
- `AsciiRaster.kt` — `fun rasterize(cells: CellBuffer, pixels: IntArray, rasterW)`; reuses buffers, no
  allocation per frame.
- `AsciiRenderer.kt` — `fun render(p: WallpaperPreset.Ascii, w, h, colorMode, accent, minute): Bitmap`
  (still frame, scaled to the screen with no filtering).
- `AsciiLayer.kt` — Compose layer for the live wallpaper; owns one raster `Bitmap`, redraws it in a
  `LaunchedEffect`, draws with `FilterQuality.None`, same parallax `graphicsLayer` as the other layers.
- `WallpaperPreset.Ascii(id, name, scene: AsciiScene, live: Boolean, minute: Int? = null)`; ids
  `ascii_live`, `ascii_lake`, `ascii_rain`, `ascii_dawn`, `ascii_pines`, `ascii_city`, `ascii_space`,
  `ascii_lighthouse`. Registered in a new `AsciiCollection`, included in `allWallpapers()`.
- `WallpaperRenderer.render` gets the `Ascii` branch (reads colour mode and accent passed in by the
  controller; previews use the current setting).
- `XeSettings.asciiColor: AsciiColor = MONO` (persisted like the other enums).
- *(Builder call)* Colour is chosen in two places. In the wallpaper editor the colour row only changes the
  preview; it is saved together with the wallpaper when the person taps Apply. In Settings → Wallpaper
  there is also an "ASCII colour" row that saves immediately; if an ASCII wallpaper is in use on Home it is
  redrawn there (and on Lock too if it was applied to both).
- *(Builder call)* Time-following wallpapers remember whether they are on Home: new setting
  `liveOnHome: Boolean` (written by `apply`: `target != LOCK`). The 15-minute refresh writes the Home
  copy only when it is true, and the Lock copy only when `liveSkyLock` is true (renamed in code to mean
  "the time-following wallpaper is on Lock", setting key kept for compatibility). This also fixes the same
  issue for Live Sky applied to Lock only. Mixed setups (two different time-following wallpapers on Home
  and Lock) are not supported: the last one applied wins, as today.

### UI

- Wallpaper screen: new group **"ASCII"** under the Xe collection, thumbnails of the 8 wallpapers
  (`ascii_live` first, marked with the existing live badge style if one exists, otherwise its caption
  says it follows the time).
- Wallpaper editor sheet for an ASCII preset: a 3-option colour row (Mono / Sky / Accent, Thai + English),
  live preview updates; Apply as today (Home / Lock / Both).
- Strings in English and Thai.

## Testing

- Field: deterministic for a seed; moon cells brighter than surrounding sky; night has star cells and day
  has none; rain scene has `/` cells; city has tinted window cells; colour modes: MONO leaves non-highlight
  cells untinted grey, ACCENT tints them with the accent, SKY uses the look's colours.
- Raster: glyph pixels land in the right cell; empty cells stay ground colour; buffer reuse (no resize).
- Live: day vs night frames differ in disc type and stars; `skyLoopRuns` gating reused.
- Controller: `ascii_live` refreshes on the 15-minute bucket like Live Sky; stills never refresh.
- Screenshot tests (Roborazzi) of all 8 wallpapers in MONO plus Lake in SKY and ACCENT.
- Performance check in a unit test: filling + rasterizing one 72 × 98 frame stays well under 15 ms on the
  JVM (guards against accidental per-cell allocation).

## Left to the builder

Exact noise constants, glyph tweaks, per-scene palettes for SKY mode, thumbnail size, how the live
badge looks, string wording.
