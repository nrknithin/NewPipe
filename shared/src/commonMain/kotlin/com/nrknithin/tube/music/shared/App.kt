/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.shared

import androidx.compose.runtime.Composable
import com.nrknithin.tube.music.shared.di.KoinApp
import com.nrknithin.tube.music.shared.navigation.Destination
import com.nrknithin.tube.music.shared.navigation.NavDisplay
import com.nrknithin.tube.music.shared.navigation.navModule
import com.nrknithin.tube.music.shared.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

/**
 * Entry point for the multiplatform compose application
 * @param startDestination Starting destination for the app; defaults to about
 * @param onCloseRequest Callback to close the app
 * @param withKoin Additional logic to execute after initialising Koin and setting content
 */
@Composable
fun App(
    startDestination: Destination = Destination.About,
    onCloseRequest: () -> Unit,
    withKoin: @Composable () -> Unit = {}
) {
    KoinApplication(
        configuration = koinConfiguration<KoinApp>(
            appDeclaration = {
                modules(navModule())
            }
        )
    ) {
        AppTheme {
            NavDisplay(
                startDestination = startDestination,
                onCloseRequest = onCloseRequest
            )
            withKoin()
        }
    }
}
