package com.xenoorigin.xelauncher.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xenoorigin.xelauncher.R

/** Inter (Latin) merged with Noto Sans Thai (Thai) into one family. */
val XeFont = FontFamily(
    Font(R.font.xe_regular, FontWeight.Normal),
    Font(R.font.xe_medium, FontWeight.Medium),
    Font(R.font.xe_semibold, FontWeight.SemiBold),
    Font(R.font.xe_bold, FontWeight.Bold),
)

object XeType {
    val LargeTitle = TextStyle(fontFamily = XeFont, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.6).sp, lineHeight = 40.sp)
    val Title = TextStyle(fontFamily = XeFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.3).sp)
    val Headline = TextStyle(fontFamily = XeFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    val Body = TextStyle(fontFamily = XeFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val Caption = TextStyle(fontFamily = XeFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val Label = TextStyle(fontFamily = XeFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = (-0.1).sp)
}
