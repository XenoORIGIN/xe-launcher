package com.xenoorigin.xelauncher.settings

import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.design.GlassQuality
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import kotlinx.serialization.Serializable

@Serializable enum class ThemeMode { LIGHT, DARK, AUTO }
@Serializable enum class AppLanguage { SYSTEM, TH, EN }
@Serializable enum class DrawerLayout { BENTO, GRID }

@Serializable enum class GestureAction { NONE, OPEN_DRAWER, NOTIFICATIONS, OPEN_SEARCH, OPEN_SETTINGS, EDIT_MODE, DRAW_LETTER }
/** Where the first-run welcome is up to; DONE once finished or skipped. */
@Serializable enum class OnboardingStage { INTRO, LANGUAGE, DEFAULT_HOME, SLIDES, PRACTICE, DONE }
@Serializable enum class DrawerSort { ALPHABETICAL, MOST_USED, RECENTLY_INSTALLED, CUSTOM }

@Serializable
data class XeSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: Long = 0xFFFFFFFF,
    val glassQuality: GlassQuality = GlassQuality.NORMAL,
    val fontScale: Float = 1f,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val iconStyle: IconStyle = IconStyle.GLASS_DARK,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconSizeDp: Int = 60,
    val showLabels: Boolean = true,
    val grayscaleContrast: Float = 1.15f,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val pressSquish: Boolean = true,
    val wallpaperParallax: Boolean = true,
    val homeColumns: Int = 4,
    val homeRows: Int = 6,
    val dockSize: Int = 4,
    val swipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDown: GestureAction = GestureAction.NOTIFICATIONS,
    val doubleTap: GestureAction = GestureAction.NONE,
    val twoFingerDown: GestureAction = GestureAction.OPEN_SEARCH,
    val drawerSort: DrawerSort = DrawerSort.ALPHABETICAL,
    val categoryBar: Boolean = true,
    val layoutLocked: Boolean = false,
    val hiddenApps: Set<String> = emptySet(),
    val wallpaperId: String = "xe_black",
    val firstRunDone: Boolean = false,
    val searchCalculator: Boolean = true,
    val searchWeb: Boolean = true,
    val searchAutoKeyboard: Boolean = true,
    /** Per-app icon style overrides, keyed by AppKey.encode(). */
    val iconOverrides: Map<String, IconStyle> = emptyMap(),
    /** Size of the symbol on its tile, 0.8..1.25 (1 = default). */
    val glyphScale: Float = 1f,
    /** Per-app custom names, keyed by AppKey.encode(). */
    val labelOverrides: Map<String, String> = emptyMap(),
    /** Built-in category set version already added to the database (see CategoryRepository). */
    val categoriesVersion: Int = 1,
    /** Hand-arranged app order (AppKey.encode() -> rank) used by DrawerSort.CUSTOM. */
    val appOrder: Map<String, Int> = emptyMap(),
    /** Categories kept out of the bar; their apps show in "Other". */
    val hiddenCategories: Set<Long> = emptySet(),
    /** Post a notification when a new version is on GitHub. */
    val updateNotify: Boolean = true,
    /** Release tag already notified about, so each version is announced once. */
    val lastNotifiedTag: String = "",
    /** Whether Xe already asked for the notification permission (Android 13+). */
    val askedNotifications: Boolean = false,
    /** Text of each note widget, keyed by its Home item id. */
    val widgetNotes: Map<String, String> = emptyMap(),
    /** Light on glass rims follows how the phone is tilted. */
    val tiltShine: Boolean = true,
    /** Hold an empty spot on Home and drag to move a glass lens over the wallpaper. */
    val glassLens: Boolean = true,
    /** Found the Supernova easter egg: its secret theme is listed. */
    val novaUnlocked: Boolean = false,
    /** First-run welcome: intro, language, default Home, tutorial. */
    val onboarding: OnboardingStage = OnboardingStage.INTRO,
    /** How each drawer page lays out its apps: Bento (big tile for the most used app) or a plain grid. */
    val drawerLayout: DrawerLayout = DrawerLayout.BENTO,
    /** Drawer icons slosh when the phone tilts and scatter when it's shaken. */
    val toyIcons: Boolean = true,
    /** Apps not opened for weeks gather dust that can be rubbed off. */
    val appDust: Boolean = true,
    /** When dust was switched on: no app is dustier than this (ms, 0 = not yet). */
    val dustSince: Long = 0L,
    /** When each app was last wiped clean, keyed by AppKey.encode(). */
    val dustWiped: Map<String, Long> = emptyMap(),
    /** Soft light in each category's own colour behind the drawer page. */
    val categoryGlow: Boolean = true,
    /** Dragging along the category bar swells the icons near the finger. */
    val barMagnify: Boolean = true,
    /** Tapping empty space in the drawer makes ripples and bubbles. */
    val pond: Boolean = true,
    /** Best score in the secret Bubble Pop game. */
    val bubbleBest: Int = 0,
    /** One-off upgrades for the playful features already applied (1 = double tap draws a letter). */
    val playVersion: Int = 0,
)
