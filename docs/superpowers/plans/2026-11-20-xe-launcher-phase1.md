# Xe Launcher Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the first installable Xe Launcher APK — Home, Dock, categorised App Drawer, search, folders, hidden apps, Liquid Glass, black-and-white glass icons, bundled wallpapers, full Settings, animations, TH/EN — plus CI releases and the download website.

**Architecture:** Single-module Android app, one `MainActivity` declared as HOME, Jetpack Compose UI with Navigation Compose for Settings. Data layer = `LauncherApps` (apps), Room (layout, categories, folders, launch stats), DataStore (settings). Glass = Haze backdrop blur + AGSL refraction shader drawn over a launcher-owned wallpaper bitmap. Static website in `website/` deployed to GitHub Pages.

**Tech Stack:** Kotlin 2.x, AGP 8.x, Gradle 8.14 (wrapper), JDK 21 (toolchain 17 target), Compose BOM, Navigation Compose, Room (KSP), DataStore Preferences, Coil 3, kotlinx.serialization, Haze, JUnit4 + Robolectric + Roborazzi (screenshot tests), GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-20-xe-launcher-design.md`

## Global Constraints

- applicationId `com.xenoorigin.xelauncher`, app name "Xe Launcher"
- minSdk 29, targetSdk/compileSdk = latest stable; blur only on API 31+, refraction only on API 33+, below that a translucent tint
- Android only, distributed as APK; every release signed with the same keystore (from GitHub Secrets)
- Fonts: Inter (Latin) + Noto Sans Thai (Thai), bundled, OFL. No Apple fonts, icons, wallpapers or logos
- Defaults: dark theme, wallpaper "Xe Black", icon style `GLASS_DARK` (real app icon → grayscale on dark glass tile), glass quality `NORMAL`, animation speed `NORMAL`, swipe-down = notification shade
- Every UI element must stay inside its own panel's bounds (no overflow)
- All user-facing strings in `values/strings.xml` (EN) and `values-th/strings.xml` (TH)
- Respect system "remove animations" (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` → no motion)
- Repo `XenoORIGIN/xe-launcher` (public); site `https://xenoorigin.github.io/xe-launcher/`; stable APK URL `https://github.com/XenoORIGIN/xe-launcher/releases/latest/download/xe-launcher.apk`

## Review Focus

1. **Package changes while running** — install/uninstall/update/disable an app → drawer, categories, search and Home update within a second; removed apps disappear from Home and folders (empty folder of 1 dissolves into its app).
2. **Clone apps / work profile** (Samsung Dual Messenger shows two "Messages") → each (package, activity, user) is a separate entry with a small clone badge; launching opens the right user's app.
3. **Thai labels** — search matches Thai substrings and Latin case-insensitively; A–Z sort uses Thai collation (`Collator.getInstance(Locale("th"))`) with Latin before Thai.
4. **Launcher process death** (Samsung kills it often) → layout, categories, settings and wallpaper all restored exactly; first-run defaults are never re-applied over existing data.
5. **Large app counts (250+)** → drawer opens and pages without jank; icons load async from a disk cache, never on the main thread.

Each line has a test in the owning task (Tasks 3, 3, 9/10, 4/5, 2).

---

## File Structure

```
app/src/main/java/com/xenoorigin/xelauncher/
  MainActivity.kt              HOME activity, edge-to-edge, hosts XeApp
  XeApp.kt                     root composable: wallpaper + pager(Home | Drawer) + overlays + nav to Settings
  XeApplication.kt             manual DI container (AppGraph)
  di/AppGraph.kt
  design/  Type.kt Color.kt Theme.kt Motion.kt Glass.kt GlassShader.kt PressSquish.kt Fonts.kt
  apps/    AppKey.kt AppEntry.kt AppSource.kt LauncherAppsSource.kt
  icons/   IconStyle.kt IconProcessor.kt IconRepository.kt AppIcon.kt
  data/    XeDatabase.kt entities/*.kt dao/*.kt
  settings/ XeSettings.kt SettingsStore.kt
  categories/ BuiltInCategory.kt Categorizer.kt CategoryRepository.kt
  home/    HomeRepository.kt HomeLayout.kt HomeScreen.kt Dock.kt EditMode.kt IconMenu.kt FolderPopup.kt Gestures.kt
  drawer/  DrawerState.kt AppDrawer.kt CategoryBar.kt DrawerMenuSheet.kt CategoryEditor.kt
  search/  SearchEngine.kt Calculator.kt SearchPage.kt
  wallpaper/ WallpaperPreset.kt WallpaperRenderer.kt WallpaperController.kt WallpaperScreen.kt
  settings/ui/ SettingsNav.kt SettingsHome.kt AppearanceScreen.kt IconsScreen.kt AnimationScreen.kt GesturesScreen.kt PagesScreens.kt HiddenAppsScreen.kt BackupScreen.kt AboutScreen.kt
  backup/  BackupManager.kt
  update/  UpdateChecker.kt
  onboarding/ SetDefaultHome.kt
app/src/main/res/  font/ values/ values-th/ mipmap-anydpi-v26/ raw/glass.agsl
app/src/test/...   unit + Robolectric + Roborazzi tests mirroring packages
.github/workflows/android.yml  .github/workflows/pages.yml
website/ index.html style.css app.js assets/
```

---

### Task 1: Scaffold, signing and CI release

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `XeApplication.kt`, `.github/workflows/android.yml`, `.gitignore`, `README.md`
- Test: `app/src/test/.../SmokeTest.kt`

**Interfaces:**
- Produces: `MainActivity` (intent filter `MAIN` + `HOME` + `DEFAULT`, `launchMode=singleTask`, `screenOrientation=portrait`, `stateNotNeeded=true`, theme with `windowShowWallpaper=false`); `XeApplication.graph: AppGraph`; Gradle tasks `assembleRelease`, `testDebugUnitTest`, `verifyRoborazziDebug`.

- [ ] **Step 1:** Write `SmokeTest` (Robolectric): launching `MainActivity` reaches RESUMED; manifest resolves it for `Intent(ACTION_MAIN).addCategory(CATEGORY_HOME)`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*SmokeTest'` → FAIL (no project).
- [ ] **Step 3:** Create the Gradle project and manifest. Release signing reads `XE_KEYSTORE_PATH`, `XE_KEYSTORE_PASSWORD`, `XE_KEY_ALIAS`, `XE_KEY_PASSWORD` env vars; falls back to debug signing when absent. `versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()`, `versionName = "1.0.$versionCode"`. Output file renamed to `xe-launcher.apk`.
- [ ] **Step 4:** Run the test → PASS; `./gradlew assembleRelease` → APK exists.
- [ ] **Step 5:** `android.yml`: on push to `main` + manual dispatch; JDK 21; decode `KEYSTORE_BASE64` secret to file; `./gradlew testDebugUnitTest assembleRelease`; create/update release tag `v1.0.${{ github.run_number }}` with `xe-launcher.apk` attached (marked latest).
- [ ] **Step 6:** Generate a release keystore once (keytool, RSA 4096, validity 10000 days), store as repo secrets; keep a backup copy out of git and hand it to the owner.
- [ ] **Step 7:** Commit `chore: scaffold Xe Launcher with CI release`.

### Task 2: Design system — fonts, theme, motion, Liquid Glass

**Files:**
- Create: `design/*.kt`, `res/font/*`, `res/raw/glass.agsl`
- Test: `design/GlassStyleTest.kt`, `design/GlassScreenshotTest.kt` (Roborazzi)

**Interfaces:**
- Produces:
  - `enum class GlassQuality { HIGH, NORMAL, SAVER }`
  - `data class GlassStyle(val blurRadius: Dp, val tint: Color, val rimAlpha: Float, val refraction: Float, val noiseAlpha: Float)` + `fun glassStyleFor(q: GlassQuality, dark: Boolean, sdk: Int = Build.VERSION.SDK_INT): GlassStyle`
  - `val LocalGlassStyle`, `val LocalHazeState`
  - `fun Modifier.liquidGlass(shape: Shape, style: GlassStyle = LocalGlassStyle.current): Modifier` — Haze child blur + tint + specular rim (sweep gradient, bright top-left, faint iridescent) + inner top/bottom glow + noise; adds AGSL refraction layer when `style.refraction > 0`
  - `fun Modifier.pressSquish(interaction: MutableInteractionSource, haptics: Boolean = true): Modifier` — scale to 0.94 x / 0.96 y on press with spring back
  - `object XeMotion { fun spring(speed: AnimationSpeed): SpringSpec<Float>; val staggerStepMs: Int = 28 }`, `enum class AnimationSpeed(val scale: Float) { OFF(0f), FAST(0.7f), NORMAL(1f), SMOOTH(1.35f) }`
  - `@Composable fun XeTheme(settings: XeSettings, content: @Composable () -> Unit)` — typography: LargeTitle 34sp Bold −0.6sp, Title 22sp SemiBold, Body 15sp, Label 12sp Medium; family `XeFont` (Inter + Noto Sans Thai fallback)

- [ ] **Step 1:** `GlassStyleTest`: `SAVER` → `blurRadius == 0.dp && refraction == 0f`; `NORMAL` on sdk 31 → blur 20.dp, refraction 0; `HIGH` on sdk 33 → refraction > 0; `HIGH` on sdk 32 → refraction 0; any quality on sdk 30 → blur 0.
- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Implement design files. Blur values: HIGH 24.dp, NORMAL 20.dp, SAVER 0. Dark tint `Color.Black.copy(.28f)`, light tint `Color.White.copy(.18f)`.
- [ ] **Step 4:** `GlassScreenshotTest` (Robolectric native graphics, sdk 34): pill, circle and squircle glass over the "Xe Blue" preset, dark + light; record golden and visually inspect for overflow/rim quality.
- [ ] **Step 5:** Run tests → PASS. Commit `feat(design): Xe theme and Liquid Glass`.

### Task 3: App source (LauncherApps)

**Files:**
- Create: `apps/*.kt`
- Test: `apps/LauncherAppsSourceTest.kt`, `apps/AppKeyTest.kt`

**Interfaces:**
- Produces:
  - `data class AppKey(val packageName: String, val activity: String, val userSerial: Long) { fun encode(): String; companion object { fun decode(s: String): AppKey? } }` — format `pkg/activity#serial`
  - `data class AppEntry(val key: AppKey, val label: String, val androidCategory: Int, val installedAt: Long, val isClone: Boolean)`
  - `interface AppSource { val apps: StateFlow<List<AppEntry>>; fun launch(key: AppKey, sourceBounds: Rect?, opts: Bundle?); fun openAppInfo(key: AppKey); fun uninstall(key: AppKey); fun shortcuts(key: AppKey): List<ShortcutInfo> ; fun startShortcut(s: ShortcutInfo, bounds: Rect?) }`
  - `class LauncherAppsSource(context: Context, scope: CoroutineScope) : AppSource` — all profiles from `UserManager.userProfiles`; refreshes on `LauncherApps.Callback`

- [ ] **Step 1:** Tests: `AppKey` encode/decode round-trip, decode of garbage → null; with Robolectric shadow LauncherApps: two profiles exposing same package → 2 entries, the non-main-user one `isClone == true`; `onPackageRemoved` callback → entry gone from `apps` (Review Focus 1, 2).
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `feat(apps): LauncherApps source with profiles`.

### Task 4: Settings store

**Files:**
- Create: `settings/XeSettings.kt`, `settings/SettingsStore.kt`
- Test: `settings/SettingsStoreTest.kt`

**Interfaces:**
- Produces:
  - `data class XeSettings(themeMode: ThemeMode = DARK, accent: Long = 0xFFFFFFFF, glassQuality: GlassQuality = NORMAL, fontScale: Float = 1f, language: AppLanguage = SYSTEM, iconStyle: IconStyle = GLASS_DARK, iconShape: IconShape = SQUIRCLE, iconSizeDp: Int = 60, showLabels: Boolean = true, grayscaleContrast: Float = 1.15f, animationSpeed: AnimationSpeed = NORMAL, pressSquish: Boolean = true, wallpaperParallax: Boolean = true, homeColumns: Int = 4, homeRows: Int = 6, dockSize: Int = 4, swipeUp: GestureAction = OPEN_DRAWER, swipeDown: GestureAction = NOTIFICATIONS, doubleTap: GestureAction = NONE, twoFingerDown: GestureAction = OPEN_SEARCH, drawerSort: DrawerSort = ALPHABETICAL, categoryBar: Boolean = true, layoutLocked: Boolean = false, hiddenApps: Set<String> = emptySet(), wallpaperId: String = "xe_black", firstRunDone: Boolean = false)`
  - enums `ThemeMode{LIGHT,DARK,AUTO}`, `AppLanguage{SYSTEM,TH,EN}`, `IconShape{SQUIRCLE,CIRCLE,ROUNDED}`, `GestureAction{NONE,OPEN_DRAWER,NOTIFICATIONS,OPEN_SEARCH,OPEN_SETTINGS,EDIT_MODE}`, `DrawerSort{ALPHABETICAL,MOST_USED,RECENTLY_INSTALLED}`
  - `class SettingsStore(context: Context) { val settings: Flow<XeSettings>; suspend fun update(f: (XeSettings) -> XeSettings); suspend fun exportJson(): String; suspend fun importJson(json: String) }`

- [ ] **Step 1:** Tests: fresh store emits `XeSettings()` with the spec defaults (dark, `GLASS_DARK`, `NORMAL`, `xe_black`); `update` persists across a new `SettingsStore` instance on the same file (Review Focus 4); export→import round-trip equals; unknown enum value in imported JSON falls back to default instead of throwing.
- [ ] **Step 2–4:** FAIL → implement (single JSON-serialized preference key) → PASS.
- [ ] **Step 5:** Commit `feat(settings): DataStore-backed settings`.

### Task 5: Database, categories and auto-categorisation

**Files:**
- Create: `data/XeDatabase.kt`, `data/entities/*`, `data/dao/*`, `categories/*.kt`
- Test: `categories/CategorizerTest.kt`, `categories/CategoryRepositoryTest.kt`

**Interfaces:**
- Produces:
  - Entities: `CategoryEntity(id: Long, builtIn: String?, name: String?, iconName: String, position: Int)`, `AppCategoryOverride(appKey: String /*PK*/, categoryId: Long)`, `LaunchStat(appKey: String /*PK*/, count: Int, lastLaunch: Long)`, plus `HomeItemEntity`, `FolderEntity` (used by Task 8)
  - `enum class BuiltInCategory(val iconName: String) { COMMUNICATION("message-circle"), SOCIAL("affiliate"), GAMES("device-gamepad-2"), MEDIA("photo"), TOOLS("settings"), OTHER("category") }` — labels: สื่อสาร/Communication, โซเชียล/Social, เกม/Games, สื่อ/Media, เครื่องมือ/Tools, แอปอื่น/Other
  - `object Categorizer { fun categorize(packageName: String, androidCategory: Int): BuiltInCategory }`
  - `data class CategoryWithApps(val id: Long, val title: String, val iconName: String, val apps: List<AppEntry>)`
  - `class CategoryRepository(db: XeDatabase, apps: AppSource, settings: SettingsStore) { val categories: Flow<List<CategoryWithApps>>; suspend fun ensureDefaults(); suspend fun add(name: String, iconName: String): Long; suspend fun rename(id: Long, name: String); suspend fun setIcon(id: Long, iconName: String); suspend fun delete(id: Long); suspend fun reorder(ids: List<Long>); suspend fun moveApp(key: AppKey, categoryId: Long); suspend fun recordLaunch(key: AppKey) }`
- Rules: Android `CATEGORY_GAME`→GAMES; `SOCIAL`,`NEWS`→SOCIAL; `AUDIO`,`VIDEO`,`IMAGE`→MEDIA; `PRODUCTIVITY`,`MAPS`,`ACCESSIBILITY`→TOOLS; else a bundled package map (`assets/category_map.json`, e.g. `jp.naver.line.android`, `com.discord`, `org.telegram.messenger`, `com.facebook.orca`, `com.samsung.android.messaging`, `com.google.android.apps.messaging`, `com.samsung.android.dialer`, `com.samsung.android.app.contacts` → COMMUNICATION; `com.facebook.katana`, `com.instagram.android`, `com.instagram.barcelona`, `com.zhiliaoapp.musically`, `com.linkedin.android`, `com.bd.nproject` → SOCIAL; `com.mojang.minecraftpe` → GAMES; `com.android.settings`, `com.sec.android.app.myfiles` → TOOLS) else prefix rules (`com.samsung.android.app.` camera/gallery → MEDIA) else OTHER. Override table wins. Hidden apps excluded. Empty user categories are kept; empty built-in categories are hidden from the bar.
- Sorting inside a category follows `XeSettings.drawerSort`; ALPHABETICAL uses Thai collator with Latin first (Review Focus 3).

- [ ] **Step 1:** `CategorizerTest`: game category → GAMES; `com.discord` with `CATEGORY_UNDEFINED` → COMMUNICATION; unknown package → OTHER. `CategoryRepositoryTest` (in-memory Room + fake AppSource): `ensureDefaults` twice → still 6 categories (Review Focus 4); `moveApp` then categories show app in new one only; `delete(userCategory)` → its apps return to auto categories; `reorder` persists; ALPHABETICAL orders `["Zoom","กล้อง","Apple","ไลน์"]` as `["Apple","Zoom","กล้อง","ไลน์"]`; MOST_USED orders by `LaunchStat.count` desc.
- [ ] **Step 2–4:** FAIL → implement → PASS.
- [ ] **Step 5:** Commit `feat(categories): auto + editable categories`.

### Task 6: Icons — grayscale glass default and other styles

**Files:**
- Create: `icons/*.kt`
- Test: `icons/IconProcessorTest.kt`, `icons/IconRepositoryTest.kt`, `icons/AppIconScreenshotTest.kt`

**Interfaces:**
- Produces:
  - `enum class IconStyle { GLASS_DARK, GLASS_COLOR, GLASS_CLEAR, ORIGINAL }`
  - `object IconProcessor { fun grayscale(src: Bitmap, contrast: Float): Bitmap; fun dominantColor(src: Bitmap): Int; fun foregroundOf(d: Drawable, sizePx: Int): Bitmap }` — grayscale via `ColorMatrix.setSaturation(0)` then contrast around 128; dominantColor ignores pixels with alpha < 128 and near-white/near-black, falls back to `0xFF8E8E93`
  - `class IconRepository(context: Context, apps: AppSource, cacheDir: File) { suspend fun icon(key: AppKey, style: IconStyle, sizePx: Int, contrast: Float): ImageBitmap; fun invalidate(packageName: String) }` — LRU memory (64) + PNG disk cache keyed `encode()+style+size+contrast`, work on `Dispatchers.Default`
  - `@Composable fun AppIcon(app: AppEntry, size: Dp, style: IconStyle, shape: IconShape, label: Boolean, modifier: Modifier = Modifier)` — GLASS_DARK: dark liquidGlass tile + grayscale foreground at 62% of tile; GLASS_COLOR: tile gradient from dominantColor; GLASS_CLEAR: clear glass tile + colour foreground; ORIGINAL: unmodified; clone badge when `app.isClone`

- [ ] **Step 1:** Tests: grayscale output has r==g==b for every opaque pixel; transparent pixels stay transparent; dominantColor of a half-red/half-white bitmap is red; repository second call for same key hits disk cache (no re-render, verified with counter); `invalidate(pkg)` forces re-render (Review Focus 1, 5).
- [ ] **Step 2–4:** FAIL → implement → PASS. Screenshot test of the 4 styles on Xe Black and Xe Blue; inspect.
- [ ] **Step 5:** Commit `feat(icons): grayscale glass icons and styles`.

### Task 7: Wallpapers — Xe Collection, renderer, controller

**Files:**
- Create: `wallpaper/*.kt`
- Test: `wallpaper/WallpaperPresetTest.kt`, `wallpaper/WallpaperControllerTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface WallpaperPreset { val id: String; val nameRes: Int }` with `Mesh(colors: List<Long>, seed: Int, grain: Float)`, `Abstract(colors: List<Long>, seed: Int, shapes: Int)`, `Image(file: File)`
  - `val XeCollection: List<WallpaperPreset>` — 12 items, first `xe_black` (black with soft grey light blobs, like the owner's black-glass reference), also `xe_blue` (blue/violet flow, as mockup v2)
  - `object WallpaperRenderer { fun render(p: WallpaperPreset, w: Int, h: Int): Bitmap }` — deterministic for same seed
  - `enum class WallpaperTarget { HOME, LOCK, BOTH }`
  - `class WallpaperController(context: Context, settings: SettingsStore) { val current: StateFlow<ImageBitmap?>; suspend fun apply(preset: WallpaperPreset, target: WallpaperTarget); suspend fun importFromUri(uri: Uri): WallpaperPreset.Image }` — renders at `WindowManager` max bounds, saves `files/wallpaper/current.png`, calls `WallpaperManager.setBitmap(bmp, null, true, flags)`, persists `wallpaperId`
  - `@Composable fun WallpaperLayer(controller: WallpaperController, parallax: Float)` — draws `current`, provides `LocalHazeState` source
- [ ] **Step 1:** Tests: `XeCollection.size == 12`, ids unique, first id `xe_black`; render same preset twice → identical pixels; render size matches request; `apply` writes file and restores `current` after a new controller instance (Review Focus 4).
- [ ] **Step 2–4:** FAIL → implement → PASS. Record a contact-sheet screenshot of all 12 for review.
- [ ] **Step 5:** Commit `feat(wallpaper): Xe Collection and controller`.

### Task 8: Home layout model

**Files:**
- Create: `home/HomeRepository.kt`, `home/HomeLayout.kt`
- Test: `home/HomeRepositoryTest.kt`

**Interfaces:**
- Consumes: `HomeItemEntity(id: Long, type: String /*APP|FOLDER*/, appKey: String?, folderId: Long?, container: String /*PAGE|DOCK|FOLDER*/, page: Int, x: Int, y: Int, position: Int)`, `FolderEntity(id: Long, name: String)`
- Produces:
  - `data class HomeLayout(val pages: List<List<HomeCell>>, val dock: List<HomeCell>)`; `sealed interface HomeCell { val id: Long; val x: Int; val y: Int }` with `AppCell(app: AppEntry)`, `FolderCell(name: String, apps: List<AppEntry>)`
  - `class HomeRepository(db, apps: AppSource, settings: SettingsStore) { val layout: Flow<HomeLayout>; suspend fun seedDefaultIfEmpty(); suspend fun addApp(key: AppKey, page: Int? = null): Boolean; suspend fun move(itemId: Long, page: Int, x: Int, y: Int); suspend fun moveToDock(itemId: Long, position: Int): Boolean; suspend fun dropOnto(draggedId: Long, targetId: Long): Long /*folder id*/; suspend fun renameFolder(id: Long, name: String); suspend fun remove(itemId: Long); suspend fun addPage(); suspend fun removeEmptyPages() }`
- Rules: moving onto an occupied cell swaps; dock max = `dockSize`; app dropped on app → new folder named after target's category title; folder reduced to 1 app dissolves into that app cell; items whose app is uninstalled are removed (Review Focus 1); seed = dock (Phone, Messages, Camera, Browser by default handler) + page 0 empty.
- [ ] **Step 1:** Tests for each rule above, plus `seedDefaultIfEmpty` twice doesn't duplicate (Review Focus 4).
- [ ] **Step 2–4:** FAIL → implement → PASS.
- [ ] **Step 5:** Commit `feat(home): layout repository`.

### Task 9: App Drawer UI with category animation

**Files:**
- Create: `drawer/*.kt`
- Test: `drawer/DrawerStateTest.kt`, `drawer/AppDrawerScreenshotTest.kt`

**Interfaces:**
- Consumes: `CategoryRepository.categories`, `AppIcon`, `liquidGlass`, `pressSquish`, `XeMotion`
- Produces:
  - `class DrawerState(initial: Int = 0) { val pagerState: PagerState; val bubbleOffsetFraction: Float /*0..n-1 continuous*/; suspend fun animateTo(index: Int) }`
  - `@Composable fun AppDrawer(categories: List<CategoryWithApps>, state: DrawerState, settings: XeSettings, onLaunch: (AppEntry, Rect) -> Unit, onLongPress: (AppEntry, Rect) -> Unit, onSearch: () -> Unit, onStore: () -> Unit, onMenu: () -> Unit)`
  - `@Composable fun CategoryBar(categories, state)` — capsule glass, height 66.dp, radius 33.dp, horizontal padding 8.dp, equal-weight slots; **bubble**: inner glass pill 50.dp high, x follows `bubbleOffsetFraction` continuously; while moving its width stretches by up to 35% proportional to velocity, then springs back (dampingRatio 0.55, stiffness 380 × speed); selected icon scales 1→1.12→1 and goes from 62% to 100% white
  - Title: crossfade + 12.dp vertical slide; grid: per-item stagger `index * XeMotion.staggerStepMs` with alpha 0→1, scale 0.86→1, translateY 10.dp→0
  - `DrawerMenuSheet`: add shortcut / folder / category, category bar toggle, sorting, hidden apps, lock layout, "all settings"
  - `CategoryEditor`: rename, choose icon (from bundled Tabler subset), reorder (drag), delete, move apps
- Layout: header top padding = status bar + 56.dp, LargeTitle, three 42.dp glass circle buttons; 4-column grid; bar sits above navigation bar inset + 14.dp; when `categoryBar == false` show one A–Z list
- [ ] **Step 1:** `DrawerStateTest`: `animateTo(3)` lands at page 3 and `bubbleOffsetFraction == 3f`; with AnimationSpeed.OFF it snaps without intermediate frames. Screenshot test with 6 categories on a 360dp-wide device: assert every bar child's bounds are inside the bar bounds (semantics tree check) — prevents the overflow the owner reported; also at fontScale 1.3.
- [ ] **Step 2–4:** FAIL → implement → PASS; inspect screenshots against `xe-drawer-mockup.html`.
- [ ] **Step 5:** Commit `feat(drawer): categorised drawer with liquid category bar`.

### Task 10: Search page

**Files:**
- Create: `search/*.kt`
- Test: `search/SearchEngineTest.kt`, `search/CalculatorTest.kt`

**Interfaces:**
- Produces: `sealed interface SearchResult { App(entry), Shortcut(info), Calc(expr: String, value: String), Web(query) }`; `object SearchEngine { fun search(q: String, apps: List<AppEntry>, shortcuts: List<ShortcutInfo>): List<SearchResult> }`; `object Calculator { fun eval(expr: String): Double? }` (+ − × ÷ * / ^ % parentheses, decimals; invalid → null); `@Composable fun SearchPage(...)` glass search field with keyboard auto-open
- Ranking: label prefix > word prefix > substring > package substring; case-insensitive; Thai matches by substring (Review Focus 3); calculator result first when `eval != null` and query contains an operator; `Web` always last.
- [ ] **Step 1:** Tests: "ไล" finds "ไลน์"; "TEL" finds "Telegram" before "Hotel Finder"; "2+3×4" → 14; "(1+2" → null; "5/0" → null.
- [ ] **Step 2–4:** FAIL → implement → PASS. **Step 5:** Commit `feat(search): search page`.

### Task 11: Home UI, Dock, edit mode, folders, gestures

**Files:**
- Create: `home/HomeScreen.kt`, `home/Dock.kt`, `home/EditMode.kt`, `home/IconMenu.kt`, `home/FolderPopup.kt`, `home/Gestures.kt`, `XeApp.kt`
- Test: `home/GestureResolverTest.kt`, `home/HomeScreenshotTest.kt`

**Interfaces:**
- Consumes: `HomeRepository`, `AppDrawer`, `SearchPage`, `WallpaperLayer`, `IconRepository`, `AppSource`
- Produces: `object GestureResolver { fun resolve(dx: Float, dy: Float, velocityY: Float, pointers: Int, s: XeSettings): GestureAction? }` (threshold 48.dp or fling 900 dp/s); `fun expandNotificationShade(context)` via `StatusBarManager` reflection `expandNotificationsPanel` with permission `EXPAND_STATUS_BAR`, no-op on failure
- Behaviour: horizontal pager of pages with parallax wallpaper (10% of page width); drawer is a vertical sheet following the finger (blur and dim ramp 0→1, home scales 1→0.94); app launch passes icon bounds to `ActivityOptions.makeClipRevealAnimation`; long-press icon → glass menu springs from the icon (shortcuts, edit icon [opens style picker for that app], move to category, hide, app info, uninstall); long-press empty → edit mode (±2° wiggle 140 ms, drag with others yielding by spring, bottom glass bar: Widgets [disabled "เร็ว ๆ นี้" until Phase 2], Wallpaper, Settings); folder opens from its bounds into centred glass panel with editable name; back press closes overlays in order; HOME intent while on Home closes everything and scrolls to page 0.
- [ ] **Step 1:** `GestureResolverTest` for up/down/two-finger/short-move-null. Screenshot tests: Home with dock, open folder, icon menu, edit mode.
- [ ] **Step 2–4:** FAIL → implement → PASS. **Step 5:** Commit `feat(home): home screen, dock, folders, gestures`.

### Task 12: Settings screens, backup and update check

**Files:**
- Create: `settings/ui/*.kt`, `backup/BackupManager.kt`, `update/UpdateChecker.kt`
- Test: `backup/BackupManagerTest.kt`, `update/UpdateCheckerTest.kt`, `settings/ui/SettingsScreenshotTest.kt`

**Interfaces:**
- Produces:
  - Nav routes `settings`, `settings/wallpaper`, `settings/appearance`, `settings/icons`, `settings/animation`, `settings/gestures`, `settings/backup`, `settings/pages/home`, `settings/pages/apps`, `settings/pages/search`, `settings/hidden`, `settings/about`
  - `class BackupManager(db, settings: SettingsStore, wallpaper: WallpaperController) { suspend fun export(out: OutputStream); suspend fun import(input: InputStream): Result<Unit> }` — `.xebackup` = zip of `settings.json`, `db.json` (categories, overrides, home items, folders, launch stats), `wallpaper.png`; version field `1`
  - `object UpdateChecker { suspend fun latest(client): ReleaseInfo?; fun isNewer(remote: String, local: String): Boolean }` using `https://api.github.com/repos/XenoORIGIN/xe-launcher/releases/latest`; `ReleaseInfo(tag: String, apkUrl: String, notes: String)`
- Settings Home layout (Large Title "ตั้งค่า"/"Settings", glass rows with coloured outline icons, matching the owner's Smart Launcher reference): Wallpaper, Appearance, Icons, Animation, Gestures, Xe Studio (row disabled with "เร็ว ๆ นี้" until Phase 3), Backup, section "หน้า/Pages": Home screen, App page, Search page; About. The notification-highlight card is hidden until Phase 2.
- Every control writes through `SettingsStore.update` and applies live (theme, glass, icons, animation speed, language via `AppCompatDelegate.setApplicationLocales`).
- [ ] **Step 1:** Tests: backup export→import into empty DB reproduces categories, home items and settings; corrupt zip → `Result.failure` and nothing changed; `isNewer("v1.0.12","1.0.9") == true`, `isNewer("v1.0.9","1.0.9") == false`.
- [ ] **Step 2–4:** FAIL → implement → PASS; screenshots of every settings screen in TH and EN.
- [ ] **Step 5:** Commit `feat(settings): settings screens, backup, update check`.

### Task 13: Localisation, app icon and first run

**Files:**
- Create: `res/values/strings.xml`, `res/values-th/strings.xml`, `res/xml/locales_config.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml` + layers, `onboarding/SetDefaultHome.kt`
- Test: `LocalisationTest.kt`, `onboarding/SetDefaultHomeTest.kt`

**Interfaces:**
- Produces: `@Composable fun SetDefaultHomeCard(onRequest: () -> Unit)` shown on first run when Xe isn't the default home; request via `RoleManager.createRequestRoleIntent(ROLE_HOME)`
- App icon: dark glass squircle, white "Xe", subtle rim highlight; monochrome layer for themed icons.
- [ ] **Step 1:** `LocalisationTest`: every key in `values` exists in `values-th` and vice versa (parse XML). First-run test: when not default → card shown; after `firstRunDone` → not shown.
- [ ] **Step 2–4:** FAIL → implement → PASS. **Step 5:** Commit `feat: TH/EN strings, app icon, first run`.

### Task 14: Website and Pages deploy

**Files:**
- Create: `website/index.html`, `website/style.css`, `website/app.js`, `website/assets/*` (screenshots from Roborazzi goldens, app icon), `.github/workflows/pages.yml`
- Test: `website/check.mjs` (Playwright: page loads, download button href equals stable APK URL, no horizontal overflow at 360px width, TH/EN toggle swaps text)

- Content: Liquid Glass on black, Inter + Noto Sans Thai; hero with phone screenshot; features (Liquid Glass, ไอคอนกระจกดำ, หมวดแอป, แอนิเมชัน, Xe Studio "เร็ว ๆ นี้", Widget "เร็ว ๆ นี้"); download button → stable APK URL with version fetched from GitHub API (falls back to plain button); Samsung install steps (1 ดาวน์โหลด, 2 อนุญาตติดตั้งแอปที่ไม่รู้จัก, 3 ติดตั้ง, 4 ตั้งค่า > แอป > เลือกแอปเริ่มต้น > แอปหน้าหลัก > Xe Launcher); "Android เท่านั้น" note; TH default, EN toggle.
- `pages.yml`: on push touching `website/**` or after `android.yml` succeeds; deploy `website/` with `actions/deploy-pages`.
- [ ] **Step 1:** Write `check.mjs`; run → FAIL. **Step 2:** Build site; run → PASS; screenshot at 390×844 and inspect.
- [ ] **Step 3:** Commit `feat(website): download site`.

### Task 15: Release verification

- [ ] **Step 1:** Push to `XenoORIGIN/xe-launcher` `main`; wait for `android.yml` green; confirm release has `xe-launcher.apk` and that `releases/latest/download/xe-launcher.apk` returns 302.
- [ ] **Step 2:** Confirm `pages.yml` deployed and `https://xenoorigin.github.io/xe-launcher/` loads with working download button.
- [ ] **Step 3:** Run full suite `./gradlew testDebugUnitTest verifyRoborazziDebug` → PASS.
- [ ] **Step 4:** Whole-branch review (fresh reviewer) against spec + Review Focus.
