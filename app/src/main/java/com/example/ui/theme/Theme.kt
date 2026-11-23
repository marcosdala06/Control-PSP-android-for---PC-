package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PadPhoneDarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = DeckObsidian,
    primaryContainer = CyanSoft,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldLive,
    onSecondary = DeckObsidian,
    secondaryContainer = DeckSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = PsCrossBlue,
    onTertiary = DeckObsidian,
    background = DeckObsidian,
    onBackground = TextPrimary,
    surface = DeckSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeckSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DeckBezel,
    outlineVariant = DeckSurfaceElevated,
    error = CrimsonAlert,
    onError = TextPrimary,
)

@Composable
fun PadPhoneTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PadPhoneDarkColorScheme,
        typography = PadPhoneTypography,
        content = content,
    )
}
