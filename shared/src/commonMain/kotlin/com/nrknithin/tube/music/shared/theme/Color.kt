/*
 * SPDX-FileCopyrightText: 2024 NewPipe contributors <https://newpipe.net>
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.shared.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette modelled on the YouTube Music app.
 *
 * YouTube Music is a dark-first product: a near-black canvas, white primary text, grey secondary
 * text, and brand red reserved for accents (active tab, progress, play affordances). The light
 * palette mirrors it inverted, since light remains selectable in Appearance settings.
 */

/** Brand red, used for the logo background and as the accent in both palettes. */
val logoBackground = Color(0xFFFF0033)

private val brandRed = Color(0xFFFF0033)
private val brandRedDim = Color(0xFFCC0029)

// ---------------------------------------------------------------------------------------------
// Light
// ---------------------------------------------------------------------------------------------

val primaryLight = brandRedDim
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFFFDAD9)
val onPrimaryContainerLight = Color(0xFF410006)
val secondaryLight = Color(0xFF5F5E5E)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFE6E6E6)
val onSecondaryContainerLight = Color(0xFF1C1B1B)
val tertiaryLight = Color(0xFF3B3B3B)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFDEDEDE)
val onTertiaryContainerLight = Color(0xFF141414)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFFFFFFF)
val onBackgroundLight = Color(0xFF0F0F0F)
val surfaceLight = Color(0xFFFFFFFF)
val onSurfaceLight = Color(0xFF0F0F0F)
val surfaceVariantLight = Color(0xFFF2F2F2)
val onSurfaceVariantLight = Color(0xFF606060)
val outlineLight = Color(0xFFCCCCCC)
val outlineVariantLight = Color(0xFFE5E5E5)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF212121)
val inverseOnSurfaceLight = Color(0xFFF1F1F1)
val inversePrimaryLight = Color(0xFFFFB3B0)
val surfaceDimLight = Color(0xFFE0E0E0)
val surfaceBrightLight = Color(0xFFFFFFFF)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFFAFAFA)
val surfaceContainerLight = Color(0xFFF2F2F2)
val surfaceContainerHighLight = Color(0xFFEAEAEA)
val surfaceContainerHighestLight = Color(0xFFE3E3E3)

// ---------------------------------------------------------------------------------------------
// Dark - the default, and the one that should read as "YouTube Music"
// ---------------------------------------------------------------------------------------------

val primaryDark = brandRed
val onPrimaryDark = Color(0xFFFFFFFF)
val primaryContainerDark = Color(0xFF930021)
val onPrimaryContainerDark = Color(0xFFFFDAD9)
val secondaryDark = Color(0xFFC9C9C9)
val onSecondaryDark = Color(0xFF1F1F1F)
val secondaryContainerDark = Color(0xFF2E2E2E)
val onSecondaryContainerDark = Color(0xFFE8E8E8)
val tertiaryDark = Color(0xFFAAAAAA)
val onTertiaryDark = Color(0xFF1A1A1A)
val tertiaryContainerDark = Color(0xFF373737)
val onTertiaryContainerDark = Color(0xFFE0E0E0)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)

/** The YouTube Music canvas: near black, not pure black, so elevated surfaces can read above it. */
val backgroundDark = Color(0xFF030303)
val onBackgroundDark = Color(0xFFFFFFFF)
val surfaceDark = Color(0xFF030303)
val onSurfaceDark = Color(0xFFFFFFFF)
val surfaceVariantDark = Color(0xFF212121)

/** Secondary text: artist names, counts, captions. */
val onSurfaceVariantDark = Color(0xFFAAAAAA)
val outlineDark = Color(0xFF3F3F3F)
val outlineVariantDark = Color(0xFF272727)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE3E3E3)
val inverseOnSurfaceDark = Color(0xFF1F1F1F)
val inversePrimaryDark = brandRedDim
val surfaceDimDark = Color(0xFF030303)
val surfaceBrightDark = Color(0xFF383838)
val surfaceContainerLowestDark = Color(0xFF000000)
val surfaceContainerLowDark = Color(0xFF0F0F0F)

/** Cards and rows sitting on the canvas. */
val surfaceContainerDark = Color(0xFF1F1F1F)

/** The mini player bar and bottom bar. */
val surfaceContainerHighDark = Color(0xFF282828)
val surfaceContainerHighestDark = Color(0xFF323232)
