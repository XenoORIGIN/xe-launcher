# ASCII Wallpapers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one time-following animated ASCII wallpaper and seven still ASCII scenes to Xe Launcher, with a user-chosen colour mode.

**Architecture:** A pure-Kotlin cell field (`AsciiField`) computes brightness, colour and glyph overrides per cell for a scene; `AsciiRaster` stamps 5×8 glyph bitmaps into an `IntArray`; `WallpaperRenderer` turns that into a still bitmap scaled by a whole number, and `AsciiLayer` redraws it ~8 fps on Home. The existing Live Sky refresh path is generalised to "time-following wallpapers".

**Tech Stack:** Kotlin, Jetpack Compose 1.9, Robolectric (SDK 34, NATIVE graphics), Truth, Roborazzi screenshots via `SHOT_DIR`.

**Spec:** `docs/superpowers/specs/2026-11-22-ascii-wallpaper-design.md` (concept renderer: `/home/claude/xebi/lake/backdrop.py`)

## Global Constraints

- minSdk 29, no new dependencies.
- Glyph cell 5 × 8 base pixels; ground colour `#18181A`; glyph grey `60 + 190·L`; `L < 0.09` → empty cell; glyph index `round(L*7 + jitter)`, jitter fixed per cell in ±0.35.
- Base pixel drawn as a whole number `k` of screen pixels: `k = max(1, round(drawnWidth / 360))`, `drawnWidth = screenWidth × WALL_SCALE` on Home, `screenWidth` for the system copy. Renders narrower than 360 px are drawn at 360 px wide (k = 1) and scaled down with filtering (thumbnails).
- Live ASCII draws only while `skyLoopRuns(animate, lifecycle)`; ~8 fps (`delay(125)`).
- Ids: `ascii_live`, `ascii_lake`, `ascii_rain`, `ascii_dawn`, `ascii_pines`, `ascii_city`, `ascii_space`, `ascii_lighthouse`.
- Colour modes `AsciiColor { MONO, SKY, ACCENT }`, default `MONO`. Highlights (sun, moon glow, lamps, windows, beam, fireflies, ring) keep their own tint in every mode.
- Daytime live sky brightness capped at `L ≤ 0.35`.
- All new UI strings in English and Thai (`/home/claude/bin/addstr key "English" "ไทย"`).
- Tests: `/home/claude/bin/gt '<filter>'`. Stop the Gradle daemon (`./gradlew --stop`) if it runs out of memory.

## Review Focus

1. A very dark accent colour (e.g. `0xFF101010`) in ACCENT mode → glyphs must stay visible (accent lifted to a minimum brightness). Test in Task 2.
2. Screen widths not divisible by `5k` (e.g. 1080 × 1.12) → no black or transparent strip at the right/bottom edge. Test in Task 4.
3. Thumbnails (135 × 292) → must still show the scene's detail, not a 14-column blob. Test in Task 4.
4. Applying ASCII Live to **Lock only** → the 15-minute refresh must not overwrite the Home wallpaper. Test in Task 5.
5. Daytime live frame → overall dark enough for white icon labels (mean luminance of the frame < 70/255). Test in Task 2.

---

## File Structure

New package `app/src/main/java/com/xenoorigin/xelauncher/wallpaper/ascii/`:
- `AsciiGlyphs.kt` — glyph masks and `CellBuffer`.
- `AsciiRaster.kt` — cells → pixels.
- `AsciiField.kt` — noise, shared landscape parts, colour modes, `fill()`.
- `AsciiScenes.kt` — the six non-lake scene painters (keeps `AsciiField.kt` focused).
- `AsciiLayer.kt` — Compose layer for Home.

Modified: `settings/XeSettings.kt`, `wallpaper/WallpaperPreset.kt`, `wallpaper/WallpaperRenderer.kt`, `wallpaper/WallpaperController.kt`, `wallpaper/LiveSkyWallpaper.kt`, `wallpaper/WallpaperLayer.kt`, `XeApp.kt`, `settings/ui/WallpaperScreen.kt`, `res/values{,-th}/strings.xml`.

Tests in `app/src/test/java/com/xenoorigin/xelauncher/wallpaper/ascii/` plus additions to `wallpaper/WallpaperControllerTest.kt`.

---

### Task 1: Glyphs, cell buffer and raster

**Files:**
- Create: `wallpaper/ascii/AsciiGlyphs.kt`, `wallpaper/ascii/AsciiRaster.kt`
- Test: `wallpaper/ascii/AsciiRasterTest.kt` (plain JUnit, no Robolectric)

**Interfaces:**
- Produces:
  - `object AsciiGlyphs { const val CW = 5; const val CH = 8; const val RAIN = 8; const val SPARK = 9; val masks: Array<BooleanArray> /* 10 × 40, row-major y*5+x */; fun index(l: Float, jitter: Float): Int }` — masks 1–8 exactly as `backdrop.py` `_G` (with 5–7 the horizontal-line stacks), 9 = `*` (`(2,2),(1,3),(2,3),(3,3),(2,4),(0,1),(4,1),(0,5),(4,5)`).
  - `class CellBuffer(val cols: Int, val rows: Int, seed: Int) { val l: FloatArray; val color: IntArray; val glyph: IntArray /* -1 = from l */; val jitter: FloatArray; fun clear() }`
  - `object AsciiRaster { const val GROUND = 0xFF18181A.toInt(); fun rasterize(cells: CellBuffer, out: IntArray) }` — `out.size == cols*5 * rows*8`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun emptyCellsStayGround() {
    val b = CellBuffer(2, 1, 1); b.clear()
    val px = IntArray(10 * 8); AsciiRaster.rasterize(b, px)
    assertThat(px.toSet()).containsExactly(AsciiRaster.GROUND)
}
@Test fun brightCellStampsItsGlyphInItsOwnCellOnly() {
    val b = CellBuffer(2, 1, 1); b.clear()
    b.l[1] = 1f; b.color[1] = 0xFFFFFFFF.toInt(); b.jitter[1] = 0f
    val px = IntArray(10 * 8); AsciiRaster.rasterize(b, px)
    assertThat((0 until 8).flatMap { y -> (0 until 5).map { px[y * 10 + it] } }.toSet()).containsExactly(AsciiRaster.GROUND)
    val lit = (0 until 8).sumOf { y -> (5 until 10).count { px[y * 10 + it] == 0xFFFFFFFF.toInt() } }
    assertThat(lit).isEqualTo(20) // glyph 7 = four 5-px lines
}
@Test fun glyphOverrideWins() { /* l = 0, glyph[0] = RAIN → 6 lit pixels */ }
@Test fun indexIgnoresDimCells() { assertThat(AsciiGlyphs.index(0.08f, 0.3f)).isEqualTo(0); assertThat(AsciiGlyphs.index(1f, 0.3f)).isEqualTo(7) }
@Test fun jitterIsStablePerSeed() { assertThat(CellBuffer(4, 4, 7).jitter.toList()).isEqualTo(CellBuffer(4, 4, 7).jitter.toList()) }
```

- [ ] **Step 2: Run** `/home/claude/bin/gt '*AsciiRasterTest'` — Expected: compile FAIL (types missing).
- [ ] **Step 3: Implement** the two files; `rasterize` fills with `GROUND` then stamps each non-empty cell's mask in `color[i]`; no allocation inside `rasterize`.
- [ ] **Step 4: Run** the same command — Expected: `tests=5 failed=0`.
- [ ] **Step 5: Commit** `feat(ascii): glyphs and raster`.

---

### Task 2: Field — shared landscape, Lake scene, live look, colour modes

**Files:**
- Create: `wallpaper/ascii/AsciiField.kt`
- Modify: `settings/XeSettings.kt` (add `@Serializable enum class AsciiColor { MONO, SKY, ACCENT }` next to `IconGlass`; field `val asciiColor: AsciiColor = AsciiColor.MONO`)
- Test: `wallpaper/ascii/AsciiFieldTest.kt` (plain JUnit)

**Interfaces:**
- Consumes: `CellBuffer`, `SkyLook`/`skyAt` from `wallpaper/Sky.kt`.
- Produces:
  - `enum class AsciiScene { LAKE, RAIN, DAWN, PINES, CITY, SPACE, LIGHTHOUSE }`
  - `class AsciiField(val cols: Int, val rows: Int, seed: Int = 11) { val cells: CellBuffer; val horizonRow: Int /* ≈ 0.58·rows */; val groundRow: Int /* ≈ 0.86·rows */; fun fill(scene: AsciiScene, t: Float, look: SkyLook? = null, color: AsciiColor = AsciiColor.MONO, accent: Long = 0xFFFFFFFF) }`
  - `look == null` → the scene's fixed default; for LAKE that is night (`skyAt(0)`).
  - internal helpers used by Task 3: `sky(look)`, `orb(x, y, r, sun: Boolean)`, `clouds(t, cover)`, `stars(t, amount)`, `ridges()`, `water(t, orbX)`, `shore()`, `tint(i, argb, strength)`, `finishColors(mode, look, accent)`.
- Algorithms: port `backdrop.py` `Backdrop.render` cell maths using **fractions of cols/rows** (moon at x = 0.24·cols, y = 0.2·horizonRow, radius 0.11·cols in x and 0.11·cols·5/8 in y; far ridge height `3 + 15·r1^1.3` scaled by `rows/45`). Value noise and fBm precomputed once in the constructor with `java.util.Random(seed)`. Live look: `sunUp` → sun disc (no craters, halo ×1.4, tint `0xFFFFE0A0`), else moon (tint `0xFFD6E2FF` at strength 0.25 on the halo); stars amount = `look.stars`; sky `L = 0.035 + 0.05·y/horizon + 0.3·luminance(look.mid)` capped at 0.35. Colour modes: MONO = grey; SKY = mix of `look.top→look.bottom` by row (water uses the mirrored row, ground uses `look.bottom` darkened ×0.5) scaled by `0.35 + 0.65·L`; ACCENT = accent lifted so its max channel ≥ 160, scaled the same. Highlight tints are applied after the mode colour, so they survive every mode.

- [ ] **Step 1: Write failing tests**

```kotlin
private fun field() = AsciiField(72, 98)
@Test fun sameSeedSameCells() { val a = field().apply { fill(AsciiScene.LAKE, 1f) }; val b = field().apply { fill(AsciiScene.LAKE, 1f) }; assertThat(a.cells.l.toList()).isEqualTo(b.cells.l.toList()) }
@Test fun moonIsBrighterThanTheSkyAroundIt() { /* mean l in moon disc ≥ 0.6, mean l of sky row 2 far right ≤ 0.2 */ }
@Test fun nightHasStarsDayHasNone() { /* count lit cells above ridge excluding disc and clouds: night (skyAt(0)) > 15, noon (skyAt(12*60)) == 0 stars-only cells */ }
@Test fun dayUsesSunNightUsesMoon() { /* noon: disc cells carry tint 0xFFFFE0A0; midnight: none do */ }
@Test fun daytimeStaysDarkEnoughForLabels() { /* rasterize noon frame, mean luminance of all pixels < 70 */ }   // Review Focus 5
@Test fun monoLeavesPlainCellsGrey() { /* every lit cell without highlight has r == g == b */ }
@Test fun accentTintsPlainCells() { /* accent 0xFF2F8BFF: a lit water cell has b > r */ }
@Test fun darkAccentStillVisible() { /* accent 0xFF101010: max channel of a bright cell ≥ 120 */ }   // Review Focus 1
@Test fun skyModeFollowsTheLook() { /* SKY at dusk (skyAt(sunset+20)): a sky cell is warmer (r > b) than at midnight */ }
@Test fun waterShimmersOverTime() { /* l of water rows differs between t = 0 and t = 1 */ }
@Test fun landscapeGridWorks() { val f = AsciiField(96, 30); f.fill(AsciiScene.LAKE, 0f); assertThat(f.groundRow).isIn(Range.closed(20, 29)); assertThat(f.cells.l.count { it > .5f }).isGreaterThan(10) }
@Test fun fillIsFast() { val f = field(); val px = IntArray(360 * 784); repeat(3) { f.fill(AsciiScene.LAKE, it.toFloat()); AsciiRaster.rasterize(f.cells, px) }; val t0 = System.nanoTime(); repeat(10) { f.fill(AsciiScene.LAKE, it.toFloat()); AsciiRaster.rasterize(f.cells, px) }; assertThat((System.nanoTime() - t0) / 10 / 1e6).isLessThan(15.0) }
```

- [ ] **Step 2: Run** `/home/claude/bin/gt '*AsciiFieldTest'` — Expected: compile FAIL.
- [ ] **Step 3: Implement** `AsciiField` (LAKE only in `fill`; other scenes throw `NotImplementedError` until Task 3) and the `AsciiColor` setting.
- [ ] **Step 4: Run** — Expected: all 12 pass. Also run `/home/claude/bin/gt '*Settings*'` — Expected: no failures (serialization of the new field).
- [ ] **Step 5: Commit** `feat(ascii): field, lake scene, live look and colour modes`.

---

### Task 3: The six other scenes

**Files:**
- Create: `wallpaper/ascii/AsciiScenes.kt` (extension functions on `AsciiField`, one per scene)
- Modify: `AsciiField.fill` dispatch
- Test: `wallpaper/ascii/AsciiScenesTest.kt`

**Interfaces:**
- Consumes: Task 2 helpers.
- Produces: `fill(scene, …)` works for every `AsciiScene`.
- Scene facts the tests pin (fractions of the grid):
  - RAIN: cloud cover 1, no disc; ≥ 3% of cells have `glyph == AsciiGlyphs.RAIN`; water ripple cells.
  - DAWN: sun disc centre at `horizonRow − 2`, x = 0.62·cols, tint `0xFFFFC27A`; glow band rows `horizonRow−6 .. horizonRow` tinted `0xFFFF9E7A` at strength ≤ 0.5; no stars.
  - PINES: ≥ 6 pine silhouettes (stacked triangles, base on `groundRow`, heights 0.15–0.35·rows, `L ≤ 0.12`); fireflies = 8–14 cells with `glyph == SPARK`, tint `0xFFC8FF8C`; moon; no water.
  - CITY: skyline of 10–16 rectangles between 0.45 and 0.7·rows; lit windows tint `0xFFFFD166` on 25–35% of window cells; moon; river reflection below `groundRow`.
  - SPACE: no ground; dense stars (≥ 4% of cells); planet disc centre (0.62·cols, 0.42·rows) radius 0.3·cols shaded left-dark; ring ellipse tilted 20° tinted `0xFFC27BFF`; one meteor streak of ≥ 6 cells diagonal.
  - LIGHTHOUSE: sea rows from `horizonRow`; cliff right 35% of the width; tower at x = 0.74·cols; beam cone from the lamp toward the left, tint `0xFFFFD166`, covering ≥ 40 cells; stars.

- [ ] **Step 1: Write failing tests** — one per scene asserting the facts above, e.g.

```kotlin
@Test fun rainHasStreaksAndNoMoon() { val f = AsciiField(72, 98); f.fill(AsciiScene.RAIN, 1f); assertThat(f.cells.glyph.count { it == AsciiGlyphs.RAIN }).isAtLeast(72 * 98 * 3 / 100); assertThat(f.cells.color.count { it == 0xFFD6E2FF.toInt() }).isEqualTo(0) }
@Test fun cityHasLitWindows() { /* count cells tinted 0xFFFFD166 between 0.45 and 0.7·rows ≥ 20 */ }
@Test fun everySceneDrawsSomethingOnBothGridShapes() { for (s in AsciiScene.entries) for ((c, r) in listOf(72 to 98, 96 to 30)) { val f = AsciiField(c, r); f.fill(s, 0f); assertWithMessage("$s $c×$r").that(f.cells.l.count { it > .3f }).isGreaterThan(c * r / 50) } }
```

- [ ] **Step 2: Run** `/home/claude/bin/gt '*AsciiScenesTest'` — Expected: FAIL (`NotImplementedError`).
- [ ] **Step 3: Implement** the six painters.
- [ ] **Step 4: Run** — Expected: all pass; re-run `*AsciiFieldTest` — still green.
- [ ] **Step 5: Commit** `feat(ascii): rain, dawn, pines, city, space and lighthouse scenes`.

---

### Task 4: Presets, renderer and contact sheet

**Files:**
- Modify: `wallpaper/WallpaperPreset.kt`, `wallpaper/WallpaperRenderer.kt`
- Test: `wallpaper/ascii/AsciiRendererTest.kt` (Robolectric, `@GraphicsMode(NATIVE)`, `@Config(sdk = [34])`)

**Interfaces:**
- Produces:
  - `data class WallpaperPreset.Ascii(override val id: String, override val name: String, val scene: AsciiScene, val live: Boolean = false, val minute: Int? = null, val color: AsciiColor = AsciiColor.MONO, val accent: Long = 0xFFFFFFFF) : WallpaperPreset`
  - `const val ASCII_LIVE_ID = "ascii_live"`; `val AsciiCollection: List<WallpaperPreset.Ascii>` (8, live first, names "ASCII Live", "Lake", "Rain", "Dawn", "Pines", "City", "Space", "Lighthouse"); `allWallpapers() = XeCollection + AsciiCollection + SecretCollection`.
  - `fun WallpaperPreset.Ascii.withColors(s: XeSettings) = copy(color = s.asciiColor, accent = s.accent)`
  - `fun isTimeWallpaper(id: String): Boolean = id == LIVE_SKY_ID || id == ASCII_LIVE_ID`
  - `fun asciiPixelScale(drawnWidth: Float): Int = max(1, round(drawnWidth / 360f))`
  - `WallpaperRenderer.ascii(p: WallpaperPreset.Ascii, w: Int, h: Int, t: Float = 0f): Bitmap` and the `render` branch. Live presets use `skyAt(p.minute ?: minuteNow(), sunToday())`. Raster is centred; leftover edge pixels filled with `GROUND`. Width < 360 → render at 360 × (h·360/w) with k = 1, then `Bitmap.createScaledBitmap(…, true)`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun collectionHasEightWithLiveFirst() { assertThat(AsciiCollection.map { it.id }).containsExactly("ascii_live","ascii_lake","ascii_rain","ascii_dawn","ascii_pines","ascii_city","ascii_space","ascii_lighthouse").inOrder(); assertThat(allWallpapers().byId("ascii_city")).isNotNull() }
@Test fun rendersAtTheRequestedSize() { val b = WallpaperRenderer.render(AsciiCollection[1], 1080, 2340); assertThat(b.width to b.height).isEqualTo(1080 to 2340) }
@Test fun pixelsAreWholeBlocks() { /* 1080 wide → k = 3: for 200 random lit pixels, the pixel's 3×3 block (aligned to the raster origin) is one colour */ }
@Test fun noEmptyStripAtTheEdges() { val b = WallpaperRenderer.render(AsciiCollection[1], 1209, 2620); for (y in listOf(0, 1300, 2619)) for (x in listOf(0, 1208)) assertThat(Color.alpha(b.getPixel(x, y))).isEqualTo(255); assertThat(b.getPixel(1208, 2619)).isNotEqualTo(0xFF000000.toInt()) }   // Review Focus 2
@Test fun thumbnailsKeepDetail() { val b = WallpaperRenderer.render(AsciiCollection[5], 135, 292); assertThat((0 until 135).map { b.getPixel(it, 150) }.toSet().size).isAtLeast(12) }   // Review Focus 3
@Test fun colourComesFromThePreset() { /* lake MONO vs ACCENT(0xFF2F8BFF): some pixel differs, ACCENT pixel has blue > red */ }
@Test fun contactSheet() { /* all 8 in MONO + lake SKY + lake ACCENT at 270×585, saved to "$SHOT_DIR/ascii_wallpapers.png" */ }
```

- [ ] **Step 2: Run** `/home/claude/bin/gt '*AsciiRendererTest'` — Expected: compile FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — Expected: all pass. Open `app/src/test/screenshots/ascii_wallpapers.png` with Read and check every scene is recognisable and dark enough; fix and re-run if not. Run `/home/claude/bin/gt '*Wallpaper*'` — Expected: existing tests still green (WallpaperArtTest iterates `XeCollection` only).
- [ ] **Step 5: Commit** `feat(ascii): presets, renderer and contact sheet`.

---

### Task 5: Time-following refresh for ASCII Live (and the Lock-only fix)

**Files:**
- Modify: `settings/XeSettings.kt` (`val liveOnHome: Boolean = true`), `wallpaper/WallpaperController.kt`, `wallpaper/LiveSkyWallpaper.kt` (`SkyMirrorJob`, `watchScreenOn`), `XeApp.kt` (schedule + refresh loop use `isTimeWallpaper`)
- Test: additions to `wallpaper/WallpaperControllerTest.kt`

**Interfaces:**
- Consumes: `isTimeWallpaper`, `ASCII_LIVE_ID`, `withColors`.
- Produces:
  - `WallpaperController.refreshLiveSky(minute)` → renders `timePreset(settings, minute)` (Sky or ASCII Live with colours); writes Home only if `liveOnHome`; returns early unless `isTimeWallpaper(wallpaperId)`.
  - `refreshLockSky(minute)` → same preset to Lock.
  - `apply()` sets `liveOnHome = target != LOCK` and `liveSkyLock = isTimeWallpaper(preset.id) && target != HOME` for time wallpapers; existing behaviour for others; ASCII presets are rendered via `withColors(settings)`.
  - `suspend fun reapplyAscii()` — if the current id is an ASCII preset: re-render with current colours to Home if `liveOnHome`, to Lock if `liveSkyLock` (stills: Home only, matching today's file).
  - `SkyMirrorJob`, screen-on receiver and `LiveSkyWallpaper.schedule` call sites use `isTimeWallpaper(s.wallpaperId)`; `shouldMirrorHome` gets `liveOnHome` too: `shouldMirrorHome(chosen, liveWallpaperActive, onHome) = chosen && onHome && !liveWallpaperActive` (the live-wallpaper check stays Sky-only: pass `false` for ASCII).

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun asciiLive_refreshesOnTheQuarterHour() { /* apply ascii_live(minute=12*60) HOME; refresh 12*60+5 → no new set; refresh 23*60 → +1; noon pixel != night pixel */ }
@Test fun asciiStill_neverRefreshes() { /* apply ascii_city HOME; refresh 23*60 → count unchanged */ }
@Test fun lockOnly_doesNotOverwriteHome() { /* apply xe_blue HOME, then ascii_live LOCK; refreshLiveSky(23*60) → current.png unchanged (same bytes) and settings.liveOnHome == false */ }   // Review Focus 4
@Test fun liveSkyLockOnly_sameFix() { /* same with WallpaperPreset.Sky */ }
@Test fun reapplyAscii_usesNewColour() { /* apply ascii_lake HOME (MONO); settings asciiColor = ACCENT, accent blue; reapplyAscii(); some pixel of current has blue > red */ }
@Test fun mirrorDecision() { assertThat(shouldMirrorHome(true, false, true)).isTrue(); assertThat(shouldMirrorHome(true, false, false)).isFalse(); assertThat(shouldMirrorHome(true, true, true)).isFalse() }
```

- [ ] **Step 2: Run** `/home/claude/bin/gt '*WallpaperControllerTest'` — Expected: new tests FAIL.
- [ ] **Step 3: Implement.** Update `LiveSkyWallpaperTest` call sites of `shouldMirrorHome` to the new signature.
- [ ] **Step 4: Run** `/home/claude/bin/gt '*Wallpaper*'` and `/home/claude/bin/gt '*LiveSky*'` — Expected: all pass.
- [ ] **Step 5: Commit** `feat(ascii): ASCII Live follows the time; time wallpapers on Lock no longer overwrite Home`.

---

### Task 6: Live ASCII on Home

**Files:**
- Create: `wallpaper/ascii/AsciiLayer.kt`
- Modify: `wallpaper/WallpaperLayer.kt` (new param `ascii: WallpaperPreset.Ascii? = null`), `XeApp.kt` (pass `allWallpapers().byId(settings.wallpaperId) as? WallpaperPreset.Ascii` with `withColors(settings)`)
- Test: `wallpaper/ascii/AsciiLayerTest.kt` (Robolectric + `createAndroidComposeRule`, View.draw pixel reads as in `LiquidGlassTest`)

**Interfaces:**
- Consumes: `AsciiField`, `AsciiRaster`, `asciiPixelScale`, `skyLoopRuns`, `rememberXeVisible`, `WALL_SCALE`.
- Produces: `@Composable fun AsciiLayer(preset: WallpaperPreset.Ascii, parallaxPx: () -> Offset, animate: Boolean, modifier: Modifier = Modifier)`
  - Size: drawn area = layout × `WALL_SCALE`; `k = asciiPixelScale(width × WALL_SCALE)`; cols/rows = ceil(drawn / (5k, 8k)); one `Bitmap` + `IntArray` per size (remembered), raster drawn with `FilterQuality.None` at scale k, centred; translation = `parallaxPx()` rounded to whole pixels; **no** extra `scaleX/Y`.
  - Redraw loop: when `preset.live && skyLoopRuns(animate, state)`: every 125 ms `t += 0.125`, minute refreshed every 15 s; otherwise one frame (and for live presets one per minute).
  - `WallpaperLayer` draws `AsciiLayer` instead of the saved bitmap whenever `ascii != null` (live and stills alike), and stays the haze source.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun drawsCrispBlocks() { /* host 360×780 dp box at density 1; after idle, sample 50 lit pixels: each equals its right and lower neighbour inside the same k-block */ }
@Test fun stillPresetDrawsOnce() { /* count of fill() calls via a test hook `AsciiLayerProbe.frames` stays 1 after advancing the clock 2 s */ }
@Test fun livePresetAnimatesOnlyWhenAllowed() { /* animate=true → frames grows after 1 s; animate=false → stays 1 */ }
@Test fun parallaxMovesByWholePixels() { /* parallax 7.4 px → drawn content shifted by 7 px (compare column of pixels) */ }
```

- [ ] **Step 2: Run** `/home/claude/bin/gt '*AsciiLayerTest'` — Expected: compile FAIL.
- [ ] **Step 3: Implement** (`internal object AsciiLayerProbe { @Volatile var frames = 0 }` incremented per fill, reset in tests).
- [ ] **Step 4: Run** — Expected: all pass; `/home/claude/bin/gt '*HomeScreenshotTest*'` still green.
- [ ] **Step 5: Commit** `feat(ascii): live and still ASCII drawn crisp on Home`.

---

### Task 7: Wallpaper screen — ASCII group, colour choice, editor

**Files:**
- Modify: `settings/ui/WallpaperScreen.kt`, `res/values/strings.xml`, `res/values-th/strings.xml`
- Test: `settings/AsciiWallpaperScreenTest.kt` (Robolectric Compose test, pattern of `WallpaperSheetTest`)

**Interfaces:**
- Consumes: `AsciiCollection`, `withColors`, `reapplyAscii()`, `XeSettings.asciiColor`.
- Produces UI:
  - Group "ASCII" after the Xe collection: a 3-chip colour row (`testTag("ascii_color_MONO|SKY|ACCENT")`) that saves `asciiColor` immediately and calls `graph.wallpaper.reapplyAscii()`; then `ThumbGrid(AsciiCollection.map { it.withColors(s) })`.
  - Editor for an `Ascii` preset: caption (`ascii_live_desc` for live, `ascii_still_desc` otherwise) and the same 3-chip row (`testTag("editor_ascii_color_…")`) changing only the previewed preset's `color`; on Apply, `asciiColor` is saved to the chosen colour before `apply()`. Shuffle, glow and zoom rows are not shown for ASCII; dim and blur stay.
  - Strings (EN / TH): `ascii_group` "ASCII" / "ASCII"; `ascii_color` "ASCII colour" / "สีของ ASCII"; `ascii_mono` "Mono" / "ขาวเทา"; `ascii_sky` "Sky" / "ตามท้องฟ้า"; `ascii_accent` "Accent" / "ตามสีเน้น"; `ascii_live_desc` "Moves and follows the time of day: sun by day, moon and stars at night." / "ขยับได้และเปลี่ยนตามเวลาจริง กลางวันมีพระอาทิตย์ กลางคืนมีพระจันทร์และดาว"; `ascii_still_desc` "A still scene drawn in text characters." / "ฉากภาพนิ่งที่วาดด้วยตัวอักษร".

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun asciiGroupShowsEightThumbnails() { /* onAllNodesWithTag("thumb_ascii_…") count == 8 (add testTag("thumb_${p.id}") in ThumbGrid) */ }
@Test fun colourChipSavesImmediately() { /* click ascii_color_ACCENT → settings.asciiColor == ACCENT */ }
@Test fun editorColourOnlyPreviewsUntilApply() { /* open ascii_lake, click editor_ascii_color_SKY → settings still MONO; set wallpaper → Home → settings SKY */ }
```

- [ ] **Step 2: Run** `/home/claude/bin/gt '*AsciiWallpaperScreenTest'` — Expected: FAIL.
- [ ] **Step 3: Implement**; add strings with `addstr`.
- [ ] **Step 4: Run** — Expected: pass; then the full suite `/home/claude/bin/gt` — Expected: `failed=0` (LocalisationTest included).
- [ ] **Step 5: Commit** `feat(ascii): ASCII group, colour choice and editor in the wallpaper screen`.
