# Xe Launcher 1.0.14 "Polish" — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One release that fixes the category editor and live sky, makes widgets free-sized and easy to arrange, adds glass refraction and four signature touches, ranks apps by time spent, and gives update alerts a chime.

**Architecture:** All changes are in `:app`. New pure-logic pieces (sun times, widget size math, usage ranking, refraction offsets) live in small files with JVM/Robolectric tests; UI changes are pinned by Compose tests and Roborazzi screenshots. Per-category accent/sort are stored in `XeSettings` maps (no DB migration).

**Tech Stack:** Kotlin, Compose, Haze, AGSL RuntimeShader (API 33+), UsageStatsManager, WallpaperService, JobScheduler, Robolectric/Roborazzi.

**Spec:** `docs/superpowers/specs/2026-11-22-xe-polish-release-design.md`

## Global Constraints

- minSdk 29, target 36. Lens refraction only on API ≥ 33, else Light.
- Home must stay smooth at 90 Hz on the A16: no per-frame allocations in icon drawing; Light is the default icon glass.
- No location permission; latitude comes from the time zone (fallback 13.75°N, 100.5°E).
- Existing layouts, categories and settings carry over; new settings fields have defaults that reproduce today's behaviour, except icon glass = LIGHT and the four touches = on.
- Thai + English strings via `/home/claude/bin/addstr key "English" "ไทย"`.
- Release `v1.0.14`, signed with the existing key; Xe Studio APK rebuilt at the same version.

## Review Focus

1. Usage access never granted or revoked later → ranking falls back to launch counts, no crash, no empty "Used longest". Test in Task 3.
2. A widget resized/moved onto occupied cells or past the grid edge → refused, shown red, original kept. Test in Task 9.
3. Time zone with no sensible latitude, or polar day/night (sunrise formula NaN) → fallback 06:00/18:00. Test in Task 4.
4. Lens shader unavailable (API < 33 or compile failure) → Light used silently. Test in Task 10.
5. Live sky wallpaper service hidden / battery saver → draws once per visibility change, no animation loop. Test in Task 6.

---

### Task 1: Update alert chime
**Files:** create `app/src/main/res/raw/xe_chime.ogg` (synthesised with python+ffmpeg: three glass partials ~1.6/2.4/3.9 kHz, exp decay, 0.8 s, mono, ≤ 30 KB); modify `update/UpdateNotifier.kt`; test `update/UpdateNotifierTest.kt`.
**Produces:** `UpdateNotifier.CHANNEL_ID = "updates_alert_v2"`; `ensureChannel(ctx)` creates it with `android.resource://<pkg>/raw/xe_chime` and `AudioAttributes.USAGE_NOTIFICATION`, and deletes legacy `"updates_alert"`.
- [ ] Test `channelUsesXeChime` (sound uri ends with `/raw/xe_chime`… or resource id), `legacyChannelIsRemoved` → RED → implement → GREEN → commit.

### Task 2: Category editor fixes + accent & sort per category
**Files:** `design/Reorderable.kt`, `drawer/CategoryEditor.kt`, `settings/XeSettings.kt`, `drawer/AppDrawer.kt` / `drawer/CategoryBar.kt` (use accent), `drawer/DrawerState.kt` (sort inside a category); tests `design/ReorderableTest.kt`, `drawer/CategoryEditorTest.kt`.
**Produces:** `XeSettings.categoryAccent: Map<String, Long>` (key = category id as string), `XeSettings.categorySort: Map<String, CategorySort>`; `@Serializable enum class CategorySort { FOLLOW, ALPHABETICAL, TIME, NEWEST }`; `fun sortForCategory(id: Long, s: XeSettings): DrawerSort`.
- Rows: titles get `maxLines = 2, overflow = Ellipsis`, row height 76.dp; non-dragged rows animate their offset with a spring when order changes; dragging state is cleared when the item list changes or the gesture is cancelled; swipe-left on a row toggles hidden (except OTHER).
- Form sheet gains accent swatches (8 + none) and a sort picker.
- [ ] Tests: `longThaiNameIsNotCutMidWord` (text node has 2-line ellipsis config / full text present), `dragStateClearsWhenListChanges`, `swipeLeftHidesCategory`, `accentAndSortAreSaved`, `categorySortFollowsDrawerByDefault` → RED → GREEN → screenshot `category_editor` → commit.

### Task 3: Rank by time spent
**Files:** create `apps/UsageTime.kt`, `apps/UsageAccess.kt` (permission check + intent `Settings.ACTION_USAGE_ACCESS_SETTINGS`); modify manifest (`PACKAGE_USAGE_STATS` with `tools:ignore="ProtectedPermissions"`), `XeApp.kt` (`useCounts` → `usageScore`), Bento label `bento_most_used` → "Used longest", DrawerSort.MOST_USED label, settings card in Settings › Apps; tests `apps/UsageTimeTest.kt`.
**Produces:** `class UsageTime(source: () -> Map<String, Long>?)` with `fun score(key: AppKey, launches: Int): Long` (ms in last 7 days when available, else launches); `fun hasUsageAccess(ctx): Boolean`.
- [ ] Tests: `usesForegroundTimeWhenAllowed`, `fallsBackToLaunchCountsWithoutAccess` (source returns null), `accessRevokedMidSessionFallsBack` → RED → GREEN → commit.

### Task 4: Sun times and a sky that follows them
**Files:** `wallpaper/Sky.kt`, create `wallpaper/SunTimes.kt`; tests `wallpaper/SkyTest.kt`, `wallpaper/SunTimesTest.kt`.
**Produces:** `data class SunTimes(val sunrise: Int, val sunset: Int)` (minutes of day); `fun sunTimes(date: LocalDate, lat: Double, lon: Double, utcOffsetMin: Int): SunTimes` (NOAA simplified; NaN/polar → 360/1080); `fun latLonFor(zone: ZoneId): Pair<Double, Double>` (table of common zones, fallback Bangkok); `fun skyAt(minuteOfDay: Int, sun: SunTimes = SunTimes(360, 1100)): SkyLook` with keyframes placed relative to sunrise/sunset (night, pre-dawn −60, dawn −20, sunrise, morning +70, noon, afternoon sunset−110, golden −30, sunset, dusk +30, night +80).
- [ ] Tests: `bangkokSunriseIsAroundSixFifteenInNovember` (2026-11-22 → 6:10–6:25, sunset 17:40–17:55), `polarNightFallsBack`, `skyIsDarkAfterDusk` (sunset+90 → stars > .7), `skyIsBlueAtNoon`, `oneMinuteChangesAreSmall` (max channel delta between consecutive minutes ≤ 6) → RED → GREEN → commit.

### Task 5: Live sky drawn live inside Xe
**Files:** create `wallpaper/LiveSkyLayer.kt`; modify `wallpaper/WallpaperLayer.kt` (when wallpaperId == LIVE_SKY_ID draw LiveSkyLayer instead of the bitmap), `WallpaperController.kt` (mirror refresh: every 15 min of sky time, HOME target; lock handled in Task 6), `XeApp.kt` loop uses minute ticks.
**Produces:** `@Composable fun LiveSkyLayer(sun: SunTimes, minute: () -> Int, animate: Boolean, modifier)` — gradient + orb from `skyAt`, twinkling stars (deterministic positions, alpha sine per star), 3 slow cloud bands (drift ≤ 4 dp/s); `animate = false` when AnimationSpeed.OFF or battery saver.
- [ ] Tests: `liveSkyScreenshotNight`, `liveSkyScreenshotDay` (Roborazzi), `mirrorRefreshesAtMostEvery15Minutes` → RED → GREEN → commit.

### Task 6: "Xe Live Sky" system wallpaper, home job, lock on screen-on
**Files:** create `wallpaper/LiveSkyWallpaperService.kt`, `res/xml/live_sky_wallpaper.xml`, `wallpaper/SkyMirrorJob.kt` (JobService, 15 min periodic, HOME only, skipped when our live wallpaper is active), `wallpaper/ScreenOnSky.kt` (dynamic `ACTION_SCREEN_ON` receiver registered in `XeApplication`; sets LOCK bitmap only if sky bucket changed since last lock write); manifest entries; "Also use on the phone's wallpaper" card in wallpaper picker after choosing Live Sky (`WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER` with our component).
**Produces:** `fun lockNeedsUpdate(lastWrittenBucket: Int, now: Int): Boolean`; `object LiveSkyWallpaper { fun isActive(ctx): Boolean; fun pickerIntent(ctx): Intent }`.
- Engine draws `skyAt(now)` on visibility change and once a minute only while visible (Handler postDelayed, removed when invisible).
- [ ] Tests: `engineDoesNotTickWhileInvisible` (Robolectric engine), `lockUpdatesOnlyWhenBucketChanged`, `jobSkipsWhenLiveWallpaperActive`, `pickerIntentNamesOurService` → RED → GREEN → commit.

### Task 7: Prettier wallpapers + Liquid set
**Files:** `wallpaper/WallpaperRenderer.kt` (light leaks, bokeh orbs, dithered grain to kill banding), `wallpaper/WallpaperPreset.kt` (add `WallpaperPreset.Liquid(id, name, colors, seed, style: LiquidStyle {RIBBONS, AURORA, CHROME})` and ≥ 6 Liquid presets), themes list uses them, `WallpaperController.restore()` redraws built-in presets once when `settings.wallpaperArtVersion < 2`.
- [ ] Tests: `everyPresetRendersWithoutBanding` (adjacent-row max delta small on a gradient strip), `liquidPresetsArePresentAndDistinct`, `builtInWallpaperRedrawnOnceAfterUpdate`, `importedPhotoNotRedrawn` + screenshots of 4 presets → RED → GREEN → commit; refresh `website/assets/wall/*.webp` not required.

### Task 8: Responsive glass widgets
**Files:** create `widgets/WidgetSizing.kt`; modify `widgets/GlassWidgets.kt` (each kind reads `LocalWidgetSpan` and picks a layout), `GlassWidgetKind` gets `minX, minY, maxX = 4, maxY = 4` and `presets: List<Pair<Int,Int>>`.
**Produces:** `data class Span(val x: Int, val y: Int)`; `val LocalWidgetSpan = compositionLocalOf { Span(2, 2) }`; `fun GlassWidgetKind.clamp(s: Span): Span`; `fun GlassWidgetKind.presetSpans(cols: Int): List<Span>` (S/M/L/XL, deduped, within grid).
- [ ] Tests: `clockMinimumIsTwoByOne`, `presetsFitGrid`, screenshots of CLOCK/BATTERY/MONTH at 1×1, 2×1, 2×2, 4×2, 4×4 (where allowed) → RED → GREEN → commit.

### Task 9: Direct widget editing (move, corner handles, presets)
**Files:** create `home/WidgetEditOverlay.kt`, `home/WidgetEditMath.kt`; modify `home/HomeRepository.kt` (`placeWidget`), `home/HomeScreen.kt` (long-press widget → overlay), `XeApp.kt` (remove `WidgetResizeSheet` use; menu "Resize" opens overlay), delete `WidgetResizeSheet`.
**Produces:** `fun proposePlacement(cell: WidgetCell, dxCells: Int, dyCells: Int, dwCells: Int, dhCells: Int, cols: Int, rows: Int, min: Span, max: Span): Placement` (`data class Placement(x, y, w, h, val fits: Boolean)`); `suspend fun HomeRepository.placeWidget(id: Long, x: Int, y: Int, w: Int, h: Int): Boolean` (transaction; false leaves it unchanged).
- Overlay: lifted widget with wobble, ghost at snapped cell, 4 corner handles (48 dp touch), live "W×H" chip, red tint when `fits == false`, glass toolbar with preset chips + Remove + Done; haptic tick on each snap.
- [ ] Tests: `placementClampsToGrid`, `placementOntoOccupiedCellDoesNotFit` (repository), `placeWidgetFailureKeepsOriginal`, `handleDragResizesAndShowsLabel` (Compose), `presetChipResizes` → RED → GREEN → screenshot → commit.

### Task 10: Icon glass — Off / Light / Lens
**Files:** `settings/XeSettings.kt` (`iconGlass: IconGlass = LIGHT`), create `design/IconRefraction.kt`, modify `icons/AppIcon.kt`, `home/HomeScreen.kt`/`drawer/AppDrawer.kt` (provide scroll velocity/offset via `LocalScrollFlow`), Appearance settings row.
**Produces:** `enum class IconGlass { OFF, LIGHT, LENS }`; `object ScrollFlow { val offset: MutableFloatState; val velocity: MutableFloatState }` (draw-only reads); `fun highlightShift(offsetPx: Float, tiltX: Float): Float`; `fun Modifier.iconLens(strength: () -> Float): Modifier` (RuntimeShader on API 33+, returns Modifier unchanged otherwise or on shader failure).
- [ ] Tests: `highlightMovesWithScroll`, `lensFallsBackBelowApi33` (`@Config(sdk=[31])` → no renderEffect), `iconGlassOffDrawsNoSheen` → RED → GREEN → commit.

### Task 11: Liquid dock
**Files:** `home/HomeScreen.kt` (`Dock`), create `home/DockMagnify.kt`.
**Produces:** `fun dockScale(fingerX: Float?, slotCenterX: Float, slotWidth: Float): Float` (1 at distance ≥ 1.8 slots, up to 1.45 under finger, cosine falloff); finger down on dock background + horizontal slide magnifies; lift over an icon launches it; setting `liquidDock = true`.
- [ ] Tests: `scaleIsMaxUnderFinger`, `scaleFallsOffWithDistance`, `liftOverIconLaunchesIt` (Compose) → RED → GREEN → commit.

### Task 12: Depth parallax
**Files:** `wallpaper/WallpaperLayer.kt`, `home/HomeScreen.kt`; setting `depthParallax = true`.
**Produces:** `fun parallaxOffset(pageFraction: Float, pageCount: Int, tilt: Offset, widthPx: Float): Offset` (pages shift wallpaper ≤ 8% width total, tilt ≤ 12 dp), wallpaper drawn 1.1× to allow shift.
- [ ] Tests: `firstAndLastPageSpanAtMostEightPercent`, `tiltIsBounded` → RED → GREEN → commit.

### Task 13: Glass bubble launch
**Files:** `XeApp.kt` (`launch`), create `home/LaunchBubble.kt`; setting `glassLaunch = true`.
**Produces:** overlay bubble that grows from icon bounds (180 ms spring) while `ActivityOptions.makeScaleUpAnimation` starts the app; skipped when AnimationSpeed.OFF.
- [ ] Tests: `launchPassesScaleUpOptions` (captured opts non-null when on, null when off) → RED → GREEN → commit.

### Task 14: Time-of-day rim tint
**Files:** `design/Glass.kt` (RimBrush tint), `XeApp.kt` provides `LocalRimTint`; setting `timeTint = true`.
**Produces:** `fun rimTintAt(minute: Int, sun: SunTimes): Color` (warm amber ±40 min around sunset, cool blue at night, neutral by day, alpha ≤ .35).
- [ ] Tests: `rimTintWarmAtSunset`, `rimTintNeutralAtNoon`, `rimTintCoolAtMidnight` → RED → GREEN → commit.

### Task 15: UI consistency pass + settings for the touches
**Files:** `design/Components.kt` (GlassSheet uses `XeMotion` settle spring for enter/exit; GlassPillButton/GlassIconButton get pressSquish), Settings › Appearance new section "Effects" with Icon glass picker and toggles (liquid dock, depth, glass launch, time tint); screenshots of Home, drawer, settings refreshed and reviewed.
- [ ] Tests: `effectsTogglesPersist`, updated screenshots reviewed by eye; full suite → commit.

### Task 16: Release v1.0.14
- [ ] `./gradlew --stop`; run `:app`, `:studio`, `:studio-core` tests separately → green; `:app:lintDebug :studio:lintDebug` → clean.
- [ ] Build both release APKs with `GITHUB_RUN_NUMBER=14` + signing env; verify cert SHA-256 `ea0e6430…a09c68` and versionName 1.0.14.
- [ ] Copy APKs to `release/`, `release/version.txt` = 1.0.14, Thai+English `release/notes.md`, NEWS entry in `website/app.js`; `node` syntax check + `website/check.py`.
- [ ] Final whole-branch review (fresh reviewer) before pushing; fix Critical/Important with tests.
- [ ] Commit, `git push origin HEAD:main`, confirm Publish release succeeded with both assets, republish website files to the artifact, give the owner both website links.
