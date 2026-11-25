package com.rotguard.app.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class VerdantPalette(
    val isDark: Boolean,
    val canvas: Color,
    val card: Color,
    val cardBorder: Color,
    val warmCard: Color,
    val warmBorder: Color,
    val rule: Color,
    val ruleStrong: Color,
    val ink: Color,
    val inkMuted: Color,
    val navActiveInk: Color,
    val sageTint: Color,
    val mossAccent: Color,
    val mossBtnFill: Color,
    val mossBtnStroke: Color,
    val mossBtnInk: Color,
    val primaryCtaFill: Color,
    val primaryCtaInk: Color,
    val terracotta: Color,
    val terracottaSoft: Color,
    val sunGold: Color,
    val canopy: Color,
    val canopyCard: Color,
    val canopyStroke: Color,
    val canopyCore: Color,
    val haloOuter: Color,
    val haloMid: Color,
    val canopyMuted: Color,
    val cream: Color,
)

val LightVerdantPalette = VerdantPalette(
    isDark = false,
    canvas = Color(0xFFEFF3EC),
    card = Color(0xFFFAFCF8),
    cardBorder = Color(0xFFD0DBCD),
    warmCard = Color(0xFFF9F4EA),
    warmBorder = Color(0xFFE4D7BF),
    rule = Color(0xFFD3DEC9),
    ruleStrong = Color(0xFF8FA395),
    ink = Color(0xFF1C352B),
    inkMuted = Color(0xFF4E685C),
    navActiveInk = Color(0xFF2F6F4E),
    sageTint = Color(0xFFDCE5D8),
    mossAccent = Color(0xFF2F6F4E),
    mossBtnFill = Color(0xFF2F6F4E),
    mossBtnStroke = Color(0xFF24573D),
    mossBtnInk = Color(0xFFFAFCF8),
    primaryCtaFill = Color(0xFF1C352B),
    primaryCtaInk = Color(0xFFF6F5EE),
    terracotta = Color(0xFFC96846),
    terracottaSoft = Color(0xFFF4E5DE),
    sunGold = Color(0xFFDFA952),
    canopy = Color(0xFF1C352B),
    canopyCard = Color(0xFF244337),
    canopyStroke = Color(0xFF396151),
    canopyCore = Color(0xFF162A22),
    haloOuter = Color(0xFF294B3D),
    haloMid = Color(0xFF335C4B),
    canopyMuted = Color(0xFF9BB5A8),
    cream = Color(0xFFF6F5EE),
)

val DarkVerdantPalette = VerdantPalette(
    isDark = true,
    canvas = Color(0xFF11211A),
    card = Color(0xFF182D24),
    cardBorder = Color(0xFF274337),
    warmCard = Color(0xFF202821),
    warmBorder = Color(0xFF3B4737),
    rule = Color(0xFF243F34),
    ruleStrong = Color(0xFF4A6E5E),
    ink = Color(0xFFEEF3EB),
    inkMuted = Color(0xFF94AFA2),
    navActiveInk = Color(0xFF63B584),
    sageTint = Color(0xFF223D31),
    mossAccent = Color(0xFF63B584),
    mossBtnFill = Color(0xFF2D6D4C),
    mossBtnStroke = Color(0xFF63B584),
    mossBtnInk = Color(0xFFFAFCF8),
    primaryCtaFill = Color(0xFFEEF3EB),
    primaryCtaInk = Color(0xFF11211A),
    terracotta = Color(0xFFE28360),
    terracottaSoft = Color(0xFF35211A),
    sunGold = Color(0xFFE8B861),
    canopy = Color(0xFF0B1712),
    canopyCard = Color(0xFF14271F),
    canopyStroke = Color(0xFF274337),
    canopyCore = Color(0xFF08120E),
    haloOuter = Color(0xFF183428),
    haloMid = Color(0xFF214636),
    canopyMuted = Color(0xFF94AFA2),
    cream = Color(0xFFEEF3EB),
)

val LocalVerdantPalette = staticCompositionLocalOf { LightVerdantPalette }

object VerdantTheme {
    val colors: VerdantPalette
        @Composable
        get() = LocalVerdantPalette.current
}
