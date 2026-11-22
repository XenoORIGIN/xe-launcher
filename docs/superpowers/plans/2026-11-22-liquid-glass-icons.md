# Liquid Glass Icons Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the "Clear glass" icon style with iOS-26-style Liquid Glass: see-through tiles showing a frosted wallpaper crop, milky-white symbols, thick bright rims. Apply it to icons, the dock, folders and widgets.

**Architecture:**
- One frosted copy of the wallpaper (`WallpaperController.glassBackdrop`) is shared by every glass element. A `Modifier.liquidGlass(shape)` draws, for its own window rect, the matching crop (same mapping as `GlassLens`), plus a film, rim and shadow.
- A new icon-cache variant turns symbols milky white.
- `LocalLiquidGlass` (on when `iconStyle == GLASS_CLEAR`) switches the dock bar, folder popup and widget panels to `LiquidSurface`.

**Tech Stack:** Kotlin, Jetpack Compose, Robolectric + Roborazzi.

**Spec:** docs/superpowers/specs/2026-11-22-liquid-glass-icons-design.md

## Global Constraints
- minSdk 29, target 36. No new dependencies.
- Frosted backdrop is about 360 px wide (≈1.1 MB ARGB).
- `IconStyle.GLASS_CLEAR` keeps its enum name (it is stored in settings). Only its label and description change: "Liquid Glass" (en and th). Description: "Clear glass with a white symbol, like iOS" / "กระจกใสแบบ iOS สัญลักษณ์สีขาว".
- Symbol is milky white at ≈85% opacity with a faint ≈18% black edge or shadow.
- Tile layers, bottom to top:
  1. shadow (≈2 dp down, ≈25% black)
  2. backdrop crop
  3. white film ≈8%
  4. symbol
  5. rim ≈2× today's stroke, with top-left and bottom-right highlights
- Existing rim tint and Icon glass sheen or lens still layer on top.
- Release as 1.0.15 (GITHUB_RUN_NUMBER=15).

## Review Focus
1. No wallpaper yet (backdrop null on first start) → tiles show a frosted film, no crash. Test `liquidGlassWithoutBackdropDrawsFilm` in Task 2.
2. Pure white or pastel wallpaper → white symbols must stay readable. Screenshot `home_liquid_white` in Task 4, reviewed by eye.
3. Custom icons the user picked (gallery or drawing) under Liquid Glass → shown milky white, not as a raw photo. Test `customIconUnderLiquidGlassIsWhitened` in Task 3.
4. Tile moved, for example the drawer scrolling or a page swipe → the crop follows the tile's current window position. Test `backdropSourceFollowsParallax` in Task 2. The reviewer checks the position is re-read on layout.
5. Liquid Glass off → dock, folder popup and widgets look exactly as today. Test `liquidSurfaceOffUsesGlassSurface` in Task 5.

---

### Task 1: Frosted glass backdrop
**Files:**
- Modify: `wallpaper/WallpaperRenderer.kt`, `wallpaper/WallpaperController.kt`
- Test: `wallpaper/WallpaperControllerTest.kt`

**Interfaces:**
- Produces:
  - `fun WallpaperRenderer.glassBackdrop(src: Bitmap): Bitmap`: width 360 (height keeps aspect), box blur radius 3, each channel lerped 12% toward 255.
  - `val WallpaperController.glassBackdrop: StateFlow<ImageBitmap?>`: set in `show()` next to `backdrop`.

- [ ] Test `glassBackdropIsSmallBlurredAndBrighter`:
  - render a 900×1950 striped bitmap (black and white 10 px stripes)
  - expect width == 360
  - expect average luminance > the source's
  - expect the max neighbour difference along x < 255/2 (blurred)
- [ ] Test `controllerPublishesGlassBackdrop`: after `apply(XeCollection.first(), HOME)`, `glassBackdrop.value` is non-null.
- [ ] RED → implement → GREEN (`/home/claude/bin/gt 'com.xenoorigin.xelauncher.wallpaper.*'`) → commit.

### Task 2: Liquid glass modifier and mapping
**Files:**
- Create: `design/LiquidGlass.kt`
- Test: `design/LiquidGlassTest.kt`

**Interfaces:**
- Consumes: `home.wallMapping(bmp: IntSize, screen: Size, parallaxPx: Offset): WallMapping` (internal, same module).
- Produces:
  - `class GlassBackdrop(val image: () -> ImageBitmap?, val parallaxPx: () -> Offset, val screen: () -> Size)`
  - `val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }`
  - `val LocalLiquidGlass = compositionLocalOf { false }`
  - `fun backdropSource(tile: Rect, bmp: IntSize, screen: Size, parallaxPx: Offset): Rect`: the bitmap rect behind `tile`.
  - `fun Modifier.liquidGlass(shape: Shape, film: Float = .08f): Modifier`:
    - reads its rect via `onGloballyPositioned { positionInRoot }` into a non-recomposing holder, read at draw time
    - draws the layers listed in Global Constraints
    - with no backdrop, draws the film at .18 instead
    - rim uses `GlassStyle.rimTint`

- [ ] Tests:
  - `backdropSourceMatchesWallpaperBehindTile`: bmp 360×780, screen 1080×2340, zero parallax. The centre tile `Rect(490f,1120f,590f,1220f)` maps to a rect centred at (180, 390) ±1 with width 100/k, where k = 3×1.12.
  - `backdropSourceFollowsParallax`: parallax (+33.6, 0) shifts source left by 10 px ±.5.
  - `liquidGlassWithoutBackdropDrawsFilm` (Robolectric compose): a 60 dp box with `liquidGlass(CircleShape)` and no `LocalGlassBackdrop` renders without throwing. The centre pixel is not fully transparent.
  - Screenshot `liquid_tile.png`: 4 tiles over a coloured stripe wallpaper backdrop, reviewed by eye.
- [ ] RED → implement → GREEN → commit.

### Task 3: Milky-white symbols
**Files:**
- Modify: `icons/IconProcessor.kt`, `icons/IconRepository.kt`
- Test: `icons/IconProcessorTest.kt` (create if missing), `icons/IconRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `fun IconProcessor.milkyWhite(src: Bitmap, shade: Float = .15f): Bitmap`: alpha kept. rgb = 255 − (255 − lum)·shade.
  - `fun IconProcessor.milkyFromFull(src: Bitmap): Bitmap`: grayscale, rgb as `milkyWhite`, alpha × (.25 + .75·lum/255).
  - `Variant.GLYPH_WHITE`: `variantOf(GLASS_CLEAR) = GLYPH_WHITE`.
    - glyph → `milkyWhite(glyph)`, fillsTile = false
    - non-glyph or custom → `milkyFromFull(whole)`, fillsTile = false (drawn inside the glass)

- [ ] Tests:
  - `milkyWhiteKeepsShapeAndIsLight`: every pixel keeps its alpha. Every opaque pixel has r, g, b ≥ 216.
  - `milkyFromFullFadesDarkAreas`: a black pixel's alpha is < a white pixel's alpha.
  - `customIconUnderLiquidGlassIsWhitened`: a repo with a custom red bitmap, `icon(key, GLASS_CLEAR, 96, 1f)`. The centre pixel's r, g and b are all ≥ 216, and `fillsTile == false`.
- [ ] RED → implement → GREEN → commit.

### Task 4: Liquid Glass app tiles and folder tiles
**Files:**
- Modify: `icons/AppIcon.kt` (GLASS_CLEAR branch, around line 136), `home/HomeScreen.kt` (folder tile, around line 394)
- Test: `icons/AppIconScreenshotTest.kt`, `home/HomeScreenshotTest.kt`

**Interfaces:**
- Consumes: `Modifier.liquidGlass`, `LocalLiquidGlass`, `Variant.GLYPH_WHITE` output.
- Produces: the GLASS_CLEAR tile:
  - `Box(Modifier.fillMaxSize().liquidGlass(s))`
  - symbol `Image` at alpha .85, scale `glyphScale` (non-fills) or .72 (former full icons)
  - a 1 dp ≈18% black shadow under the symbol, drawn as an offset tinted copy
- Folder tile: under GLASS_CLEAR it uses `liquidGlass(fs)`.

- [ ] Screenshots, reviewed by eye against the video frames:
  - `home_liquid.png` (busy photo-like wallpaper)
  - `home_liquid_white.png` (white wallpaper)
  - `drawer_liquid.png`
- [ ] Test `liquidTileUsesWhiteSymbol`: AppIcon in GLASS_CLEAR with a sample repo. The rendered centre pixel's r, g and b are all > 200.
- [ ] Implement → screenshots reviewed → full `/home/claude/bin/gt` → commit.

### Task 5: Dock, folder popup and widgets go clear
**Files:**
- Modify: `design/LiquidGlass.kt` (add `LiquidSurface`), `home/HomeScreen.kt` (dock bar, around line 427), `home/FolderPopup.kt:89`, `widgets/GlassWidgets.kt` (125, 337–339), `XeApp.kt` (provide locals)

**Interfaces:**
- Produces:
  - `@Composable fun LiquidSurface(shape: Shape, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)`. When `LocalLiquidGlass` is true it uses `liquidGlass(shape, film = .10f)`; otherwise it calls `GlassSurface(shape, modifier, content = content)` unchanged.
  - XeApp provides `LocalLiquidGlass provides (settings.iconStyle == IconStyle.GLASS_CLEAR)` and `LocalGlassBackdrop provides GlassBackdrop({ graph.wallpaper.glassBackdrop.value }, parallax, { screenSize })`.

- [ ] Tests:
  - `liquidSurfaceOffUsesGlassSurface`: compose both modes. With the flag off, the rendered pixels equal a plain `GlassSurface` render (same bitmap hash).
  - Screenshots `folder_liquid.png` and `widgets_liquid.png`, reviewed by eye.
- [ ] Implement → GREEN → commit.

### Task 6: Name, website, release 1.0.15
**Files:** `res/values*/strings.xml` (`style_glass_clear`, `style_glass_clear_desc`), `website/play.js` (STYLES label + clear-style drawing), `website/style.css` if needed, `website/app.js` NEWS, `release/notes.md`, `release/version.txt`

- [ ] Change the strings per Global Constraints. In the web demo, rename the style to "Liquid Glass" and restyle "clear": see-through tile (backdrop-filter blur 6px brightness 1.12), white symbol at .85, 2 px bright rim, soft shadow.
- [ ] Checks:
  - `node -e new Function(...)`
  - `python3 website/check.py`
  - all three module suites
  - lint
- [ ] Final whole-branch review: fresh opus reviewer (executing-plans), then a fix pass.
- [ ] Build both APKs with GITHUB_RUN_NUMBER=15. Verify the cert ea0e6430…a09c68 and version 1.0.15.
- [ ] Release:
  - write release files (Thai + English notes)
  - push `HEAD:main`
  - confirm the release has both APKs
  - republish the website files to artifact 62318dc7-062e-45b8-bdb3-ac864a0e5af8
  - reply in Thai with both website links
