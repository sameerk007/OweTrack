package com.owetrack.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.owetrack.app.data.AppTheme

private val Light = lightColorScheme(primary=Color(0xFF006C4C), onPrimary=Color.White, primaryContainer=Color(0xFF8CF8C6), secondary=Color(0xFF4D6357), error=Color(0xFFBA1A1A), surface=Color(0xFFF8FAF7), surfaceVariant=Color(0xFFDCE5DE))
private val Dark = darkColorScheme(primary=Color(0xFF70DBAB), primaryContainer=Color(0xFF005139), secondary=Color(0xFFB4CCBE), surface=Color(0xFF101412), surfaceVariant=Color(0xFF404943))
@Composable fun OweTrackTheme(theme:AppTheme, content: @Composable () -> Unit) {
    val dark = theme==AppTheme.DARK || (theme==AppTheme.SYSTEM && isSystemInDarkTheme())
    MaterialTheme(colorScheme=if(dark) Dark else Light, typography=Typography(), content=content)
}
