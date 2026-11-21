# Xe Studio Phase 1 — Icon themes end to end — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the first Xe Studio APK: monochrome app with the pen icon, local projects, the icon-theme editor (whole set + per-icon), and one-tap "Use in Xe" that Xe Launcher picks up — released together with an Xe update.

**Architecture:** The xe-launcher repo becomes three modules: `:app` (Xe Launcher, unchanged id), `:studio` (Xe Studio, `com.xenoorigin.xestudio`) and `:studio-core` (Android library: recipe schema + icon renderer, no UI). Studio stores projects locally as JSON files and exposes the applied theme through a signature-protected ContentProvider; Xe copies the theme into its own storage on apply and renders icons with `:studio-core`.

**Tech Stack:** Kotlin 2.2.21, AGP 8.13.2, Compose BOM 2025.10.01, kotlinx-serialization 1.9.0, Haze 1.6.10, Robolectric 4.16, Roborazzi 1.50.0, Truth.

**Spec:** `docs/superpowers/specs/2026-11-21-xe-studio-design.md`

**Later phases (separate plans, each ships an APK):** 2 Wallpaper editor + moving wallpapers (and Xe drawing Studio wallpapers; Xe's own wallpaper studio moves out) · 3 Widget editor + AppWidget · 4 Accounts (Supabase, Google + email) + cloud sync · 5 Store, likes, reports, admin, keep-alive job.

## Global Constraints

- minSdk 29, targetSdk 36, compileSdk 36, JVM 17 — same as `:app`.
- Studio package/applicationId `com.xenoorigin.xestudio`; app name "Xe Studio" (both languages).
- Both apps signed with the release key via env `XE_KEYSTORE_PATH`, `XE_KEYSTORE_PASSWORD`, `XE_KEY_ALIAS`, `XE_KEY_PASSWORD`; cert SHA-256 must stay `ea0e6430…a09c68`. Never print the password.
- Studio versionCode/versionName use the same `GITHUB_RUN_NUMBER` scheme as `:app` (`1.0.N`); release APK file name `xe-studio.apk`.
- UI strings in `values/strings.xml` (English) and `values-th/strings.xml` (Thai); Thai is what the owner reads.
- Studio look: monochrome (black, white, greys) for chrome; colour only in the user's work; tool panels/sheets use Haze glass; spring animations (`spring(dampingRatio≈0.7–0.8)`), press squish on controls.
- Recipes are versioned JSON (`schemaVersion`), stored as text; images the user imports are re-encoded WebP, longest side ≤ 512 px, ≤ 1 MB.
- Xe must keep working when Studio is missing: it never reads Studio at render time, only at apply time.
- Tests: Robolectric + Compose UI tests + Roborazzi screenshots (record with `-Proborazzi.test.record=true`), run per module.

## Review Focus

1. Studio uninstalled after a theme was applied → Xe keeps showing the themed icons (copied theme), no crash. Test in Task 8.
2. Theme JSON from a newer Studio (`schemaVersion` higher) or corrupt → `IconRecipe.decode` returns null; Xe keeps its previous style and Studio shows "can't open". Test in Task 2 and Task 8.
3. App whose icon fails to load / app uninstalled while its override exists → renderer falls back to a letter tile; overrides for missing apps are ignored. Test in Task 3.
4. Gallery photo 4000×3000 picked as an override → stored ≤ 512 px WebP ≤ 1 MB, editor stays responsive. Test in Task 6.
5. Another app (different signature) queries the theme provider → SecurityException; only Xe can read. Test in Task 7.

---

### Task 1: Fix update alert being marked "sent" while notifications are blocked (Xe)

Promised to the owner; ships in the same release.

**Files:**
- Modify: `app/src/main/java/com/xenoorigin/xelauncher/update/UpdateNotifier.kt`
- Test: `app/src/test/java/com/xenoorigin/xelauncher/update/UpdateNotifierTest.kt`

**Interfaces:**
- Produces: `UpdateNotifier(settings, check, post: (ReleaseInfo) -> Boolean)` — `post` now returns whether the alert was really shown; `systemPoster(context): (ReleaseInfo) -> Boolean`.

- [ ] **Step 1: Failing test** `blockedPostDoesNotMarkTagAsNotified`: post returns false → `lastNotifiedTag` stays `""`; a second `checkNow()` after post starts returning true posts once and stores `"v1.0.99"`.
- [ ] **Step 2: Run** `/home/claude/bin/gt 'UpdateNotifierTest'` → FAIL (type mismatch / tag stored).
- [ ] **Step 3: Implement** — store `lastNotifiedTag` only when `post` returns true; `systemPoster` returns `areNotificationsEnabled() && notify succeeded`. Update existing callers/tests to the Boolean return.
- [ ] **Step 4: Run** same filter → PASS.
- [ ] **Step 5: Commit** `fix(update): only remember an update alert that was actually shown`.

### Task 2: `:studio-core` module and the icon recipe

**Files:**
- Modify: `settings.gradle.kts` (include `:studio-core`, `:studio`), `gradle/libs.versions.toml` (add `android-library` plugin id `com.android.library` version.ref agp)
- Create: `studio-core/build.gradle.kts` (namespace `com.xenoorigin.xestudio.core`, serialization, Robolectric tests), `studio-core/src/main/AndroidManifest.xml`
- Create: `studio-core/src/main/java/com/xenoorigin/xestudio/core/IconRecipe.kt`
- Test: `studio-core/src/test/java/com/xenoorigin/xestudio/core/IconRecipeTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable enum class TileShape { CIRCLE, SQUIRCLE, ROUNDED, SQUARE, HEXAGON, FLOWER, TEARDROP }`
  - `@Serializable enum class Treatment { ORIGINAL, MONO, TINT, DUOTONE }`
  - `@Serializable data class Fill(val colors: List<Int>, val angle: Float = 90f)` (1 colour = solid, 2+ = linear gradient)
  - `@Serializable sealed interface IconOverride` with `Image(val file: String)`, `Letter(val text: String, val color: Int, val bg: Fill)`
  - `@Serializable data class IconRecipe(val schemaVersion: Int = 1, val shape: TileShape = SQUIRCLE, val background: Fill = Fill(listOf(0xFF111111.toInt())), val treatment: Treatment = MONO, val tint: Int = 0xFFFFFFFF.toInt(), val tint2: Int = 0xFF888888.toInt(), val outlineWidth: Float = 0f, val outlineColor: Int = 0xFFFFFFFF.toInt(), val shadow: Float = 0f, val glyphScale: Float = 0.62f, val overrides: Map<String, IconOverride> = emptyMap())` — override keys are `AppKey.encode()` strings.
  - `IconRecipe.Companion.CURRENT_SCHEMA = 1`, `fun encode(r: IconRecipe): String`, `fun decode(json: String): IconRecipe?` (ignoreUnknownKeys; null on parse error or `schemaVersion > CURRENT_SCHEMA`).

- [ ] **Step 1: Failing tests** `roundTripKeepsEveryField`, `unknownFieldsAreIgnored` (extra `"sparkle":1` decodes), `newerSchemaIsRejected` (`schemaVersion: 2` → null), `garbageIsRejected` (`"{nope"` → null), `defaultsAreMonoSquircle`.
- [ ] **Step 2: Run** `./gradlew :studio-core:testDebugUnitTest` → FAIL (module/classes missing).
- [ ] **Step 3: Implement** the module and `IconRecipe.kt` per the interface above.
- [ ] **Step 4: Run** same → PASS; also `./gradlew :app:testDebugUnitTest --tests '*UpdateNotifierTest*'` still PASS (build wiring didn't break `:app`).
- [ ] **Step 5: Commit** `feat(studio-core): versioned icon recipe`.

### Task 3: Icon renderer in `:studio-core`

**Files:**
- Create: `studio-core/src/main/java/com/xenoorigin/xestudio/core/IconRenderer.kt`, `studio-core/src/main/java/com/xenoorigin/xestudio/core/TileShapes.kt`
- Test: `studio-core/src/test/java/com/xenoorigin/xestudio/core/IconRendererTest.kt`

**Interfaces:**
- Consumes: `IconRecipe`, `IconOverride`, `TileShape`.
- Produces:
  - `fun tilePath(shape: TileShape, sizePx: Float): android.graphics.Path`
  - `object IconRenderer { fun render(recipe: IconRecipe, appKey: String, label: String, source: android.graphics.drawable.Drawable?, sizePx: Int, overrideImage: (String) -> android.graphics.Bitmap? = { null }): android.graphics.Bitmap }`
  - Order: override for `appKey` (Image via `overrideImage(file)`, else Letter) → else source icon foreground treated per `treatment`, scaled by `glyphScale`, on `background` clipped to `tilePath` → outline → shadow. `source == null` and no override → letter tile from `label.first()`.

- [ ] **Step 1: Failing tests** (Robolectric NATIVE graphics, 96 px):
  - `circleCornersAreTransparent` — pixel (1,1) alpha == 0 for CIRCLE, != 0 for SQUARE.
  - `backgroundFillsTileEdge` — pixel near left-middle edge equals background colour for solid fill.
  - `monoProducesGrayGlyph` — red source drawable → centre pixel has r==g==b.
  - `tintColoursGlyph` — TINT with tint 0xFF00FF00 → centre pixel green dominant.
  - `overrideImageWins` — override Image for key "pkg/act#0" → centre pixel equals override bitmap colour.
  - `missingSourceFallsBackToLetter` — source null, no override → bitmap not blank (some opaque non-background pixels in centre).
  - `overrideForOtherAppIgnored` — override keyed "other/x#0" doesn't affect "pkg/act#0".
- [ ] **Step 2: Run** `./gradlew :studio-core:testDebugUnitTest` → FAIL.
- [ ] **Step 3: Implement** with `android.graphics` only (no Compose) so both apps can call it off the main thread. HEXAGON/FLOWER/TEARDROP paths are builder's choice of geometry.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(studio-core): icon renderer`.

### Task 4: `:studio` app shell — pen icon, monochrome theme, four tabs

**Files:**
- Create: `studio/build.gradle.kts` (applicationId `com.xenoorigin.xestudio`, same signing/version/runNumber block as `:app`, release output name `xe-studio.apk`, deps: core-ktx, activity-compose, navigation-compose, compose BOM ui/foundation/material3, haze, serialization, coroutines, `project(":studio-core")`; test deps as `:app`), `studio/proguard-rules.pro`
- Create: `studio/src/main/AndroidManifest.xml`, `studio/src/main/java/com/xenoorigin/xestudio/MainActivity.kt`, `.../StudioApp.kt` (scaffold + bottom bar), `.../design/StudioTheme.kt` (monochrome colours, type, `glassPanel` modifier using Haze, `pressSquish` modifier)
- Create: `studio/src/main/res/drawable/ic_launcher_foreground.xml` (rounded chunky pen, white on black), `.../ic_launcher_monochrome.xml`, `studio/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `values/strings.xml`, `values-th/strings.xml`
- Test: `studio/src/test/java/com/xenoorigin/xestudio/StudioAppTest.kt`, `.../StudioScreenshotTest.kt`

**Interfaces:**
- Produces: `enum class Tab { CREATE, STORE, MINE, PROFILE }`; `@Composable fun StudioApp(graph: StudioGraph)`; `class StudioGraph(context: Context)` (holds stores added by later tasks); test tags `tab_create`, `tab_store`, `tab_mine`, `tab_profile`.
- Store and Profile tabs show a calm "coming soon" placeholder in this phase.

- [ ] **Step 1: Failing tests** `fourTabsShowAndSwitch` (each tag exists; clicking `tab_mine` shows text of `R.string.mine_empty`), `createOffersIconTheme` (Create tab has node with text of `R.string.new_icon_theme`), screenshot `studio_shell` recorded.
- [ ] **Step 2: Run** `./gradlew :studio:testDebugUnitTest` → FAIL.
- [ ] **Step 3: Implement** shell; Thai strings: "สร้าง", "ร้านธีม", "งานของฉัน", "โปรไฟล์", "ธีมไอคอนใหม่", "ยังไม่มีงาน เริ่มสร้างอันแรกได้เลย", "เร็ว ๆ นี้". Tab switch animates with a spring-driven indicator.
- [ ] **Step 4: Run** → PASS; `./gradlew :studio:assembleRelease` with signing env → produces `studio/build/outputs/apk/release/xe-studio.apk`; `apksigner verify --print-certs` shows SHA-256 `ea0e6430…a09c68`.
- [ ] **Step 5: Commit** `feat(studio): app shell with pen icon and four tabs`.

### Task 5: Local projects

**Files:**
- Create: `studio/src/main/java/com/xenoorigin/xestudio/projects/ProjectStore.kt`
- Modify: `StudioGraph` (add `projects`), My stuff tab lists projects
- Test: `studio/src/test/java/com/xenoorigin/xestudio/projects/ProjectStoreTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable enum class ProjectKind { ICONS }` (WALLPAPER/WIDGET added in later phases)
  - `@Serializable data class Project(val id: String, val kind: ProjectKind, val name: String, val recipeJson: String, val updatedAt: Long)`
  - `class ProjectStore(dir: File, clock: () -> Long = System::currentTimeMillis) { val projects: StateFlow<List<Project>>; suspend fun create(kind: ProjectKind, name: String, recipeJson: String): Project; suspend fun save(p: Project): Project /* bumps updatedAt */; suspend fun delete(id: String); fun imageDir(id: String): File }`
  - Layout: `dir/<id>/project.json`, `dir/<id>/images/`. Writes are atomic (temp file + rename). Unreadable project folders are skipped, not fatal.

- [ ] **Step 1: Failing tests** `createThenReloadFromDisk`, `saveBumpsUpdatedAtAndSortsNewestFirst`, `deleteRemovesFolderAndImages`, `corruptProjectIsSkipped`.
- [ ] **Step 2: Run** `./gradlew :studio:testDebugUnitTest --tests '*ProjectStoreTest*'` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(studio): local projects`.

### Task 6: Icon theme editor (whole set + per-icon)

**Files:**
- Create: `studio/src/main/java/com/xenoorigin/xestudio/icons/InstalledApps.kt`, `.../icons/IconEditorScreen.kt`, `.../icons/IconEditorState.kt`, `.../icons/OverrideSheet.kt`, `.../icons/DrawPad.kt`, `.../images/ImageImport.kt`
- Test: `studio/src/test/java/com/xenoorigin/xestudio/icons/IconEditorStateTest.kt`, `.../icons/IconEditorScreenTest.kt`, `.../images/ImageImportTest.kt`

**Interfaces:**
- Consumes: `IconRecipe`, `IconRenderer`, `ProjectStore`, `Project`.
- Produces:
  - `data class InstalledApp(val key: String /* AppKey.encode() format "pkg/activity#userSerial" */, val label: String, val icon: Drawable?)`; `fun interface AppSource { suspend fun apps(): List<InstalledApp> }` with a `LauncherApps` implementation.
  - `class IconEditorState(project: Project, store: ProjectStore) { val recipe: StateFlow<IconRecipe>; fun update(f: (IconRecipe) -> IconRecipe); fun setOverride(key: String, o: IconOverride?); suspend fun flush() }` — edits debounced 400 ms then saved.
  - `object ImageImport { fun normalize(src: Bitmap): ByteArray /* WebP, longest side ≤ 512, ≤ 1_000_000 bytes */ }`
  - Screen test tags: `preview_grid`, `ctl_shape_<NAME>`, `ctl_treatment_<NAME>`, `icon_<key>`, `override_sheet`.
- Behaviour: preview grid of real apps (rendered off main thread, cached per recipe hash); whole-set controls for shape, background (solid/gradient presets + picker), treatment, tint(s), outline, shadow, glyph size; changing a control replays a staggered spring "ripple" over the grid. Tapping an icon opens the override sheet: pick image (photo picker), letter/symbol, draw (simple pen draw pad, black/white + current tint), or reset.

- [ ] **Step 1: Failing tests**
  - `updateIsSavedAfterDebounce` (virtual time: update then advance 400 ms → store has new recipeJson).
  - `overrideSetAndCleared`.
  - `normalizeShrinksHugePhoto` (4000×3000 bitmap → decoded result longest side 512, size ≤ 1_000_000).
  - Screen: `tappingShapeChangesRecipe` (fake AppSource with 3 apps; click `ctl_shape_CIRCLE` → state recipe shape CIRCLE), `tappingIconOpensOverrideSheet`.
  - Screenshot `icon_editor` recorded.
- [ ] **Step 2: Run** `./gradlew :studio:testDebugUnitTest --tests '*Icon*' --tests '*ImageImport*'` → FAIL.
- [ ] **Step 3: Implement.** Thai copy for controls: "ทรง", "พื้นหลัง", "สีไอคอน", "เส้นขอบ", "เงา", "ขนาดสัญลักษณ์", "แตะไอคอนเพื่อแก้ทีละตัว", "ใช้รูป", "ตัวอักษร", "วาดเอง", "คืนค่าเดิม".
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(studio): icon theme editor`.

### Task 7: "Use in Xe" — theme provider and broadcast (Studio side)

**Files:**
- Modify: `studio/src/main/AndroidManifest.xml` (permission `com.xenoorigin.xestudio.permission.READ_THEME` protectionLevel `signature`; provider authority `com.xenoorigin.xestudio.themes` with `readPermission`; `<queries><package android:name="com.xenoorigin.xelauncher"/></queries>`)
- Create: `studio/src/main/java/com/xenoorigin/xestudio/link/ThemeProvider.kt`, `.../link/XeLink.kt`
- Modify: icon editor top bar gets the "ใช้กับ Xe" button
- Test: `studio/src/test/java/com/xenoorigin/xestudio/link/ThemeProviderTest.kt`, `.../link/XeLinkTest.kt`

**Interfaces:**
- Produces (the contract Xe relies on — keep exact):
  - Authority `com.xenoorigin.xestudio.themes`. `content://com.xenoorigin.xestudio.themes/icons` → cursor with one row, columns `recipe` (String JSON), `version` (Long = applied time). `openFile(content://…/images/<file>, "r")` → the override image.
  - Broadcast action `com.xenoorigin.xestudio.action.THEME_APPLIED`, sent to package `com.xenoorigin.xelauncher`, permission `com.xenoorigin.xestudio.permission.READ_THEME`.
  - `object XeLink { fun isXeInstalled(ctx): Boolean; suspend fun applyIcons(ctx: Context, project: Project, store: ProjectStore): ApplyResult }` with `enum class ApplyResult { APPLIED, XE_MISSING }`. Applied theme copied to `filesDir/applied/icons.json` + `applied/images/`.
- When Xe is missing the button shows a sheet linking `https://xenoorigin.github.io/xe-launcher/`.

- [ ] **Step 1: Failing tests** `providerReturnsAppliedRecipe`, `providerServesOverrideImage`, `providerRejectsPathTraversal` (`images/../applied/icons.json` → FileNotFoundException), `applyWithoutXeReturnsXeMissing`, `applySendsBroadcastToXe` (Robolectric shadow: package installed → broadcast with action captured), `manifestProviderRequiresSignaturePermission` (parse PackageInfo: provider readPermission == the permission; permission protectionLevel == signature).
- [ ] **Step 2: Run** `./gradlew :studio:testDebugUnitTest --tests '*link*'` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(studio): use icon theme in Xe`.

### Task 8: Xe reads and renders Studio themes

**Files:**
- Modify: `app/build.gradle.kts` (`implementation(project(":studio-core"))`), `app/src/main/AndroidManifest.xml` (`<uses-permission android:name="com.xenoorigin.xestudio.permission.READ_THEME"/>`, `<queries><package android:name="com.xenoorigin.xestudio"/></queries>`, receiver for `THEME_APPLIED`)
- Modify: `app/src/main/java/com/xenoorigin/xelauncher/icons/IconStyle.kt` (add `STUDIO`), `icons/IconRepository.kt` (variant + cache key for STUDIO), `icons/AppIcon.kt` (STUDIO draws the rendered bitmap as the full tile), settings icon-style picker (new choice "จาก Xe Studio", shown only when a theme is stored)
- Create: `app/src/main/java/com/xenoorigin/xelauncher/studio/StudioThemeStore.kt`, `.../studio/StudioThemeReceiver.kt`
- Test: `app/src/test/java/com/xenoorigin/xelauncher/studio/StudioThemeStoreTest.kt`, additions to `IconRepositoryTest`

**Interfaces:**
- Consumes: provider contract from Task 7; `IconRecipe.decode`, `IconRenderer.render`.
- Produces: `class StudioThemeStore(dir: File, resolver: ContentResolver) { val theme: StateFlow<StoredTheme?>; suspend fun pull(): Boolean /* copies recipe + images into dir; false and keeps old copy on any failure */; fun image(file: String): Bitmap? }`, `data class StoredTheme(val recipe: IconRecipe, val version: Long)`.
- Rules: receiver calls `pull()`, then sets `iconStyle = STUDIO` and invalidates icon caches. Render with STUDIO uses the stored copy only (never the provider). If no stored theme, STUDIO renders as GLASS_DARK. A per-app custom picture from `CustomIconStore` still wins. Cache key includes `version`.

- [ ] **Step 1: Failing tests** `pullCopiesRecipeAndImages` (fake provider via Robolectric `ProviderController`), `pullKeepsOldThemeWhenProviderMissing`, `pullRejectsNewerSchema` (provider returns schemaVersion 2 → returns false, old theme kept), `studioStyleUsesStoredThemeAfterStudioRemoved` (pull, then unregister provider, render still uses theme), `customPictureBeatsStudioTheme`, `noThemeFallsBackToGlassDark`.
- [ ] **Step 2: Run** `/home/claude/bin/gt 'StudioThemeStoreTest'` and `/home/claude/bin/gt 'IconRepositoryTest'` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** both → PASS; then full `./gradlew :app:testDebugUnitTest` (run alone after `./gradlew --stop` if the daemon dies) → all PASS.
- [ ] **Step 5: Commit** `feat(xe): show icon themes from Xe Studio`.

### Task 9: Xe's "Xe Studio" settings entry opens the app

**Files:**
- Modify: `app/src/main/java/com/xenoorigin/xelauncher/settings/ui/SettingsScreens.kt` (`StudioScreen`), strings
- Test: `app/src/test/java/com/xenoorigin/xelauncher/settings/ui/StudioScreenTest.kt`

**Interfaces:**
- Produces: `StudioScreen` first row "เปิดแอป Xe Studio" (tag `open_studio`): launches `com.xenoorigin.xestudio` if installed, else opens `https://github.com/XenoORIGIN/xe-launcher/releases/latest/download/xe-studio.apk`. Ruling for phase 1: Xe's wallpaper studio and icon settings rows stay until Studio has replacements (wallpaper moves in phase 2, widgets in phase 3); the old Icon Studio row gets subtitle "ย้ายไป Xe Studio แล้ว — แตะเพื่อเปิด" and opens Studio when it's installed.

- [ ] **Step 1: Failing tests** `openStudioLaunchesAppWhenInstalled`, `openStudioOpensDownloadWhenMissing` (Robolectric: next started activity intent).
- [ ] **Step 2: Run** `/home/claude/bin/gt 'StudioScreenTest'` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(xe): open Xe Studio from settings`.

### Task 10: Release both apps together

**Files:**
- Modify: `.github/workflows/publish.yml` (upload/create with both `release/xe-launcher.apk` and `release/xe-studio.apk`; Studio APK optional-if-missing so old flow still works), `website/app.js` (NEWS entry + a "Xe Studio" download button linking `…/releases/latest/download/xe-studio.apk`), `website/index.html` if the button needs markup
- Create/Modify: `release/xe-launcher.apk`, `release/xe-studio.apk`, `release/version.txt`, `release/notes.md` (Thai + English: what's new, "อัปเดตทั้งสองแอปคู่กัน", restricted-settings note not needed)

- [ ] **Step 1:** `./gradlew --stop`; run `./gradlew :studio-core:testDebugUnitTest`, `:studio:testDebugUnitTest`, `:app:testDebugUnitTest` separately → all PASS; `./gradlew :app:lintDebug :studio:lintDebug` → no errors.
- [ ] **Step 2:** Build both release APKs with `GITHUB_RUN_NUMBER=N` (next number after the latest release) and signing env; verify each with `apksigner verify --print-certs` (SHA-256 `ea0e6430…a09c68`) and `aapt2 dump badging` (versionName `1.0.N`, Studio package `com.xenoorigin.xestudio`).
- [ ] **Step 3:** Copy APKs to `release/`, write version/notes, NEWS entry; `node -e` syntax check of `website/app.js` and `python3 website/check.py` → OK.
- [ ] **Step 4:** Commit `release: v1.0.N with Xe Studio`, `git push origin HEAD:main`; confirm the "Publish release" and "Website" runs succeed and the release has both assets (`gh api repos/XenoORIGIN/xe-launcher/releases/latest --jq '.assets[].name'`).
- [ ] **Step 5:** Republish `website/app.js` to artifact slug `62318dc7-062e-45b8-bdb3-ac864a0e5af8` (list_files first, base_version = latest). Tell the owner both website links.
