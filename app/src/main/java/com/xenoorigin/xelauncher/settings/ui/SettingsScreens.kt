package com.xenoorigin.xelauncher.settings.ui

import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.offset
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import android.widget.Toast
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.xenoorigin.xelauncher.BuildConfig
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassQuality
import com.xenoorigin.xelauncher.design.GlassSheet
import com.xenoorigin.xelauncher.design.GlassSwitch
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeColors
import com.xenoorigin.xelauncher.design.XeRow
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.di.AppGraph
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.settings.AppLanguage
import com.xenoorigin.xelauncher.settings.DrawerSort
import com.xenoorigin.xelauncher.settings.GestureAction
import com.xenoorigin.xelauncher.settings.ThemeMode
import com.xenoorigin.xelauncher.settings.XeSettings
import com.xenoorigin.xelauncher.update.ReleaseInfo
import com.xenoorigin.xelauncher.update.UpdateChecker
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private typealias Update = ((XeSettings) -> XeSettings) -> Unit

@Composable
fun SettingsRoot(s: XeSettings, push: (String) -> Unit, onClose: () -> Unit, newVersion: String? = null) {
    SettingsPage(stringResource(R.string.settings), onBack = onClose) {
        if (newVersion != null) SettingsGroup {
            NavRow(stringResource(R.string.update_banner, newVersion.removePrefix("v")), R.drawable.ti_download, RowColors.Green) { push("about") }
        }
        SettingsGroup {
            NavRow(stringResource(R.string.themes), R.drawable.ti_sparkles, RowColors.Pink, subtitle = stringResource(R.string.themes_row_desc)) { push("themes") }
            NavRow(stringResource(R.string.wallpaper), R.drawable.ti_photo, RowColors.Green) { push("wallpaper") }
            NavRow(stringResource(R.string.appearance), R.drawable.ti_palette, RowColors.Orange) { push("appearance") }
            NavRow(stringResource(R.string.icons), R.drawable.ti_apps, RowColors.Blue) { push("icons") }
            NavRow(stringResource(R.string.animation), R.drawable.ti_sparkles, RowColors.Purple) { push("animation") }
            NavRow(stringResource(R.string.play_title), R.drawable.ti_device_gamepad_2, RowColors.Orange, subtitle = stringResource(R.string.play_row_desc)) { push("play") }
            NavRow(
                stringResource(R.string.badges_title), R.drawable.ti_bell, RowColors.Red,
                subtitle = stringResource(if (com.xenoorigin.xelauncher.badges.rememberBadgeAccess()) R.string.badges_row_on else R.string.badges_row_needs_access),
            ) { push("badges") }
            NavRow(stringResource(R.string.gestures), R.drawable.ti_hand_finger, RowColors.Red) { push("gestures") }
            NavRow(stringResource(R.string.xe_studio), R.drawable.ti_brush, RowColors.Pink, subtitle = stringResource(R.string.xe_studio_desc)) { push("studio") }
            NavRow(stringResource(R.string.backup), R.drawable.ti_device_floppy, RowColors.Cyan) { push("backup") }
        }
        SettingsGroup(stringResource(R.string.pages)) {
            NavRow(stringResource(R.string.home_screen), R.drawable.ti_home, RowColors.Blue, subtitle = stringResource(R.string.home_settings_desc)) { push("pages/home") }
            NavRow(stringResource(R.string.app_page), R.drawable.ti_layout_grid, RowColors.Indigo, subtitle = stringResource(R.string.app_page_desc)) { push("pages/apps") }
            NavRow(stringResource(R.string.search_page), R.drawable.ti_search, RowColors.Cyan, subtitle = stringResource(R.string.search_page_desc)) { push("pages/search") }
        }
        SettingsGroup {
            NavRow(stringResource(R.string.bug_report), R.drawable.ti_bug, RowColors.Red, subtitle = stringResource(R.string.bug_report_row_desc)) { push("bugs") }
            NavRow(stringResource(R.string.about), R.drawable.ti_info_circle, RowColors.Grey) { push("about") }
        }
    }
}

@Composable
fun AppearanceScreen(s: XeSettings, update: Update, back: () -> Unit) {
    SettingsPage(stringResource(R.string.appearance), back) {
        SettingsGroup(stringResource(R.string.theme)) {
            listOf(ThemeMode.DARK to R.string.theme_dark, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.AUTO to R.string.theme_auto).forEach { (m, l) ->
                ChoiceRow(stringResource(l), s.themeMode == m) { update { it.copy(themeMode = m) } }
            }
        }
        SettingsGroup(stringResource(R.string.accent)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                XeColors.Accents.forEach { c ->
                    val argb = c.value.toLong() ushr 32
                    val sel = s.accent == argb
                    Box(
                        Modifier.size(30.dp).background(c, CircleShape)
                            .border(if (sel) 3.dp else 1.dp, if (sel) LocalXeScheme.current.text else Color.White.copy(alpha = .2f), CircleShape)
                            .clickable { update { it.copy(accent = argb) } },
                    )
                }
            }
        }
        SettingsGroup(stringResource(R.string.glass_quality)) {
            listOf(
                Triple(GlassQuality.HIGH, R.string.glass_high, R.string.glass_high_desc),
                Triple(GlassQuality.NORMAL, R.string.glass_normal, R.string.glass_normal_desc),
                Triple(GlassQuality.SAVER, R.string.glass_saver, R.string.glass_saver_desc),
            ).forEach { (q, l, d) -> ChoiceRow(stringResource(l), s.glassQuality == q, stringResource(d)) { update { it.copy(glassQuality = q) } } }
        }
        SettingsGroup(stringResource(R.string.effects)) {
            XeRow(stringResource(R.string.tilt_shine), subtitle = stringResource(R.string.tilt_shine_desc)) { GlassSwitch(s.tiltShine, { v -> update { it.copy(tiltShine = v) } }) }
            XeRow(stringResource(R.string.glass_lens), subtitle = stringResource(R.string.glass_lens_desc)) { GlassSwitch(s.glassLens, { v -> update { it.copy(glassLens = v) } }) }
        }
        SettingsGroup {
            SliderRow(stringResource(R.string.text_size), s.fontScale, .85f..1.3f, steps = 8, display = { "${(it * 100).roundToInt()}%" }) { v -> update { it.copy(fontScale = v) } }
        }
        SettingsGroup(stringResource(R.string.language)) {
            listOf(AppLanguage.SYSTEM to R.string.lang_system, AppLanguage.TH to R.string.lang_th, AppLanguage.EN to R.string.lang_en).forEach { (lang, l) ->
                ChoiceRow(stringResource(l), s.language == lang) {
                    update { it.copy(language = lang) }
                    AppCompatDelegate.setApplicationLocales(
                        when (lang) { AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList(); AppLanguage.TH -> LocaleListCompat.forLanguageTags("th"); AppLanguage.EN -> LocaleListCompat.forLanguageTags("en") },
                    )
                }
            }
        }
    }
}

@Composable
fun IconsScreen(s: XeSettings, update: Update, back: () -> Unit, sample: com.xenoorigin.xelauncher.apps.AppEntry?, onBulk: () -> Unit) {
    SettingsPage(stringResource(R.string.icons), back) {
        SettingsGroup(stringResource(R.string.icon_style)) {
            com.xenoorigin.xelauncher.home.IconStylePicker(sample, s.iconStyle, s.iconShape, { st -> update { it.copy(iconStyle = st) } })
            Text(
                stringResource(
                    when (s.iconStyle) {
                        IconStyle.GLASS_DARK -> R.string.style_glass_dark_desc
                        IconStyle.MONO -> R.string.style_mono_desc
                        IconStyle.GLASS_COLOR -> R.string.style_glass_color_desc
                        IconStyle.GLASS_CLEAR -> R.string.style_glass_clear_desc
                        IconStyle.ORIGINAL -> R.string.style_original_desc
                        IconStyle.STUDIO -> R.string.style_studio_desc
                    },
                ),
                style = com.xenoorigin.xelauncher.design.XeType.Caption, color = com.xenoorigin.xelauncher.design.LocalXeScheme.current.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            NavRow(stringResource(R.string.bulk_icons), R.drawable.ti_palette, RowColors.Pink, subtitle = stringResource(R.string.bulk_icons_desc), onClick = onBulk)
        }
        SettingsGroup(stringResource(R.string.icon_shape)) {
            listOf(
                IconShape.SQUIRCLE to R.string.shape_squircle, IconShape.CIRCLE to R.string.shape_circle, IconShape.ROUNDED to R.string.shape_rounded,
                IconShape.SQUARE to R.string.shape_square, IconShape.TEARDROP to R.string.shape_teardrop,
            ).forEach { (sh, l) ->
                ChoiceRow(stringResource(l), s.iconShape == sh) { update { it.copy(iconShape = sh) } }
            }
        }
        SettingsGroup {
            SliderRow(stringResource(R.string.icon_size), s.iconSizeDp.toFloat(), 44f..76f, steps = 15, display = { "${it.roundToInt()} dp" }) { v -> update { it.copy(iconSizeDp = v.roundToInt()) } }
            SliderRow(stringResource(R.string.symbol_size), s.glyphScale, .8f..1.25f, steps = 8, display = { "${(it * 100).roundToInt()}%" }) { v -> update { it.copy(glyphScale = (v * 20).roundToInt() / 20f) } }
            SliderRow(stringResource(R.string.bw_contrast), s.grayscaleContrast, .8f..1.6f, steps = 7, display = { "%.1f".format(it) }) { v -> update { it.copy(grayscaleContrast = (v * 10).roundToInt() / 10f) } }
            XeRow(stringResource(R.string.show_labels)) { GlassSwitch(s.showLabels, { v -> update { it.copy(showLabels = v) } }) }
            if (s.iconOverrides.isNotEmpty()) XeRow(stringResource(R.string.reset_all_icons), icon = R.drawable.ti_refresh, onClick = { update { it.copy(iconOverrides = emptyMap()) } })
        }
    }
}

/** Xe Studio: one place for every creative tool. */
@Composable
fun StudioScreen(push: (String) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.xe_studio), back) {
        SettingsGroup {
            NavRow(stringResource(R.string.studio_wallpaper), R.drawable.ti_wallpaper, RowColors.Green, subtitle = stringResource(R.string.studio_wallpaper_desc)) { push("wallpaper") }
            NavRow(stringResource(R.string.studio_icons), R.drawable.ti_palette, RowColors.Pink, subtitle = stringResource(R.string.studio_icons_desc)) { push("icons") }
            NavRow(stringResource(R.string.bulk_icons), R.drawable.ti_apps, RowColors.Blue, subtitle = stringResource(R.string.bulk_icons_desc)) { push("bulk") }
            NavRow(stringResource(R.string.studio_widgets), R.drawable.ti_layout_grid_add, RowColors.Orange, subtitle = stringResource(R.string.studio_widgets_desc)) { push("widgets_help") }
        }
    }
}

@Composable
fun WidgetsHelpScreen(back: () -> Unit) {
    SettingsPage(stringResource(R.string.studio_widgets), back) {
        Text(stringResource(R.string.widgets_how), style = com.xenoorigin.xelauncher.design.XeType.Body, color = com.xenoorigin.xelauncher.design.LocalXeScheme.current.text)
    }
}

@Composable
fun AnimationScreen(s: XeSettings, update: Update, back: () -> Unit) {
    SettingsPage(stringResource(R.string.animation), back) {
        SettingsGroup(stringResource(R.string.anim_speed)) {
            listOf(AnimationSpeed.OFF to R.string.anim_off, AnimationSpeed.FAST to R.string.anim_fast, AnimationSpeed.NORMAL to R.string.anim_normal, AnimationSpeed.SMOOTH to R.string.anim_smooth)
                .forEach { (sp, l) -> ChoiceRow(stringResource(l), s.animationSpeed == sp) { update { it.copy(animationSpeed = sp) } } }
        }
        SettingsGroup {
            XeRow(stringResource(R.string.press_squish)) { GlassSwitch(s.pressSquish, { v -> update { it.copy(pressSquish = v) } }) }
            XeRow(stringResource(R.string.parallax)) { GlassSwitch(s.wallpaperParallax, { v -> update { it.copy(wallpaperParallax = v) } }) }
        }
    }
}

/** Unread counts on icons: Xe's access to notifications, and how the badge looks. */
@Composable
fun BadgesScreen(s: XeSettings, update: Update, back: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val access = com.xenoorigin.xelauncher.badges.rememberBadgeAccess()
    SettingsPage(stringResource(R.string.badges_title), back) {
        SettingsGroup {
            if (access) XeRow(stringResource(R.string.badges_access_on), icon = R.drawable.ti_check, iconTint = RowColors.Green, subtitle = stringResource(R.string.badges_access_on_desc))
            else XeRow(
                stringResource(R.string.badges_access_off), icon = R.drawable.ti_bell, iconTint = RowColors.Red,
                subtitle = stringResource(R.string.badges_access_off_desc),
                onClick = { com.xenoorigin.xelauncher.badges.BadgeListener.requestAccess(ctx) },
            )
        }
        SettingsGroup(stringResource(R.string.badges_style)) {
            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Box {
                    Box(
                        Modifier.size(58.dp).glassTile(RoundedCornerShape(17.dp), LocalGlassStyle.current, Color.White.copy(alpha = .12f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(R.drawable.ti_message_circle), null, Modifier.size(26.dp), tint = LocalXeScheme.current.text) }
                    CompositionLocalProvider(com.xenoorigin.xelauncher.badges.LocalBadgeStyle provides s.badgeStyle) {
                        androidx.compose.runtime.key(s.badgeStyle) {
                            com.xenoorigin.xelauncher.badges.UnreadBadge(3, 58.dp, Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-7).dp))
                        }
                    }
                }
            }
            listOf(
                com.xenoorigin.xelauncher.settings.BadgeStyle.RED to R.string.badge_red,
                com.xenoorigin.xelauncher.settings.BadgeStyle.WHITE to R.string.badge_white,
                com.xenoorigin.xelauncher.settings.BadgeStyle.DOT to R.string.badge_dot,
                com.xenoorigin.xelauncher.settings.BadgeStyle.OFF to R.string.badge_off,
            ).forEach { (st, l) -> ChoiceRow(stringResource(l), s.badgeStyle == st) { update { it.copy(badgeStyle = st) } } }
        }
        Text(stringResource(R.string.badges_note), style = XeType.Caption, color = LocalXeScheme.current.textSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp))
    }
}

/** Every toy in one place, each with a line on how to play with it. */
@Composable
fun PlayScreen(s: XeSettings, update: Update, push: (String) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.play_title), back) {
        SettingsGroup(stringResource(R.string.play_drawer)) {
            XeRow(stringResource(R.string.toy_icons), icon = R.drawable.ti_device_mobile_vibration, iconTint = RowColors.Purple, subtitle = stringResource(R.string.toy_icons_desc)) {
                GlassSwitch(s.toyIcons, { v -> update { it.copy(toyIcons = v) } })
            }
            XeRow(stringResource(R.string.pond), icon = R.drawable.ti_droplet, iconTint = RowColors.Cyan, subtitle = stringResource(R.string.pond_desc)) {
                GlassSwitch(s.pond, { v -> update { it.copy(pond = v) } })
            }
            XeRow(stringResource(R.string.app_dust), icon = R.drawable.ti_wind, iconTint = RowColors.Grey, subtitle = stringResource(R.string.app_dust_desc)) {
                GlassSwitch(s.appDust, { v -> update { it.copy(appDust = v, dustSince = if (v) System.currentTimeMillis() else 0L, dustWiped = emptyMap()) } })
            }
            XeRow(stringResource(R.string.bar_magnify), icon = R.drawable.ti_zoom_in, iconTint = RowColors.Blue, subtitle = stringResource(R.string.bar_magnify_desc)) {
                GlassSwitch(s.barMagnify, { v -> update { it.copy(barMagnify = v) } })
            }
            XeRow(stringResource(R.string.category_glow), icon = R.drawable.ti_color_filter, iconTint = RowColors.Pink, subtitle = stringResource(R.string.category_glow_desc)) {
                GlassSwitch(s.categoryGlow, { v -> update { it.copy(categoryGlow = v) } })
            }
        }
        SettingsGroup(stringResource(R.string.play_everywhere)) {
            XeRow(stringResource(R.string.tilt_shine), icon = R.drawable.ti_sun_high, iconTint = RowColors.Orange, subtitle = stringResource(R.string.tilt_shine_icons_desc)) {
                GlassSwitch(s.tiltShine, { v -> update { it.copy(tiltShine = v) } })
            }
            NavRow(stringResource(R.string.action_draw), R.drawable.ti_signature, RowColors.Green, subtitle = stringResource(R.string.draw_setting_desc)) { push("gestures") }
            NavRow(stringResource(R.string.live_sky), R.drawable.ti_moon, RowColors.Indigo, subtitle = stringResource(R.string.live_sky_desc)) { push("wallpaper") }
        }
        Text(
            stringResource(R.string.play_secret), style = XeType.Caption, color = LocalXeScheme.current.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        )
    }
}

private val gestureChoices = listOf(
    GestureAction.NONE to R.string.action_none, GestureAction.OPEN_DRAWER to R.string.action_drawer,
    GestureAction.NOTIFICATIONS to R.string.action_notifications, GestureAction.OPEN_SEARCH to R.string.action_search,
    GestureAction.OPEN_SETTINGS to R.string.action_settings, GestureAction.EDIT_MODE to R.string.action_edit,
    GestureAction.DRAW_LETTER to R.string.action_draw,
)

@Composable
fun GesturesScreen(s: XeSettings, update: Update, back: () -> Unit) {
    var picking by remember { mutableStateOf<Int?>(null) }
    val label = { a: GestureAction -> gestureChoices.first { it.first == a }.second }
    SettingsPage(stringResource(R.string.gestures), back) {
        SettingsGroup {
            NavRow(stringResource(R.string.gesture_swipe_up), R.drawable.ti_chevron_up, RowColors.Blue, stringResource(label(s.swipeUp))) { picking = 0 }
            NavRow(stringResource(R.string.gesture_swipe_down), R.drawable.ti_chevron_down, RowColors.Green, stringResource(label(s.swipeDown))) { picking = 1 }
            NavRow(stringResource(R.string.gesture_double_tap), R.drawable.ti_hand_finger, RowColors.Orange, stringResource(label(s.doubleTap))) { picking = 2 }
            NavRow(stringResource(R.string.gesture_two_finger), R.drawable.ti_arrows_move, RowColors.Purple, stringResource(label(s.twoFingerDown))) { picking = 3 }
        }
    }
    GlassSheet(picking != null, { picking = null }) {
        val current = when (picking) { 0 -> s.swipeUp; 1 -> s.swipeDown; 2 -> s.doubleTap; else -> s.twoFingerDown }
        gestureChoices.forEach { (a, l) ->
            ChoiceRow(stringResource(l), current == a) {
                val p = picking
                update { st -> when (p) { 0 -> st.copy(swipeUp = a); 1 -> st.copy(swipeDown = a); 2 -> st.copy(doubleTap = a); else -> st.copy(twoFingerDown = a) } }
                picking = null
            }
        }
    }
}

@Composable
fun HomePageScreen(s: XeSettings, update: Update, push: (String) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.home_screen), back) {
        SettingsGroup {
            SliderRow(stringResource(R.string.grid_columns), s.homeColumns.toFloat(), 4f..6f, steps = 1, display = { "${it.roundToInt()}" }) { v -> update { it.copy(homeColumns = v.roundToInt()) } }
            SliderRow(stringResource(R.string.grid_rows), s.homeRows.toFloat(), 5f..7f, steps = 1, display = { "${it.roundToInt()}" }) { v -> update { it.copy(homeRows = v.roundToInt()) } }
            SliderRow(stringResource(R.string.dock_size), s.dockSize.toFloat(), 3f..5f, steps = 1, display = { "${it.roundToInt()}" }) { v -> update { it.copy(dockSize = v.roundToInt()) } }
        }
        SettingsGroup {
            XeRow(stringResource(R.string.lock_layout), icon = R.drawable.ti_lock) { GlassSwitch(s.layoutLocked, { v -> update { it.copy(layoutLocked = v) } }) }
            NavRow(stringResource(R.string.gestures), R.drawable.ti_hand_finger, RowColors.Red) { push("gestures") }
        }
    }
}

@Composable
fun AppPageScreen(s: XeSettings, update: Update, push: (String) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.app_page), back) {
        SettingsGroup {
            XeRow(stringResource(R.string.category_bar), icon = R.drawable.ti_layout_grid) { GlassSwitch(s.categoryBar, { v -> update { it.copy(categoryBar = v) } }) }
        }
        SettingsGroup(stringResource(R.string.drawer_layout)) {
            listOf(com.xenoorigin.xelauncher.settings.DrawerLayout.BENTO to R.string.layout_bento, com.xenoorigin.xelauncher.settings.DrawerLayout.GRID to R.string.layout_grid)
                .forEach { (lo, l) -> ChoiceRow(stringResource(l), s.drawerLayout == lo) { update { it.copy(drawerLayout = lo) } } }
        }
        SettingsGroup(stringResource(R.string.sorting)) {
            listOf(DrawerSort.ALPHABETICAL to R.string.sort_alpha, DrawerSort.MOST_USED to R.string.sort_most_used, DrawerSort.RECENTLY_INSTALLED to R.string.sort_recent)
                .forEach { (so, l) -> ChoiceRow(stringResource(l), s.drawerSort == so) { update { it.copy(drawerSort = so) } } }
        }
        SettingsGroup { NavRow(stringResource(R.string.hidden_apps), R.drawable.ti_eye_off, RowColors.Grey) { push("hidden") } }
    }
}

@Composable
fun SearchPageScreen(s: XeSettings, update: Update, back: () -> Unit) {
    SettingsPage(stringResource(R.string.search_page), back) {
        SettingsGroup {
            XeRow(stringResource(R.string.search_calc_enabled), icon = R.drawable.ti_calculator) { GlassSwitch(s.searchCalculator, { v -> update { it.copy(searchCalculator = v) } }) }
            XeRow(stringResource(R.string.search_web_enabled), icon = R.drawable.ti_world) { GlassSwitch(s.searchWeb, { v -> update { it.copy(searchWeb = v) } }) }
            XeRow(stringResource(R.string.auto_keyboard), icon = R.drawable.ti_typography) { GlassSwitch(s.searchAutoKeyboard, { v -> update { it.copy(searchAutoKeyboard = v) } }) }
        }
    }
}

@Composable
fun HiddenAppsScreen(graph: AppGraph, s: XeSettings, update: Update, back: () -> Unit) {
    val apps by graph.apps.apps.collectAsState()
    val hidden = apps.filter { it.key.encode() in s.hiddenApps }.sortedBy { it.label }
    SettingsPage(stringResource(R.string.hidden_apps), back) {
        if (hidden.isEmpty()) Text(stringResource(R.string.hidden_apps_empty), style = XeType.Body, color = LocalXeScheme.current.textSecondary)
        else SettingsGroup {
            hidden.forEach { a ->
                XeRow(a.label) { GlassPillButton(stringResource(R.string.unhide_app), { update { it.copy(hiddenApps = it.hiddenApps - a.key.encode()) } }) }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AboutScreen(back: () -> Unit, s: XeSettings? = null, update: Update = {}, notifier: com.xenoorigin.xelauncher.update.UpdateNotifier? = null) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var licences by remember { mutableStateOf(false) }
    val upToDate = stringResource(R.string.up_to_date); val failed = stringResource(R.string.update_check_failed)
    val availableFmt = stringResource(R.string.update_available); val checking = stringResource(R.string.checking)
    val release = notifier?.available?.collectAsState()?.value
    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    // Secret: tap the name 7 times quickly.
    val counter = remember { com.xenoorigin.xelauncher.design.TapCounter(needed = 7, windowMs = 650) }
    var burst by remember { mutableStateOf(0) }
    var burstAt by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var nameCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val spin = remember { androidx.compose.animation.core.Animatable(0f) }
    val found = stringResource(R.string.nova_found); val again = stringResource(R.string.nova_again)
    var game by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(Modifier.fillMaxSize()) {
    SettingsPage(stringResource(R.string.about), back) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Xe Launcher", style = XeType.Title, color = LocalXeScheme.current.text,
                modifier = Modifier
                    .onGloballyPositioned { val b = it.boundsInRoot(); nameCenter = b.center }
                    .graphicsLayer { rotationZ = spin.value; scaleX = 1f + spin.value / 1800f; scaleY = scaleX }
                    .combinedClickable(
                        remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null,
                        // Secret game: hold the name.
                        onLongClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); game = true },
                    ) {
                        val hit = counter.tap(System.currentTimeMillis())
                        if (hit) {
                            val first = s?.novaUnlocked != true
                            update { it.copy(novaUnlocked = true) }
                            burstAt = nameCenter; burst++
                            Toast.makeText(ctx, if (first) found else again, Toast.LENGTH_LONG).show()
                        }
                        scope.launch {
                            spin.snapTo(0f)
                            if (hit) spin.animateTo(360f, androidx.compose.animation.core.tween(700)) else { spin.animateTo(14f, androidx.compose.animation.core.tween(90)); spin.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = .4f, stiffness = 500f)) }
                            spin.snapTo(0f)
                        }
                    },
            )
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = XeType.Caption, color = LocalXeScheme.current.textSecondary)
        }
        SettingsGroup {
            NavRow(stringResource(R.string.check_updates), R.drawable.ti_refresh, RowColors.Blue, subtitle = status) {
                status = checking
                scope.launch {
                    val r = notifier?.checkNow() ?: UpdateChecker.check(BuildConfig.VERSION_NAME)
                    status = when (r) {
                        is com.xenoorigin.xelauncher.update.UpdateResult.Available -> availableFmt.format(r.release.tag)
                        com.xenoorigin.xelauncher.update.UpdateResult.UpToDate -> upToDate
                        com.xenoorigin.xelauncher.update.UpdateResult.Failed -> failed
                    }
                }
            }
            release?.let { r -> XeRow(stringResource(R.string.download_update), icon = R.drawable.ti_download, iconTint = RowColors.Green, onClick = { open(r.apkUrl) }) }
            if (s != null) XeRow(stringResource(R.string.update_notify)) { GlassSwitch(s.updateNotify, { v -> update { it.copy(updateNotify = v) } }) }
            val testCtx = androidx.compose.ui.platform.LocalContext.current
            val blocked = stringResource(R.string.test_alert_blocked)
            XeRow(stringResource(R.string.test_alert), icon = R.drawable.ti_bell_ringing, iconTint = RowColors.Orange, onClick = {
                if (!com.xenoorigin.xelauncher.update.UpdateNotifier.postTest(testCtx)) {
                    android.widget.Toast.makeText(testCtx, blocked, android.widget.Toast.LENGTH_LONG).show()
                    runCatching {
                        testCtx.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, testCtx.packageName)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }
            })
            NavRow(stringResource(R.string.website), R.drawable.ti_world_www, RowColors.Cyan) { open(UpdateChecker.WEBSITE) }
            NavRow(stringResource(R.string.replay_intro), R.drawable.ti_sparkles, RowColors.Purple) { update { it.copy(onboarding = com.xenoorigin.xelauncher.settings.OnboardingStage.INTRO) } }
            NavRow(stringResource(R.string.licenses), R.drawable.ti_book, RowColors.Grey) { licences = true }
        }
    }
    GlassBurst(burst, burstAt)
    androidx.compose.animation.AnimatedVisibility(game, enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = .9f), exit = androidx.compose.animation.fadeOut()) {
        com.xenoorigin.xelauncher.play.BubbleGame(s?.bubbleBest ?: 0, onRecord = { r -> update { it.copy(bubbleBest = maxOf(it.bubbleBest, r)) } }, onClose = { game = false })
    }
    }
    GlassSheet(licences, { licences = false }) {
        val text = remember {
            ctx.assets.list("licenses").orEmpty().sorted().joinToString("\n\n") { f -> "— $f —\n" + ctx.assets.open("licenses/$f").bufferedReader().use { it.readText() } }
        }
        Text(text, style = XeType.Caption, color = LocalXeScheme.current.textSecondary)
    }
}
