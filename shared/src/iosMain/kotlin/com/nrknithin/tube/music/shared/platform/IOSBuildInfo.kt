package com.nrknithin.tube.music.shared.platform

import org.koin.core.annotation.Singleton

@Singleton(binds = [BuildInfo::class])
class IOSBuildInfo : BuildInfo {
    override val isReleaseApk: Boolean = false
    override val isDebug: Boolean = false
}
