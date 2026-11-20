package com.xenoorigin.xelauncher.drawer

import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.categories.BuiltInCategory
import com.xenoorigin.xelauncher.categories.CategoryWithApps

fun sampleCategories(): List<CategoryWithApps> = BuiltInCategory.entries.mapIndexed { i, b ->
    CategoryWithApps(i.toLong() + 1, listOf("สื่อสาร", "โซเชียล", "บันเทิง", "เกม", "ช้อปปิ้ง", "การเงิน", "เดินทางและอาหาร", "งานและการเรียน", "เครื่องมือ", "แอปอื่น")[i], b.iconName, b,
        (0 until 3 + i).map { j -> AppEntry(AppKey("pkg.${j % 4}", "pkg.${j % 4}.Main", i * 100L + j), listOf("โทรศัพท์", "Discord", "กล้อง", "Notion")[j % 4], -1, 0, false) })
}
