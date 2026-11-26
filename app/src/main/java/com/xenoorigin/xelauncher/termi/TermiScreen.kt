package com.xenoorigin.xelauncher.termi

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.settings.ui.ChoiceRow
import com.xenoorigin.xelauncher.settings.ui.SettingsGroup
import com.xenoorigin.xelauncher.settings.ui.SettingsPage

private fun hhmm(m: Int) = "%02d:%02d".format(m / 60, m % 60)

/**
 * Termi's own page: the big view, level and stage progress, needs, care buttons, the wardrobe, which apps count
 * as creator apps, and the night window. [apps] = installed apps as (package, label).
 */
@Composable
fun TermiScreen(
    store: TermiStore, apps: List<Pair<String, String>>, usageAccess: Boolean, onGrantUsage: () -> Unit, onClose: () -> Unit,
    accent: Long, ascii: AsciiColor, animate: Boolean,
) {
    BackHandler(onBack = onClose)
    val scheme = LocalXeScheme.current
    val ui = rememberTermiUi(store)
    val saved by store.state.collectAsState()
    val s = remember(saved) { store.current() }
    val mood = remember(saved) { store.mood() }
    val stage = TermiRules.stage(s.xp)
    val level = TermiRules.level(s.xp)
    SettingsPage("Termi", onClose) {
        Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp))) {
            TermiView(stage, s.equipped, mood, ui.event, TermiSprite.bodyColor(accent), ascii, accent, animate, Modifier.fillMaxWidth().height(260.dp), ui.eventKey)
        }
        SettingsGroup {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.termi_level, level) + " · " + stageName(stage), style = XeType.Headline, color = scheme.text)
                val toLevel = if (level >= TermiRules.MAX_LEVEL) 0 else level * TermiRules.XP_PER_LEVEL - s.xp
                if (toLevel > 0) Text(stringResource(R.string.termi_next_level, toLevel), style = XeType.Caption, color = scheme.textSecondary)
                val next = when (stage) { Stage.JUNIOR -> TermiRules.SENIOR_XP to "Senior"; Stage.SENIOR -> TermiRules.LEAD_XP to "Lead"; Stage.LEAD -> null }
                if (next != null) Text(stringResource(R.string.termi_next_stage, next.first - s.xp, next.second), style = XeType.Caption, color = scheme.textSecondary)
                Box(Modifier.padding(top = 8.dp).testTag("termi_xp")) {
                    NeedBar("XP", ((s.xp % TermiRules.XP_PER_LEVEL) * 100f / TermiRules.XP_PER_LEVEL).let { if (level >= TermiRules.MAX_LEVEL) 100f else it }, Color(0xFFC27BFF))
                }
                NeedBar(stringResource(R.string.termi_food), s.food, Color(0xFFFFD166))
                NeedBar(stringResource(R.string.termi_fun), s.`fun`, Color(0xFFFF8CB4))
                NeedBar(stringResource(R.string.termi_energy), s.energy, Color(0xFF7FE7FF))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareButtons(store, mood == Mood.ASLEEP, Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0x33000000)))
                }
            }
        }
        SettingsGroup(stringResource(R.string.termi_wardrobe)) {
            for ((slot, title) in listOf(Slot.HEAD to R.string.termi_hats, Slot.EYES to R.string.termi_glasses, Slot.HAND to R.string.termi_held)) {
                Text(stringResource(title), style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(start = 14.dp, top = 10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WearChip(stringResource(R.string.termi_none), s.equipped[slot] == null, true, "wear_none_$slot") { store.equip(null, slot) }
                    for (item in Item.entries.filter { it.slot == slot }) {
                        val open = item.level <= level
                        WearChip(if (open) itemName(item) else stringResource(R.string.termi_locked, item.level), s.equipped[slot] == item, open, "wear_${item.name}") {
                            if (open) store.equip(item, slot)
                        }
                    }
                }
            }
        }
        SettingsGroup(stringResource(R.string.termi_creators)) {
            Text(stringResource(R.string.termi_creators_desc), style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(14.dp))
            if (!usageAccess) Box(Modifier.testTag("termi_usage")) { ChoiceRow(stringResource(R.string.termi_usage_needed), false, onClick = onGrantUsage) }
            val chosen = store.creatorApps().let { c -> remember(saved) { c } }
            val sorted = apps.sortedWith(compareBy({ it.first !in chosen }, { it.second.lowercase() }))
            for ((pkg, label) in sorted) {
                Box(Modifier.testTag("creator_$pkg")) { ChoiceRow(label, pkg in chosen) { store.setCreator(pkg, pkg !in chosen) } }
            }
        }
        SettingsGroup(stringResource(R.string.termi_night)) {
            Text(stringResource(R.string.termi_night_desc), style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(14.dp))
            Stepper(stringResource(R.string.termi_night_from), hhmm(s.nightStart), "night_start", { store.setNight(s.nightStart - 30, s.nightEnd) }) { store.setNight(s.nightStart + 30, s.nightEnd) }
            Stepper(stringResource(R.string.termi_night_to), hhmm(s.nightEnd), "night_end", { store.setNight(s.nightStart, s.nightEnd - 30) }) { store.setNight(s.nightStart, s.nightEnd + 30) }
        }
    }
}

@Composable
private fun itemName(i: Item): String = stringResource(
    when (i) {
        Item.CAP -> R.string.item_CAP; Item.COFFEE -> R.string.item_COFFEE; Item.SUNGLASSES -> R.string.item_SUNGLASSES
        Item.BEANIE -> R.string.item_BEANIE; Item.KEYBOARD -> R.string.item_KEYBOARD; Item.ROUND -> R.string.item_ROUND
        Item.PARTY -> R.string.item_PARTY; Item.DUCK -> R.string.item_DUCK; Item.VR -> R.string.item_VR
        Item.PHONE -> R.string.item_PHONE; Item.WIZARD -> R.string.item_WIZARD
    },
)

@Composable
private fun WearChip(label: String, selected: Boolean, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val scheme = LocalXeScheme.current
    Box(
        Modifier.testTag(tag).clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.text else Color.White.copy(alpha = if (enabled) .12f else .05f))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) { Text(label, color = if (selected) scheme.background else scheme.text.copy(alpha = if (enabled) 1f else .45f), fontSize = 14.sp) }
}

@Composable
private fun Stepper(label: String, value: String, tag: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val scheme = LocalXeScheme.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = XeType.Body, color = scheme.text, modifier = Modifier.weight(1f))
        Box(Modifier.testTag("${tag}_minus").clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .12f)).clickable(onClick = onMinus).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("−", color = scheme.text, fontSize = 18.sp) }
        Text(value, style = XeType.Headline, color = scheme.text, modifier = Modifier.width(72.dp).padding(horizontal = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.testTag("${tag}_plus").clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .12f)).clickable(onClick = onPlus).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("+", color = scheme.text, fontSize = 18.sp) }
    }
}
