# Xe Launcher — "Polish" release (1.0.14) — Design

Date: 2026-11-22 · Owner: Kantaphon · Status: approved 2026-11-22 (unique touches: all four; lock screen: refresh on screen-on)

## Intent

Kantaphon uses Xe every day on a Samsung Galaxy A16 5G (and an OPPO). He wants this release to make
Xe feel finished and special: fix what's broken (category editor, live sky), make widgets flexible and
easy to arrange, make everything smoother and prettier, add a few ideas no other launcher has, rank apps
by time actually spent in them, and give the update alert a sound. He chose to ship it all in **one
version** (answer 5C).

## Owner's answers (2026-11-22)

1. Live sky bugs: **A** colours don't match the real time, **B** it doesn't change until Xe is opened again.
2. Widget editing: **C** both, free drag + corner handles with a live "4×2" label, and size presets.
3. Glass refraction on icons: **C** both a light version and a real lens version, chosen in settings.
4. Update alert sound: **A** a short Xe glass chime.
5. Order: **C** everything in one release.

## Goals

### 1. Bug fixes
- **Category editor** (screenshot 2026-11-22):
  - Category names are cut mid-word ("เดินทางและ", "งานและการ") with no ellipsis.
  - A row can stay enlarged and shifted after a drag ("แอปอื่น").
  - Fix both, and make reordering feel smooth: neighbours slide out of the way on a spring instead of
    jumping, and the lifted row casts a soft glass shadow.
  - The owner also says the editor "can't be customised much". Add, per category:
    - an accent colour, used by the category bar glow and Bento cards
    - how apps are sorted inside it: "follow the drawer sort" (default, so nothing changes for
      existing categories), A–Z, time spent, newest. The global drawer sort menu and its manual
      arrange screen stay as they are (builder's call).
  - Swipe a row left to hide or show it (the eye button stays).
- **Live sky**:
  - The sky follows the real time smoothly, minute by minute. The current 20-minute buckets go.
  - Sunrise and sunset are computed for today's date at the phone's time-zone latitude (Bangkok by
    default; no location permission). The current fixed 06:00 and 18:20 go.
  - It stays right while Xe isn't on screen, and on the lock screen. Xe offers a real Android live
    wallpaper, "Xe Live Sky": it draws the sky for the current minute every time it's shown, so it is
    never stale. Inside Xe the sky is drawn live too, with twinkling stars and slow drifting clouds.
  - The sky's **colours** also follow the computed sunrise and sunset. Keyframes are placed relative to
    sunrise and sunset rather than at fixed clock times, so dusk colours arrive when the sun actually
    sets (builder's call, covers bug A).
  - Picking Live Sky in Xe applies it inside Xe at once. A glass card then offers "Also use on the
    phone's wallpaper", which opens Android's live-wallpaper picker preselected on Xe Live Sky. This
    step is optional (builder's call).
  - When the system live wallpaper isn't chosen, a 15-minute background job refreshes the **home**
    static mirror. The **lock screen** bitmap is refreshed only when the screen turns on, and only when
    the sky has visibly changed. There is no lock-screen write while the phone is in use, so no flash
    (owner's answer 2B).

### 2. Widgets
- Every Xe glass widget gets responsive layouts for any size from 1×1 to 4×4. Small sizes show the
  essence (e.g. the clock shows just the time), larger ones show more (e.g. the clock adds date, next
  alarm and battery). Each kind declares a minimum size.
- New edit flow (owner's choice C), replacing the current width/height stepper sheet:
  - Long-press a widget to lift it. It wobbles slightly and a glass outline with **corner handles**
    appears. Drag the body to move it: a ghost shows where it will land, snapping to the grid.
  - Drag a handle to resize. A live label shows the size ("4×2"), and the widget re-lays itself out
    as the size changes. Spots that are taken show red and won't accept the drop.
  - A floating glass toolbar above the widget shows **size presets** (S, M, L, XL, named by the widget's
    best sizes), plus Remove and Done. Tapping outside finishes.
  - The same works for widgets from other apps, within each widget's own min/max size.

### 3. Look, feel and smoothness
- **Glass refraction on icons** (owner's choice C), set in Settings › Appearance › Icon glass: Off, Light
  (default) or Lens.
  - Light: the bright rim highlight slides across each icon's glass as the page scrolls or the phone
    tilts. It's cheap and works on every phone.
  - Lens: each icon bends the wallpaper behind it like a real glass lens. The bend grows with scroll
    speed and settles with a spring. It needs an AGSL RuntimeShader (Android 13+); older phones fall back to Light.
- **Prettier theme wallpapers**:
  - Re-render the built-in collection with richer depth: soft light leaks, bokeh glass orbs, finer grain,
    and no colour banding.
  - Add a "Liquid" set: flowing glass ribbons, a frosted aurora and chrome waves, at least 6 new.
  - Every theme in Settings › Themes picks one of the improved wallpapers.
  - If the wallpaper in use is a built-in preset, it is redrawn once in its new look after the update.
    The user chose the preset, not its old pixels. Imported photos are never touched (builder's call).
- **UI refresh**: one pass over the main surfaces for spacing, type and motion consistency: Home, the
  dock, the drawer header, folders, the settings list and sheets. Every sheet opens and closes on the
  same spring, and press feedback is the same everywhere.
- **Unique touches** (owner approved all four, answer 1E):
  1. Liquid dock: sliding a finger along the dock magnifies the icons under it, macOS-style, and lifting
     on one opens it.
  2. Depth parallax: the wallpaper shifts slightly behind the icons as pages swipe and the phone tilts,
     so Home has depth.
  3. Glass open: tapping an app grows a glass bubble from its icon into the app-launch animation, using
     Android's launch-from-view so it stays fast.
  4. Time-of-day tint: glass rims pick up a faint tint from the live sky, warm at dusk and cool at night,
     even when another wallpaper is used. It can be turned off.

### 4. Other
- **Rank by time spent**:
  - "Most used" becomes "Used longest", ranked by foreground time over the last 7 days from
    `UsageStatsManager`.
  - This needs the special "Usage access" permission. Xe asks for it once, with a guide card like the
    unread-badges one, and Android sends the user to its settings.
  - Until access is granted, ranking falls back to Xe's own launch counts.
  - This affects the Bento hero tile, "Suggested" widget and any "most used" sort.
- **Update alert sound**:
  - A short glass chime (≈0.8 s), generated in-house and shipped as `res/raw/xe_chime.ogg`.
  - Android fixes a channel's sound when the channel is created, so alerts move to a new channel,
    `updates_alert_v2`, and the old channel is deleted.
  - The test-alert button plays it too.
- **Website**: Xe Studio is marked "In development". Done 2026-11-22, before this design.

## Non-goals
- No Xe Studio changes in this release (Studio phase 2+ stays separate).
- No weather in widgets. No location permission.
- No new categories logic beyond per-category colour and sort.

## Anti-goals
- Nothing may make Home slower: the A16 must keep scrolling at 90 Hz. Lens mode is optional, and Light is the default.
- No battery drain from the live sky: no wake-ups beyond the 15-minute job, and the in-app sky animates only while visible.
- Widget editing must never lose a widget or leave two widgets overlapping.
- Existing layouts, categories and settings carry over untouched.

## Constraints
- Kotlin + Compose, minSdk 29, target 36; tests with Robolectric, Compose UI tests and Roborazzi.
- One APK release `v1.0.14` (Xe Launcher; Xe Studio APK re-attached unchanged or rebuilt at the same version).
- Thai and English strings. The release is signed with the existing key.

## Approaches considered

**Live sky correctness:**
- (a) A system live wallpaper (`WallpaperService`) that draws the sky on demand, plus a periodic job
  as a fallback mirror. This is the one chosen: it's always right, even on the lock screen.
- (b) Only a periodic job that sets a bitmap. Rejected: Android limits periodic work to 15 minutes, it
  wakes the phone, and Samsung flashes the screen on every set.
- (c) Only while Xe is open (today's behaviour). This is the bug.

**Widget editing:** free-form handles plus presets (owner's choice C). The stepper sheet is removed.

**Refraction:** a light highlight shift plus an optional AGSL lens (owner's choice C).

## Left to the builder
- Exact responsive layouts per widget and each kind's preset sizes.
- Sunrise/sunset formula (NOAA simplified) and the latitude per time zone (fallback 13.75°N).
- Chime synthesis (partials and envelope); the new wallpaper art.
- How the tasks are split, and the tests that pin each.
