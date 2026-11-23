# Termi Pet Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Tamagotchi-style Termi widget on Xe's Home with a full-screen page, growth stages, accessories and fair care rules.

**Architecture:** Pure-Kotlin rules (`TermiState`) and a store (`TermiStore`, JSON file, injected clock and usage sources); a pure sprite builder (`TermiSprite` → list of rects) driven by animation functions (`TermiAnimations` → `TermiPose`); a Compose `TermiView` drawing the ASCII Lake scene plus the sprite; widget layouts by span and a full-screen page.

**Tech Stack:** Kotlin, Compose 1.9, kotlinx.serialization, Robolectric SDK 34, Truth.

**Spec:** `docs/superpowers/specs/2026-11-23-termi-widget-design.md` (art: `2026-11-23-termi-items.png`; concept code `/home/claude/xebi/lake/termi_lake.py` and `items.py`)

## Global Constraints

- minSdk 29; no new dependencies; no notifications.
- Never death, never level loss, never a penalty tied to phone-use amount.
- XP: feed +10 / play +10 (each at most once per 2 h), sleep +10 once/day, creator apps +1 per 2 min (≤ 60/day), night rest +40 (screen < 15 min in window). Level every 60 XP, cap 15. Senior at 400 XP, Lead at 840 XP.
- Unlocks: 2 cap, 3 coffee, 4 sunglasses, 5 beanie, 6 keyboard, 7 round glasses, 8 party hat, 9 rubber duck, 10 VR, 11 phone, 12 wizard hat.
- Night window default 00:00–06:00; asleep inside it; feed/play wake, no XP inside it.
- Decay: food −6/h, fun −8/h awake, energy −5/h awake / +20/h asleep; needs clamped 0..100.
- TERMI widget: id `xe_termi`, default 4×2, min 2×2, presets 2×2 / 4×2 / 4×4.
- Animation ~12 fps only while `skyLoopRuns(animate, lifecycle)`.
- Strings English + Thai via `/home/claude/bin/addstr`. Tests via `/home/claude/bin/gt`.

## Review Focus

1. Phone clock going backwards or a very long absence (weeks) → needs clamp to 0, no crash, no negative XP. Test in Task 1.
2. Usage stats reset at midnight / app uninstalled mid-day → no double credit, no negative credit. Test in Task 2.
3. Corrupt or missing `termi.json` → fresh Termi, no crash. Test in Task 2.
4. Widget resized to odd spans (3×2, 2×4, 3×3) → a sensible layout, nothing clipped off. Test in Task 6.
5. Night window that does not cross midnight (e.g. 13:00–14:00) or equal start/end → correct asleep check. Test in Task 1.

---

### Task 1: Rules — `termi/TermiState.kt`

**Interfaces — Produces:**
- `@Serializable data class TermiState(food: Float = 80f, fun: Float = 80f, energy: Float = 80f, xp: Int = 0, updatedAt: Long = 0, lastFeedXpAt: Long = 0, lastPlayXpAt: Long = 0, lastSleepXpDay: Long = -1, napUntil: Long = 0, equipped: Map<Slot, Item> = emptyMap(), creatorApps: Set<String>? = null, nightStart: Int = 0, nightEnd: Int = 360, creditDay: Long = -1, creditedMs: Map<String, Long> = emptyMap(), creatorXpToday: Int = 0, lastNightDay: Long = -1)`
- `enum class Stage { JUNIOR, SENIOR, LEAD }`, `enum class Slot { HEAD, EYES, HAND }`, `enum class Item(val slot: Slot, val level: Int) { CAP(HEAD,2), COFFEE(HAND,3), SUNGLASSES(EYES,4), BEANIE(HEAD,5), KEYBOARD(HAND,6), ROUND(EYES,7), PARTY(HEAD,8), DUCK(HAND,9), VR(EYES,10), PHONE(HAND,11), WIZARD(HEAD,12) }`
- `enum class Mood { HAPPY, OKAY, HUNGRY, LONELY, SLEEPY, ASLEEP }`
- `object TermiRules { fun level(xp: Int): Int; fun stage(xp: Int): Stage; fun inNight(minute: Int, start: Int, end: Int): Boolean; fun decay(s: TermiState, now: Long, minuteOfDay: (Long) -> Int): TermiState; fun mood(s: TermiState, now: Long, minute: Int): Mood; fun feed(s, now, minute): TermiState; fun play(s, now, minute): TermiState; fun sleep(s, now, minute, day: Long): TermiState; fun wake(s, now): TermiState; fun unlocked(xp: Int): List<Item>; fun equip(s, item: Item?, slot: Slot): TermiState }`

- [ ] Tests `TermiRulesTest`: level(0)=1, level(59)=1, level(60)=2, level(10_000)=15; stage(399)=JUNIOR, stage(400)=SENIOR, stage(840)=LEAD; decay 10 h awake → food 20, fun 0 (clamped), energy 30; decay asleep 2 h → energy +40; clock backwards (now < updatedAt) → unchanged; 30 days → all 0 no crash; feed twice within 2 h → food +70 total but xp +10 once; feed inside night → no XP and napUntil cleared; mood thresholds; inNight across midnight (0–360 at 23:00 false, at 01:00 true; 1320–360 at 23:00 true), non-crossing (780–840), equal start/end → never; unlocked(60*4=240 → level 5) = [CAP, COFFEE, SUNGLASSES, BEANIE]; equip locked item → no change.
- [ ] Run, see compile FAIL; implement; run PASS; commit `feat(termi): care rules`.

### Task 2: Store — `termi/TermiStore.kt`

**Interfaces — Produces:** `class TermiStore(file: File, clock: () -> Long = System::currentTimeMillis, zone: () -> ZoneId = ZoneId::systemDefault, usageToday: (Set<String>) -> Map<String, Long>? = { null }, nightScreenMs: (start: Long, end: Long) -> Long? = { _, _ -> null }, defaultCreators: () -> Set<String> = { emptySet() })` with `val state: StateFlow<TermiState>` (decayed on read), `fun feed()`, `fun play()`, `fun toggleSleep()`, `fun equip(item: Item?, slot: Slot)`, `fun setCreator(pkg: String, on: Boolean)`, `fun setNight(start: Int, end: Int)`, `fun creatorApps(): Set<String>`, `fun onHomeShown(): Credit` (`data class Credit(val creatorXp: Int, val rested: Boolean)`), `val events: SharedFlow<TermiEvent>` (`FED, PLAYED, SLEPT, WOKE, CODED, RESTED, LEVEL_UP, STAGE_UP`). Saves atomically (`file.tmp` → rename) after each change. `CREATOR_DEFAULTS: Set<String>` list of package names (Claude `com.anthropic.claude`, ChatGPT `com.openai.chatgpt`, Gemini `com.google.android.apps.bard`, GitHub `com.github.android`, Termux `com.termux`, Acode `com.foxdebug.acode`/`com.foxdebug.acodefree`, Spck `io.spck`, Dcoder `com.paprbit.dcoder`, Replit `com.replit.app`, JuiceSSH `com.sonelli.juicessh`, Pterodactyl clients `com.pterodactyl.app`).

- [ ] Tests `TermiStoreTest`: persists and reloads; corrupt file → fresh state; creator credit: 10 min of a chosen app → +5 XP; reading again with same total → +0; total going down (new day or reset) → treat as new baseline, no negative; unchosen app ignored; 3 h in a day → capped 60; next day resets cap; night rest: 10 min screen → +40 XP & energy 100, once per day; 30 min → nothing; null source → nothing; level-up emits LEVEL_UP, crossing 400 emits STAGE_UP; default creators = installed ∩ CREATOR_DEFAULTS when `creatorApps == null`.
- [ ] RED, implement, GREEN, commit `feat(termi): store, creator and night credit`.

### Task 3: Sprite — `termi/TermiSprite.kt`

**Interfaces — Produces:** `data class Px(val x: Int, val y: Int, val w: Int, val h: Int, val argb: Int)` (units on a 50 × 40 box, Termi's feet centre at (25, 36)); `data class TermiPose(face: String = "neutral", mouth: String? = null, arms: Pair<String,String> = "down" to "down", armDy: Pair<Float,Float> = 0f to 0f, legs: Pair<String,String> = "stand" to "stand", squash: Int = 0, lean: Float = 0f, dx: Float = 0f, dy: Float = 0f, screen: Int = SCREEN, sit: Boolean = false, handFree: Boolean = true)`; `object TermiSprite { val FACES: Set<String>; fun build(p: TermiPose, stage: Stage, equipped: Map<Slot, Item>, body: Int): List<Px> }` — faces, arms, legs and body exactly as `termi_lake.py` (`Termi.draw`, `FACES`, `MOUTHS`); stage items and accessories as `items.py` (unit-converted). Fractional offsets are carried in Px as unit positions rounded to 1/3 unit (store x,y in thirds: `x3`, `y3`) — the view multiplies by unit/3.
- [ ] Tests `TermiSpriteTest`: every face in `FACES` builds and stays inside the box; each stage adds its item (glasses rects for JUNIOR, headphone colour for SENIOR, gold crown for LEAD); worn hat replaces crown; held item hidden when `handFree=false`; body colour used for body rects; dark accent lifted (max channel ≥ 160).
- [ ] RED, implement, GREEN, commit `feat(termi): sprite`.

### Task 4: Animations — `termi/TermiAnimations.kt`

**Interfaces — Produces:** `enum class TermiAnim(val seconds: Float) { IDLE(3f), HELLO(4.2f), CODING(5f), PROMPT_AI(5f), THINK(4.6f), COFFEE(5f), MUSIC(4.6f), BUILD_FAIL(6f), BUG_SQUASH(5.4f), BUILD_PASS(5f), LOVE(4.6f), HUNGRY(5.6f), SLEEP(6f), NAP(3f) }`; `data class Frame(val pose: TermiPose, val props: List<Px>, val scene: SceneHint)` (`SceneHint(rain: Float = 0f, dawn: Float = 0f, dim: Float = 0f)`); `object TermiAnimations { fun frame(a: TermiAnim, t: Float, i: Int): Frame }` — ports `termi_lake.py` scene functions (raft omitted), props in sprite units; `class AnimDirector(random: Random)` with `fun next(mood: Mood, event: TermiEvent?, nowMs: Long): TermiAnim` (event wins; ASLEEP → SLEEP/NAP loop; HUNGRY → HUNGRY; LONELY → LOVE-less idle; otherwise IDLE with a random vibe animation every 20–40 s from CODING, PROMPT_AI, THINK, COFFEE, MUSIC, BUG_SQUASH, BUILD_FAIL (rare, 1 in 10)).
- [ ] Tests `TermiAnimationsTest`: every anim at t ∈ {0, .25, .5, .75, .99} builds a sprite inside the box; BUILD_FAIL rain hint > 0 near end; BUILD_PASS dawn > 0; COFFEE/CODING/PROMPT_AI frames set handFree=false; director: FED → HUNGRY anim (eat), PLAYED → one of MUSIC/BUG_SQUASH/HELLO, CODED → CODING then BUILD_PASS, ASLEEP → SLEEP, idle picks a vibe anim after ≥ 20 s and not before.
- [ ] RED, implement, GREEN, commit `feat(termi): animations and director`.

### Task 5: View — `termi/TermiView.kt`

**Interfaces — Produces:** `@Composable fun TermiView(state: TermiState, anim: () -> TermiAnim, body: Color, ascii: AsciiColor, accent: Long, animate: Boolean, modifier: Modifier, onAnimEnd: () -> Unit = {})` — draws an `AsciiField` LAKE scene (landscape grid sized to the view, live look, `groundRow` = Termi's ground) then the sprite at a whole-pixel unit so the 14-unit body ≈ 35% of the width; `FilterQuality.None`; ~12 fps while `skyLoopRuns`; `TermiViewProbe.frames` for tests.
- [ ] Tests `TermiViewTest`: draws non-ground pixels in body colour; stops when animate=false (frames 1 after 1 s); animates when allowed (≥ 8 frames/s); hidden lifecycle → no frames.
- [ ] RED, implement, GREEN, commit `feat(termi): view`.

### Task 6: Widget — kind, layouts, actions

**Files:** `widgets/GlassWidgets.kt` (`TERMI("xe_termi", 4, 2, R.string.widget_termi)`, branch → `TermiWidget(span)`), `widgets/WidgetSizing.kt` (min 2×2, presets 2×2/4×2/4×4), `termi/TermiWidget.kt`, `widgets/WidgetActions` (`termi: TermiStore?`, `openTermi: () -> Unit`), `di/AppGraph.kt` (`termi` built with real usage sources), `XeApp.kt` (open page state, `onHomeShown()` on resume).
**Interfaces — Produces:** `@Composable fun TermiWidget(span: Span)`; layout rule `termiLayout(span): TermiLayout { SMALL, BUTTONS, FULL }` — `FULL` if x ≥ 4 && y ≥ 4, `BUTTONS` if x ≥ 3, else `SMALL`. Tags: `termi_view`, `termi_feed`, `termi_play`, `termi_sleep`, `termi_bars`.
- [ ] Tests `TermiWidgetTest`: layout rule for 2×2, 3×2, 4×2, 2×4, 3×3, 4×4; SMALL has no buttons; BUTTONS has 3 buttons and no bars; FULL has bars; feed click calls store (food up); sleep button text switches to Wake while asleep; tap on view calls openTermi; presets list = [2×2, 4×2, 4×4]; minimum clamp 1×1 → 2×2.
- [ ] RED, implement, GREEN, commit `feat(termi): Home widget`.

### Task 7: Page — `termi/TermiScreen.kt` + strings

**Interfaces — Produces:** `@Composable fun TermiScreen(store: TermiStore, apps: List<AppEntry>, usageAccess: Boolean, onGrantUsage: () -> Unit, onClose: () -> Unit, body: Color, ascii: AsciiColor, accent: Long, animate: Boolean)`. Sections: header (Termi, stage, level), TermiView, XP bar (`termi_xp`), needs bars, buttons, Wardrobe rows (`wear_<ITEM>`, locked show "Lv N"), Creator apps (`creator_<pkg>` switches; usage-access row if not granted), Night rest (start/end pickers using the existing time picker pattern or ±30 min steppers `night_start_minus/plus`, `night_end_minus/plus`).
- Strings (EN/TH): widget_termi "Termi"/"Termi"; termi_feed "Feed"/"ให้อาหาร"; termi_play "Play"/"เล่น"; termi_sleep "Sleep"/"นอน"; termi_wake "Wake"/"ปลุก"; termi_food "Food"/"ความอิ่ม"; termi_fun "Fun"/"ความสนุก"; termi_energy "Energy"/"พลังงาน"; termi_level "Level %1$d"/"เลเวล %1$d"; termi_junior "Junior"/"Junior"; termi_senior "Senior"/"Senior"; termi_lead "Lead"/"Lead"; termi_next_stage "%1$d XP to %2$s"/"อีก %1$d XP เป็น %2$s"; termi_wardrobe "Wardrobe"/"ตู้เสื้อผ้า"; termi_hats "Hats"/"หมวก"; termi_glasses "Glasses"/"แว่นตา"; termi_held "Held"/"ของถือ"; termi_locked "Lv %1$d"/"Lv %1$d"; termi_creators "Creator apps"/"แอปสายสร้าง"; termi_creators_desc "Using these apps helps Termi grow. Other apps never make it sad."/"ใช้แอปเหล่านี้แล้ว Termi จะโตเร็วขึ้น แอปอื่นไม่ทำให้มันเศร้า"; termi_usage_needed "Allow usage access so Termi can see creator apps and night rest"/"เปิดสิทธิ์ดูการใช้งาน ให้ Termi รู้ว่าใช้แอปสายสร้างและได้พักตอนกลางคืน"; termi_night "Night rest"/"เวลานอน"; termi_night_desc "Termi sleeps then. Leaving the phone alone earns a bonus."/"Termi จะนอนช่วงนี้ ถ้าไม่ได้ใช้มือถือจะได้โบนัส"; item names for all 11 items.
- [ ] Tests `TermiScreenTest`: locked item not clickable and shows Lv; unlocked item click equips; creator switch persists; usage row shown only without access; night stepper changes state by 30 min.
- [ ] RED, implement, GREEN; full suite green; screenshots `termi_widget_2x2/4x2/4x4.png`, `termi_page.png` written via SHOT_DIR and reviewed; commit `feat(termi): page`.
