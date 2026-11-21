# Xe Studio — Design

Date: 2026-11-21 · Owner: Kantaphon · Status: awaiting owner approval

## Intent

Xe Studio is a separate Android app for making home-screen customisation:
icon themes, wallpapers and widgets. It is the creative companion to
Xe Launcher. Its own app does one job: making these things. What gets
made is used in Xe Launcher with one tap, saved to the cloud under the
user's account, and can be published to a community theme store where
other people browse, like and download it.

Kantaphon builds and tests on his phone only (Samsung Galaxy A16 5G, and an
OPPO). Distribution is a signed APK on GitHub Releases, the same as Xe Launcher.

## Goals

- Make a complete icon theme in minutes, then perfect single icons by hand.
- Make wallpapers (static and moving) and free-form widgets on the phone.
- One-tap "Use in Xe": icons, wallpaper and widgets change in Xe Launcher right away.
- Accounts with Google sign-in (plus email), cloud save across devices.
- Community store: publish, browse, like, download, report.
- Beautiful, smooth animation throughout.

## Non-goals (first release)

- Weather data in widgets (needs an outside weather service and location permission; later).
- Exporting a standard icon-pack APK for other launchers (Samsung One UI, OPPO, Nova).
  User-made icons only apply in Xe Launcher. Wallpapers and widgets work everywhere.
- Paid themes, comments, follows, a web version of the store.
- iOS.

## Anti-goals

- Studio must not slow down or break Xe Launcher. Xe keeps working if Studio is
  uninstalled or the cloud is down (it keeps the last applied theme).
- Cloud being paused/offline must never block creating or applying locally.
- No inappropriate content left up in the store: the owner can remove anything.
- No surprise costs: the backend stays on the free plan without a payment card.

## Constraints

- Android, Kotlin + Jetpack Compose, minSdk 29, targetSdk 36 — same stack as Xe Launcher.
- Package `com.xenoorigin.xestudio`. Signed with the same release key as Xe Launcher
  (cert SHA-256 ea0e6430…a09c68), so the two apps can trust each other with a
  signature-level permission.
- Backend: Supabase free plan (decided by owner). Limits: 1 GB file storage,
  5 GB egress/month, 500 MB database, 50,000 monthly users; the project pauses
  after 7 days without traffic and must be resumed by hand. The owner's org has one
  active project (open-wheel-gp); free plan allows two, so Xe Studio gets the second.
- Google sign-in needs an OAuth client in Google Cloud created by the owner from his
  phone browser (free, no card). Builder writes the step-by-step guide in Thai.
- UI text in Thai and English, Thai first, matching Xe Launcher's string setup.
- Tested with Robolectric/Compose tests and Roborazzi screenshots, like Xe Launcher.

## Approaches considered (backend)

- **Supabase free (chosen).** No card; auth, Postgres and storage in one; Claude can
  set it up through the connected Supabase account. Cost: 7-day pause and small egress.
- Firebase Blaze. Never pauses, bigger free quota, easiest Google sign-in. Rejected:
  needs a credit card and has no hard spending cap.
- Firebase Spark (free). Never pauses. Rejected: no file storage, so themes with the
  user's own photos could not sync or be shared.

Key idea used by all of them: creations are stored as **recipes** (small JSON describing
shapes, colours, layers), not rendered images. A theme is a few KB. Only images the user
imports (photos, a picture placed on an icon or in a widget) are stored as files,
re-encoded to WebP and capped in size. This keeps the free quota comfortable.

## The app

### Look and feel (owner's choice: monochrome + Xe glass panels)

- Monochrome minimal: black, white and greys. The only real colour on screen is the
  user's own work, so it stands out.
- Tool panels, sheets and the bottom bar use the frosted-glass style of Xe Launcher
  (Haze), over a blurred live preview of the work.
- Spring-based motion everywhere: shared-element transitions from a store card or
  project to its editor, panels that settle with spring physics, icons that re-style
  with a staggered ripple when a setting changes, a press squish on every control.
- App icon: a rounded, chunky pen, black and white (adaptive icon + monochrome layer).

### Structure

Bottom bar with four places:

1. **Create** — start a new Icon theme, Wallpaper or Widget; recent drafts.
2. **Store** — community themes: Popular, New, and search; detail page with preview,
   likes, download count, "Use in Xe", "Save to my stuff", "Report".
3. **My stuff** — everything I made or saved, synced across devices when signed in.
4. **Profile** — sign in/out, my published themes, settings, link to Xe Launcher.

A **theme** in the store bundles any of: an icon style, a wallpaper, and widgets.
Each part can also be published or applied alone.

### Icon editor (owner's choice: both auto-style and per-icon)

- Live preview grid of the phone's real installed apps.
- Whole-set controls: shape (circle, squircle, rounded square, hexagon, flower, etc.),
  background colour or gradient, icon colour treatment (original, monochrome, single
  tint, duotone), outline width/colour, shadow, padding/scale.
- Tap any icon to override just that one: draw over it, pick an image from the gallery,
  or use a letter/symbol. Overrides are keyed by app package (and activity).
- Apps installed later automatically get the whole-set style.

### Widget editor (owner's choice: free-form layers, KWGT-like)

- Empty canvas sized to a widget; add layers: text, shape, image, icon.
- Drag, resize, rotate, reorder layers; alignment guides and snapping.
- Live data inside text: time, date, battery, now playing (via media session; needs the
  notification-listener permission, with the restricted-settings guide shown in-app).
- Studio provides one Android AppWidget provider, "Xe Studio widget". The user places it
  (in Xe or any launcher) and picks which design it shows. Rendering uses Jetpack Glance or
  a bitmap rendered from the recipe; updates at least once a minute for time, on battery
  change, and on media change.

### Wallpaper editor (owner's choice: everything)

- Same layer system as widgets: gradient, geometric pattern, waves, dots, the user's photo.
  Per-layer blur, colour adjust, grain, black and white, crop to screen.
- Shake the phone to randomise the current generator layer.
- Moving wallpaper option: slow colour flow, or parallax with phone tilt. Delivered as a
  `WallpaperService` live wallpaper. Stops moving in battery saver or under 15 % battery.
- Static wallpapers are rendered at screen size and set through WallpaperManager
  (home, lock or both).

## Linking with Xe Launcher

- Studio exposes a `ContentProvider` (`com.xenoorigin.xestudio.themes`) protected by a
  signature permission; Xe Launcher reads the applied theme's recipe and any imported
  images from it.
- "Use in Xe" writes the applied theme and sends a broadcast (also signature-protected);
  Xe re-renders icons immediately and caches the result, so it keeps working offline or
  if Studio is removed.
- Xe Launcher gets a new "Themes from Xe Studio" entry in settings that opens Studio, or
  links to the download page if Studio is not installed. Xe also needs a new icon renderer
  that turns an icon recipe + the app's own icon into the styled icon.
- Icon precedence in Xe (builder's call): a Studio theme appears as one more choice,
  "From Xe Studio", in Xe's existing icon-style list (next to GLASS_DARK, MONO…). Picking another
  style switches back. A per-app custom picture set in Xe still wins over the theme for that app.
- Code sharing: Studio lives in its own repo but the recipe schema and renderers are one
  `studio-core` module used by both apps, so a theme looks identical in both.
- Wallpaper: Xe draws its own wallpaper (`windowShowWallpaper=false`; `WallpaperController`
  renders it so the glass can blur it), so a system or live wallpaper set by Studio would not
  show inside Xe. Decision (builder's call, keeps Xe's glass): Xe renders Studio wallpapers itself
  from the recipe, using a shared rendering module (`studio-core`) that both apps build from,
  including moving wallpapers. Studio also sets the system wallpaper / live wallpaper so the lock
  screen and other launchers show it.
- Widgets: Xe already hosts AppWidgets; Studio's widget is placed like any other.
- Xe's existing Settings > "Xe Studio" page (Wallpaper Studio, Icon Studio, widgets help) and
  its built-in tools move into the new app (owner's decision). The Xe entry becomes a shortcut
  that opens Xe Studio, or its download page if not installed. Users' existing Xe wallpaper and
  custom icons keep working; nothing they made is lost.
- Both apps ship together; the release notes say to update both.

## Accounts and cloud

- Supabase Auth: Google sign-in (Android Credential Manager → ID token → Supabase
  `signInWithIdToken`) and email + password.
- Browsing the store works signed out. Saving to the cloud, liking and publishing need sign-in.
- Data: `profiles` (display name, avatar), `projects` (owner, kind, recipe JSON, updated_at),
  `published_themes` (author, title, tags, recipe, preview image, likes, downloads, hidden flag),
  `likes`, `reports`. Row-level security: users edit only their own rows; everyone reads
  non-hidden published themes.
- Storage buckets: `previews` (public, small WebP) and `user-images` (images inside recipes).
- Sync: local-first. Everything is saved on the device first and uploaded when online;
  the newest `updated_at` wins on conflict.
- Keep-alive (owner's decision): a free scheduled GitHub Actions job makes a light read
  request to Supabase every few days so the free project never reaches the 7-day pause.
- When the Supabase project is paused or unreachable, the Store and sync show a calm
  "Cloud is resting, your work is safe on this phone" state; creating and applying still work.

## Store moderation

- Store preview images use a fixed set of sample apps, never the author's installed apps
  (builder's call, protects privacy).
- Report button on every theme (reason picker).
- The owner's account is admin (flag in `profiles`). Admins see a Reports list in Profile
  and can hide or delete a theme. A theme with 3+ reports is hidden automatically until reviewed.
- Uploaded images are size-limited (e.g. preview ≤ 300 KB, user image ≤ 1 MB after WebP).

## What the owner must do by hand

1. Create a Google Cloud OAuth client (Android type with the release-key SHA-1, plus a Web
   client ID that Supabase needs). Builder writes a Thai, phone-friendly guide.
2. Paste the client ID into Supabase (or let Claude set it via the connected account).
3. Install Xe Studio and the matching Xe Launcher update.

## Left to the builder

- Module layout, data classes and the recipe JSON schema (versioned).
- Exact animation curves and durations within the spring-motion direction above.
- Widget rendering approach (Glance vs bitmap) and update scheduling details.
- Icon shapes list and default styles; store sorting formula for "Popular".
- How releases of the two apps are coordinated in CI.
- Order of building, as long as each step ships something testable on the phone.
