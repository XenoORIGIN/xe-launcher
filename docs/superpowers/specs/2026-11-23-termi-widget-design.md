# Termi pet widget — design

Date: 2026-11-23 · Owner: Kantaphon · Status: draft for approval

## Intent

A Tamagotchi-style pet on Xe's Home screen. The pet is **Termi**, Xe's own chunky pixel character with a
terminal-window body, a screen for a face, short thick arms and two legs. Kantaphon codes, runs his
server and plays games all on his phone, so the pet must **never punish heavy phone use**. Termi grows when
he looks after it, when he uses the apps he calls "creator" apps (coding, AI, GitHub…), and when he leaves
the phone alone at night. It should feel alive: lots of vibe-coder animations, expressive faces, and the
ASCII night-lake scene (built in 1.0.16) behind it.

Concept art and animation already approved: `xebi/lake/termi_lake.py` (face set, poses, emotions, effects)
and the reel/sprite package sent to Kantaphon (`termi_animations.zip`). The app draws Termi in code from
the same pixel definitions; it does not ship those sprite sheets.

## Decisions Kantaphon made

- Care: tap to feed / play / sleep; creator-app use; resting at night. Neglect makes Termi hungry or lonely,
  it **never dies and never loses levels**.
- Widget: one Xe widget, resizable, three layouts by size — **2×2** Termi on the scene only; **4×2** scene,
  Termi and three small buttons (feed, play, sleep); **4×4** scene, Termi, three status bars and the buttons.
  Tapping Termi at any size opens a **full-screen Termi page**.
- Growth stages by dev rank: **Junior** (glasses) → **Senior** (headphones + hoodie) → **Lead** (crown and
  little monitors around it). Pace: **Senior after 3 days, Lead after 6 days** of regular care.
- Accessories, unlocked by level: **hats** (cap, beanie, party hat, wizard hat), **glasses** (sunglasses,
  round glasses, VR headset), **held items** (coffee, keyboard, rubber duck, phone).
- **No notifications.** Termi's face and pose show how it feels.
- Termi is drawn in code inside Xe (approach A), so its colour follows the theme and every accessory works on
  every pose and stage.

## Calls I made (Kantaphon can overrule)

- Night window 00:00–06:00, adjustable on the Termi page.
- Creator apps start as the installed apps that match a built-in list (Claude, ChatGPT, Gemini, GitHub,
  Termux, Acode, Spck, Dcoder, Replit, Juice SSH, Pterodactyl/Mcsmanager-style admin apps…); he can tick or
  untick any app.
- Termi's body colour = theme accent (`settings.accent`), lifted to stay visible; the face screen stays dark.
  Default Xe Black accent is white, matching the approved concept.
- The scene behind Termi is the ASCII Lake following the real time of day, in the chosen ASCII colour mode.

## Builder-check answers (my calls)

- **Art for stages and accessories** is drawn new in the same chunky style; preview sheet
  `2026-11-23-termi-items.png` goes to Kantaphon for approval with this design.
- **Slots:** Junior glasses → glasses slot; Senior headphones → head slot, hoodie = body trim (always shown);
  Lead crown → head slot, little monitors = effect around Termi (always shown). A worn hat replaces the
  headphones/crown, worn glasses replace Junior's glasses.
- **Held items** show only in poses where the right mitt is free ("down"/"hold"); animations that use their
  own prop (coding keyboard, coffee mug, phone, bug squash) hide the held item for that animation.
- **Night window = asleep.** Termi sleeps through it (sleep animation, energy refills +20/h, no XP from
  feed/play during it). Feed/play wake it up.
- **Taps:** tapping Termi always opens the page (the page has a Wake/Sleep button). The widget's sleep
  button shows "Wake" while asleep.
- **Scene props** from the concept (keyboard, bowl, bubbles, confetti, clouds, fireflies, bug) are ported
  with the animations; the sprite box is 50 × 40 units like the concept. Pixel unit per widget =
  whole pixels so Termi's 14-unit body is about 35% of the widget width.
- **Sizes:** TERMI minimum 2×2 (1×1 blocked). Layout by span: width ≥ 4 and height ≥ 4 → full; width ≥ 3 →
  buttons; otherwise Termi only.

## Goals

1. Widget kind `TERMI` ("xe_termi", default 4×2, presets 2×2 / 4×2 / 4×4) using the existing resize system.
2. A persistent pet model with needs, XP, levels, stages and accessories, fair to heavy phone users.
3. Animated Termi drawn in Compose at about 12 fps while Home is visible; still when Xe is hidden, animations
   are off, or power-save is on.
4. Full-screen Termi page: big scene, level + XP, the three needs, care buttons, wardrobe, creator-app picker,
   night window.
5. Strings in English and Thai.

## Non-goals

- Notifications, sounds, multiple pets, trading/online features, Xe Studio integration, Android
  system (AppWidget) version outside Xe.
- A "death" or level-loss mechanic.

## Anti-goals

- Anything that makes Termi sad because the phone was used a lot.
- Battery or scroll jank from the animation (same rules as the ASCII layer: off-main computation is not
  needed for a ~100×80 px sprite, but drawing must stop when Xe is hidden).
- Copying the look of any existing mascot. Termi stays the approved original design.

## Pet model

State is a small serializable record saved in its own file (`filesDir/termi.json`), not in `XeSettings`
(changes several times a day; keeps settings writes quiet).

- **Needs** 0–100: `food`, `fun`, `energy`. Each decays with real time since `updatedAt`
  (food −6/h, fun −8/h while awake, energy −5/h awake / +20/h asleep). Computed lazily on read — no
  background job.
- **Mood** derived: hungry if food < 30, lonely if fun < 30, sleepy if energy < 25 or inside the night window,
  happy if all ≥ 60, else okay.
- **Actions** (with cooldowns so tapping spam doesn't farm XP):
  - Feed: food +35, +10 XP, XP only once per 2 h.
  - Play: fun +35, energy −5, +10 XP, XP once per 2 h.
  - Sleep: Termi naps (asleep for 45 min or until woken by a tap); energy refills while asleep; +10 XP once
    per day.
- **Creator apps:** with Usage access granted, every time Home is shown Xe reads today's foreground time
  of the chosen creator apps (`UsageStatsManager.queryAndAggregateUsageStats(dayStart, now)`), credits the
  new minutes since the last reading: +1 XP per 2 minutes, at most 60 XP per day; also fun +1 per 2 min.
  Without Usage access this path is simply off and the page offers the existing "grant usage access" flow.
- **Night rest:** once per day after the window ends, Xe checks screen-interactive time inside the window
  (`UsageStatsManager.queryEvents`, `SCREEN_INTERACTIVE`/`SCREEN_NON_INTERACTIVE`). Under 15 minutes of
  screen time → "rested": +40 XP and energy to 100. Over → nothing lost, Termi is just a bit sleepy.
  Needs Usage access too; without it, no night bonus.
- **XP, levels, stages:** level up every 60 XP (level 1 at 0). Junior 0 XP, **Senior at 400 XP**, **Lead at
  840 XP** (regular care ≈ 140 XP/day → day 3 and day 6). Level cap 15 (all accessories unlocked by then).
- **Accessory unlocks** (level → item): 2 cap, 3 coffee, 4 sunglasses, 5 beanie, 6 keyboard, 7 round glasses,
  8 party hat, 9 rubber duck, 10 VR headset, 11 phone, 12 wizard hat. One item per slot can be worn; a worn
  hat/glasses replaces the stage's own item in that slot.

## Drawing

- `TermiSprite` (pure Kotlin, no Android): body, 25 face expressions, arm poses, legs, squash/stretch, stage
  items, accessories and effects, all as unit-grid rectangles exactly as in `termi_lake.py`. Output: a list of
  coloured rects for a given `TermiPose` (struct of face, arms, legs, squash, lean, dx, dy, screen colour,
  effects) plus stage and accessories.
- `TermiAnimations`: the 13 approved animations as functions `(t: Float, frame: Int) -> TermiPose`
  (raft, hello, coding, prompt AI, think, coffee, music, build fail, bug squash, build pass, love, hungry,
  sleep) plus a calm idle loop.
- Which animation plays: idle by default; mood overrides (hungry/sleep/love when happy); a random vibe-coder
  animation every 20–40 s when idle; right after an action its animation (feed → hungry/nom, play → random of
  music/bug squash/hello, sleep → sleep); after creator minutes were credited, "coding" then "build pass".
- `TermiView` composable draws scene (AsciiField landscape grid, LAKE + live look) + sprite with integer pixel
  scale and `FilterQuality.None`; 12 fps loop gated by `skyLoopRuns(animate, lifecycle)`.

## Widget layouts

- 2×2: scene + Termi centred.
- 4×2: scene + Termi on the left two-thirds; three round glass buttons (feed, play, sleep) stacked on the right.
- 4×4: scene + Termi on top half; three labelled bars (Food / Fun / Energy) and a row of the three buttons;
  level and stage name in a small caption.
- Tap on Termi (or anywhere outside buttons) → opens the Termi page. Buttons act directly and play the
  matching animation. In Home edit mode taps do nothing (as other widgets).

## Termi page

Full-screen overlay like Search: header (name "Termi", stage, level), large `TermiView` (scene behind),
XP bar to next level and to next stage, three needs bars, the three buttons, **Wardrobe** (three rows: Hats,
Glasses, Held — locked items show "Lv N"), **Creator apps** (list with switches + usage-access prompt),
**Night rest** window (two time pickers). Back or swipe down closes.

## Code layout

New package `termi/`: `TermiState.kt` (model + rules, pure), `TermiStore.kt` (load/save JSON, actions,
creator/night crediting with injected clock and usage sources), `TermiSprite.kt`, `TermiAnimations.kt`,
`TermiView.kt`, `TermiWidget.kt` (span layouts), `TermiScreen.kt` (page). `GlassWidgetKind.TERMI` added; its
`GlassWidget` branch draws `TermiWidget`. `AppGraph.termi: TermiStore`. `WidgetActions.openTermi` opens the
page from XeApp.

## Testing

- Rules: decay over time, cooldowns, XP caps, stage thresholds (400/840), level cap, unlock table, mood
  derivation, never negative, never level loss.
- Creator crediting with a fake usage source: only chosen apps count, per-day cap, new day resets, no double
  credit across readings.
- Night rest with fake events: under/over 15 min, once per day, no Usage access → no bonus.
- Sprite: every face and every accessory draws inside the sprite box; stage items present per stage.
- Widget: each span shows the right parts (tags), buttons call the store, tap opens the page.
- Page: wardrobe locks/unlocks by level, equipping changes state, creator switches persist.
- Screenshots: widget at 2×2, 4×2, 4×4 and the page.

## Left to the builder

Exact decay constants within the stated rates, button icons, bar styling, page layout details, the built-in
creator package list, how the random animation is picked.
