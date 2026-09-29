/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.settings

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.nrknithin.tube.music.R

/**
 * One choice in a list setting.
 *
 * @property value exactly what gets written to preferences; it has to stay byte-identical to the
 * old XML entry value, otherwise the player or the extractor stops recognising it
 * @property label what the user reads
 */
data class SettingOption(val value: String, val label: String)

/**
 * Typed access to the app's settings.
 *
 * Every key, default and option list is resolved from `settings_keys.xml` instead of being retyped
 * here, because the player, the downloader and the extractor all read these same keys out of
 * [PreferenceManager.getDefaultSharedPreferences]. A key invented in this file would be a setting
 * the user can change and nothing obeys.
 *
 * Build this off the main thread: the first [PreferenceManager] call loads the preference file from
 * disk, and the country list is 248 entries long.
 */
class SettingsRepository(private val context: Context) {

    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    // ---- Appearance ----

    /** Key of the overall theme choice. */
    val themeKey: String = string(R.string.theme_key)

    /** Key of the theme used while the device is in dark mode. */
    val nightThemeKey: String = string(R.string.night_theme_key)

    /** The theme value that defers to the device, and so enables the night theme choice. */
    val autoDeviceTheme: String = string(R.string.auto_device_theme_key)

    /** Theme shipped as the default: dark, because this is a music app. */
    val themeDefault: String = string(R.string.default_theme_value)

    /** Night theme shipped as the default. */
    val nightThemeDefault: String = string(R.string.default_night_theme_value)

    /** Light / Dark / Black / Automatic. */
    val themeOptions: List<SettingOption> =
        options(R.array.theme_values_list, R.array.theme_description_list)

    /** Only the two dark variants, since a "light night theme" makes no sense. */
    val nightThemeOptions: List<SettingOption> =
        options(R.array.night_theme_values_list, R.array.night_theme_description_list)

    // ---- Playback ----

    /** Key of the autoplay-on-open choice. */
    val autoplayKey: String = string(R.string.autoplay_key)

    /** Autoplay default: Wi-Fi only. */
    val autoplayDefault: String = string(R.string.autoplay_value)

    /** Always / Only on Wi-Fi / Never. */
    val autoplayOptions: List<SettingOption> =
        options(R.array.autoplay_type_key, R.array.autoplay_type_description)

    /** Key of "append a related track when the queue runs out". */
    val autoQueueKey: String = string(R.string.auto_queue_key)

    /** Key of "keep playing after an interruption such as a call". */
    val resumeOnAudioFocusGainKey: String = string(R.string.resume_on_audio_focus_gain_key)

    /** Key of the fast-but-imprecise seek mode. */
    val useInexactSeekKey: String = string(R.string.use_inexact_seek_key)

    /** Key of the fast-forward / rewind step. */
    val seekDurationKey: String = string(R.string.seek_duration_key)

    /** Key of the confirmation prompt shown before a queue is thrown away. */
    val clearQueueConfirmationKey: String = string(R.string.clear_queue_confirmation_key)

    /** Key of the "Hold to enqueue" tip. */
    val holdToAppendKey: String = string(R.string.show_hold_to_append_key)

    // ---- Downloads ----

    /** Key of "ask me where to save each download". */
    val downloadsStorageAskKey: String = string(R.string.downloads_storage_ask)

    /** Key of the Storage Access Framework toggle. */
    val storageUseSafKey: String = string(R.string.storage_use_saf)

    /** Key of the folder audio downloads land in. */
    val downloadPathAudioKey: String = string(R.string.download_path_audio_key)

    /** Key of the folder video downloads land in. */
    val downloadPathVideoKey: String = string(R.string.download_path_video_key)

    // ---- Content ----

    /** Key of the language the extractor asks YouTube for. */
    val contentLanguageKey: String = string(R.string.content_language_key)

    /** Key of the country the extractor asks YouTube for. */
    val contentCountryKey: String = string(R.string.content_country_key)

    /** The "follow the system" value shared by both content pickers. */
    val systemLocalization: String = string(R.string.default_localization_key)

    /** Every language the extractor accepts, system default first. */
    val languageOptions: List<SettingOption> =
        options(R.array.language_codes, R.array.language_names)

    /** Every country the extractor accepts, system default first. */
    val countryOptions: List<SettingOption> =
        options(R.array.country_codes, R.array.country_names)

    // ---- History ----

    /** Key of the watch history. */
    val watchHistoryKey: String = string(R.string.enable_watch_history_key)

    /** Key of "resume where I left off", which depends on the watch history. */
    val playbackResumeKey: String = string(R.string.enable_playback_resume_key)

    /** Key of the search history. */
    val searchHistoryKey: String = string(R.string.enable_search_history_key)

    /**
     * Reads a boolean setting.
     *
     * Wrapped in [runCatching] because a preference file written by an older version of the app can
     * hold a different type under the same key, and [SharedPreferences] answers that with a
     * ClassCastException rather than the default.
     */
    fun boolean(key: String, default: Boolean): Boolean = runCatching { preferences.getBoolean(key, default) }.getOrDefault(default)

    /** Writes a boolean setting. */
    fun setBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    /** Reads a string setting, falling back to [default] for a missing or wrongly typed value. */
    fun text(key: String, default: String): String = runCatching { preferences.getString(key, default) }.getOrNull() ?: default

    /** Writes a string setting. */
    fun setText(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    /** Clears a string setting, which for the download folders means "no folder chosen". */
    fun clearText(key: String) {
        preferences.edit().putString(key, null).apply()
    }

    /** @return the label the option carrying [value] shows, or null when no option carries it */
    fun labelOf(options: List<SettingOption>, value: String): String? = options.firstOrNull { it.value == value }?.label

    /**
     * Renders a stored download folder the way the old download settings screen did: a `file://`
     * folder shows as a plain path, anything else as the raw SAF tree URI.
     *
     * @return null when no folder has been chosen yet, so the caller can show its own hint
     */
    fun folderSummary(key: String): String? {
        val stored = text(key, "")
        if (stored.isEmpty()) {
            return null
        }
        val uri = Uri.parse(stored)
        if (uri == Uri.EMPTY) {
            return null
        }
        return if (ContentResolver.SCHEME_FILE == uri.scheme) uri.path else uri.toString()
    }

    /** @return the stored seek step in milliseconds */
    fun seekDurationMillis(): Int = text(seekDurationKey, string(R.string.seek_duration_default_value)).toIntOrNull()
        ?: DEFAULT_SEEK_MILLIS

    /**
     * The selectable seek steps.
     *
     * ExoPlayer cannot honour a 5, 15 or 25 second step while inexact seek is on, so those drop out
     * of the list exactly as the old preference screen removed them.
     */
    fun seekDurationOptions(inexactSeek: Boolean): List<SettingOption> = context.resources.getStringArray(R.array.seek_duration_value)
        .mapNotNull { value -> value.toIntOrNull() }
        .map { millis -> millis / MILLIS_PER_SECOND }
        .filterNot { seconds -> inexactSeek && seconds % 10 == 5 }
        .map { seconds ->
            SettingOption(
                value = (seconds * MILLIS_PER_SECOND).toString(),
                label = context.resources.getQuantityString(R.plurals.seconds, seconds, seconds)
            )
        }

    /**
     * Moves the stored seek step onto the nearest selectable value after inexact seek was switched
     * on, because the previously chosen 5, 15 or 25 seconds no longer exists.
     *
     * @return the new step in whole seconds so the caller can say so, or null when nothing moved
     */
    fun coerceSeekDuration(inexactSeek: Boolean): Int? {
        if (!inexactSeek) {
            return null
        }
        val seconds = seekDurationMillis() / MILLIS_PER_SECOND
        if (seconds % 10 != 5) {
            return null
        }
        val replacement = seconds + 5
        setText(seekDurationKey, (replacement * MILLIS_PER_SECOND).toString())
        return replacement
    }

    private fun string(@StringRes resource: Int): String = context.getString(resource)

    private fun options(@ArrayRes values: Int, @ArrayRes labels: Int): List<SettingOption> {
        val storedValues = context.resources.getStringArray(values)
        val shownLabels = context.resources.getStringArray(labels)
        return storedValues.zip(shownLabels) { value, label -> SettingOption(value, label) }
            .distinctBy { it.value }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000
        const val DEFAULT_SEEK_MILLIS = 10000
    }
}
