/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.nononsenseapps.filepicker.Utils
import com.nrknithin.tube.music.BuildConfig
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.settings.NewPipeSettings
import com.nrknithin.tube.music.streams.io.StoredDirectoryHelper
import com.nrknithin.tube.music.util.FilePickerActivityHelper
import com.nrknithin.tube.music.util.KEY_THEME_CHANGE
import com.nrknithin.tube.music.util.ThemeHelper
import com.nrknithin.tube.music.util.external_communication.ShareUtils
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SettingsScreen"

/** How far a row that depends on another setting is faded once that dependency turns it off. */
private const val DISABLED_ALPHA = 0.38f

/** Tallest the option list inside a picker dialog grows before it starts scrolling. */
private val DIALOG_MAX_HEIGHT = 360.dp

/**
 * What the screen shows while the preference file is being opened.
 *
 * Opening it is disk work, so it cannot happen during composition; until it finishes there is
 * genuinely nothing to draw.
 */
private sealed interface SettingsState {
    data object Loading : SettingsState

    data class Loaded(val repository: SettingsRepository) : SettingsState

    data object Failed : SettingsState
}

/**
 * The app's settings, as plain Compose rows.
 *
 * This reads and writes the same default [android.content.SharedPreferences] file and the same keys
 * the old `PreferenceFragment` screens used, so nothing the user had set is lost and the player,
 * the downloader and the extractor keep seeing the values they already look for.
 *
 * Only the settings that mean something for a music app are here. Resolution, gesture controls,
 * popup-player geometry and the rest of the video-only options are deliberately absent rather than
 * shown and ignored.
 *
 * @param modifier applied to the scrolling list
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by produceState<SettingsState>(SettingsState.Loading, context) {
        value = withContext(Dispatchers.IO) {
            runCatching { SettingsRepository(context) }
                .onFailure { Log.e(TAG, "Could not open the preference file", it) }
                .fold(
                    onSuccess = { SettingsState.Loaded(it) },
                    onFailure = { SettingsState.Failed }
                )
        }
    }

    when (val current = state) {
        SettingsState.Loading -> StatusBox(modifier) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        SettingsState.Failed -> StatusBox(modifier) {
            StatusText(text = stringResource(R.string.music_settings_unavailable))
        }

        is SettingsState.Loaded -> SettingsList(
            repository = current.repository,
            modifier = modifier
        )
    }
}

/** The settings themselves, once the preference file is open. */
@Composable
private fun SettingsList(repository: SettingsRepository, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        AppearanceSection(repository = repository)
        PlaybackSection(repository = repository)
        DownloadsSection(repository = repository)
        ContentSection(repository = repository)
        HistorySection(repository = repository)
        AboutSection()
    }
}

/**
 * Theme choice.
 *
 * The night theme only has an effect while the main theme follows the device, so outside that case
 * the row stays visible but disabled and says why, rather than silently doing nothing.
 */
@Composable
private fun AppearanceSection(repository: SettingsRepository) {
    val context = LocalContext.current
    var theme by remember {
        mutableStateOf(repository.text(repository.themeKey, repository.themeDefault))
    }
    var nightTheme by remember {
        mutableStateOf(repository.text(repository.nightThemeKey, repository.nightThemeDefault))
    }
    val followsDevice = theme == repository.autoDeviceTheme

    SectionHeader(text = stringResource(R.string.settings_category_appearance_title))

    ListSetting(
        title = stringResource(R.string.theme_title),
        options = repository.themeOptions,
        value = theme,
        onValueChange = { chosen ->
            if (chosen != theme) {
                theme = chosen
                if (chosen == repository.autoDeviceTheme) {
                    toast(context, context.getString(R.string.select_night_theme_toast))
                }
                applyTheme(context, repository, repository.themeKey, chosen)
            }
        }
    )

    ListSetting(
        title = stringResource(R.string.night_theme_title),
        options = repository.nightThemeOptions,
        value = nightTheme,
        enabled = followsDevice,
        summary = if (followsDevice) {
            stringResource(
                R.string.night_theme_summary,
                repository.labelOf(repository.nightThemeOptions, nightTheme).orEmpty()
            )
        } else {
            stringResource(
                R.string.night_theme_available,
                stringResource(R.string.auto_device_theme_title)
            )
        },
        onValueChange = { chosen ->
            if (chosen != nightTheme) {
                nightTheme = chosen
                applyTheme(context, repository, repository.nightThemeKey, chosen)
            }
        }
    )
}

/**
 * The playback settings that apply to audio.
 *
 * Seek duration and inexact seek are coupled: ExoPlayer cannot land on a 5, 15 or 25 second step
 * while inexact seek is on, so turning it on both shortens the list and moves a now-impossible
 * stored value onto the next one up.
 */
@Composable
private fun PlaybackSection(repository: SettingsRepository) {
    val context = LocalContext.current
    var autoplay by remember {
        mutableStateOf(repository.text(repository.autoplayKey, repository.autoplayDefault))
    }
    var autoQueue by remember {
        mutableStateOf(repository.boolean(repository.autoQueueKey, false))
    }
    var resumeAfterInterruption by remember {
        mutableStateOf(repository.boolean(repository.resumeOnAudioFocusGainKey, false))
    }
    var inexactSeek by remember {
        mutableStateOf(repository.boolean(repository.useInexactSeekKey, false))
    }
    var seekDuration by remember {
        mutableStateOf(repository.seekDurationMillis().toString())
    }
    var confirmClearQueue by remember {
        mutableStateOf(repository.boolean(repository.clearQueueConfirmationKey, false))
    }
    var holdToAppend by remember {
        mutableStateOf(repository.boolean(repository.holdToAppendKey, true))
    }
    val seekOptions = remember(inexactSeek) { repository.seekDurationOptions(inexactSeek) }

    SectionHeader(text = stringResource(R.string.music_settings_category_playback))

    ListSetting(
        title = stringResource(R.string.autoplay_title),
        options = repository.autoplayOptions,
        value = autoplay,
        summary = stringResource(
            R.string.autoplay_summary,
            repository.labelOf(repository.autoplayOptions, autoplay).orEmpty()
        ),
        onValueChange = { chosen ->
            autoplay = chosen
            repository.setText(repository.autoplayKey, chosen)
        }
    )

    SwitchSetting(
        title = stringResource(R.string.auto_queue_title),
        summary = stringResource(R.string.auto_queue_summary),
        checked = autoQueue,
        onCheckedChange = { checked ->
            autoQueue = checked
            repository.setBoolean(repository.autoQueueKey, checked)
        }
    )

    SwitchSetting(
        title = stringResource(R.string.resume_on_audio_focus_gain_title),
        summary = stringResource(R.string.resume_on_audio_focus_gain_summary),
        checked = resumeAfterInterruption,
        onCheckedChange = { checked ->
            resumeAfterInterruption = checked
            repository.setBoolean(repository.resumeOnAudioFocusGainKey, checked)
        }
    )

    SwitchSetting(
        title = stringResource(R.string.use_inexact_seek_title),
        summary = stringResource(R.string.use_inexact_seek_summary),
        checked = inexactSeek,
        onCheckedChange = { checked ->
            inexactSeek = checked
            repository.setBoolean(repository.useInexactSeekKey, checked)
            val moved = repository.coerceSeekDuration(checked)
            if (moved != null) {
                seekDuration = repository.seekDurationMillis().toString()
                toast(context, context.getString(R.string.new_seek_duration_toast, moved))
            }
        }
    )

    ListSetting(
        title = stringResource(R.string.seek_duration_title),
        options = seekOptions,
        value = seekDuration,
        onValueChange = { chosen ->
            seekDuration = chosen
            repository.setText(repository.seekDurationKey, chosen)
        }
    )

    SwitchSetting(
        title = stringResource(R.string.clear_queue_confirmation_title),
        summary = stringResource(R.string.clear_queue_confirmation_summary),
        checked = confirmClearQueue,
        onCheckedChange = { checked ->
            confirmClearQueue = checked
            repository.setBoolean(repository.clearQueueConfirmationKey, checked)
        }
    )

    SwitchSetting(
        title = stringResource(R.string.show_hold_to_append_title),
        summary = stringResource(R.string.show_hold_to_append_summary),
        checked = holdToAppend,
        onCheckedChange = { checked ->
            holdToAppend = checked
            repository.setBoolean(repository.holdToAppendKey, checked)
        }
    )
}

/**
 * Where downloads land.
 *
 * From Android 10 the Storage Access Framework is the only way to write outside the app's own
 * directories, so on those versions the toggle is shown disabled and explains itself instead of
 * offering a choice the platform will not honour.
 */
@Composable
private fun DownloadsSection(repository: SettingsRepository) {
    val context = LocalContext.current
    val safIsForced = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    var askEveryTime by remember {
        mutableStateOf(repository.boolean(repository.downloadsStorageAskKey, false))
    }
    var useSaf by remember {
        mutableStateOf(NewPipeSettings.useStorageAccessFramework(context))
    }
    var audioFolder by remember {
        mutableStateOf(repository.folderSummary(repository.downloadPathAudioKey))
    }
    var videoFolder by remember {
        mutableStateOf(repository.folderSummary(repository.downloadPathVideoKey))
    }

    val pickAudioFolder = rememberFolderPicker(repository, repository.downloadPathAudioKey) {
        audioFolder = repository.folderSummary(repository.downloadPathAudioKey)
    }
    val pickVideoFolder = rememberFolderPicker(repository, repository.downloadPathVideoKey) {
        videoFolder = repository.folderSummary(repository.downloadPathVideoKey)
    }

    SectionHeader(text = stringResource(R.string.settings_category_downloads_title))

    SwitchSetting(
        title = stringResource(R.string.downloads_storage_ask_title),
        summary = if (safIsForced) {
            stringResource(R.string.downloads_storage_ask_summary_no_saf_notice)
        } else {
            stringResource(R.string.downloads_storage_ask_summary)
        },
        checked = askEveryTime,
        onCheckedChange = { checked ->
            askEveryTime = checked
            repository.setBoolean(repository.downloadsStorageAskKey, checked)
        }
    )

    SwitchSetting(
        title = stringResource(R.string.downloads_storage_use_saf_title),
        summary = if (safIsForced) {
            stringResource(R.string.downloads_storage_use_saf_summary_api_29)
        } else {
            stringResource(R.string.downloads_storage_use_saf_summary)
        },
        checked = useSaf,
        enabled = !safIsForced,
        onCheckedChange = { checked ->
            useSaf = checked
            repository.setBoolean(repository.storageUseSafKey, checked)
            // A stored folder cannot survive the switch: a SAF tree URI means nothing to the plain
            // file downloader and a file path means nothing to SAF. Clearing both sends the user
            // back to the pickers below instead of leaving an address that no longer resolves.
            repository.clearText(repository.downloadPathAudioKey)
            repository.clearText(repository.downloadPathVideoKey)
            audioFolder = null
            videoFolder = null
        }
    )

    SettingRow(
        title = stringResource(R.string.download_path_audio_title),
        summary = audioFolder ?: stringResource(R.string.download_path_audio_summary),
        enabled = !askEveryTime,
        onClick = pickAudioFolder
    )

    SettingRow(
        title = stringResource(R.string.download_path_title),
        summary = videoFolder ?: stringResource(R.string.download_path_summary),
        enabled = !askEveryTime,
        onClick = pickVideoFolder
    )
}

/** The language and country the extractor asks YouTube for. */
@Composable
private fun ContentSection(repository: SettingsRepository) {
    val systemDefault = repository.systemLocalization
    var language by remember {
        mutableStateOf(repository.text(repository.contentLanguageKey, systemDefault))
    }
    var country by remember {
        mutableStateOf(repository.text(repository.contentCountryKey, systemDefault))
    }

    SectionHeader(text = stringResource(R.string.content))

    ListSetting(
        title = stringResource(R.string.content_language_title),
        options = repository.languageOptions,
        value = language,
        onValueChange = { chosen ->
            language = chosen
            repository.setText(repository.contentLanguageKey, chosen)
        }
    )

    ListSetting(
        title = stringResource(R.string.default_content_country_title),
        options = repository.countryOptions,
        value = country,
        onValueChange = { chosen ->
            country = chosen
            repository.setText(repository.contentCountryKey, chosen)
        }
    )
}

/**
 * What the app remembers.
 *
 * Resuming playback needs the watch history to know where you stopped, so that row follows the
 * history toggle rather than standing on its own.
 */
@Composable
private fun HistorySection(repository: SettingsRepository) {
    var watchHistory by remember {
        mutableStateOf(repository.boolean(repository.watchHistoryKey, true))
    }
    var resumePlayback by remember {
        mutableStateOf(repository.boolean(repository.playbackResumeKey, true))
    }
    var searchHistory by remember {
        mutableStateOf(repository.boolean(repository.searchHistoryKey, true))
    }

    SectionHeader(text = stringResource(R.string.settings_category_history_title))

    SwitchSetting(
        title = stringResource(R.string.enable_watch_history_title),
        summary = stringResource(R.string.enable_watch_history_summary),
        checked = watchHistory,
        onCheckedChange = { checked ->
            watchHistory = checked
            repository.setBoolean(repository.watchHistoryKey, checked)
        }
    )

    SwitchSetting(
        title = stringResource(R.string.enable_playback_resume_title),
        summary = stringResource(R.string.enable_playback_resume_summary),
        checked = resumePlayback && watchHistory,
        enabled = watchHistory,
        onCheckedChange = { checked ->
            resumePlayback = checked
            repository.setBoolean(repository.playbackResumeKey, checked)
        }
    )

    SwitchSetting(
        title = stringResource(R.string.enable_search_history_title),
        summary = stringResource(R.string.enable_search_history_summary),
        checked = searchHistory,
        onCheckedChange = { checked ->
            searchHistory = checked
            repository.setBoolean(repository.searchHistoryKey, checked)
        }
    )
}

/** Version and a way out to the project, so a bug report can name the build it came from. */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val projectUrl = stringResource(R.string.github_url)

    SectionHeader(text = stringResource(R.string.music_settings_category_about))

    SettingRow(
        title = stringResource(R.string.app_name),
        summary = stringResource(R.string.music_settings_version, BuildConfig.VERSION_NAME)
    )

    SettingRow(
        title = stringResource(R.string.music_settings_project_page),
        summary = projectUrl,
        onClick = { ShareUtils.openUrlInBrowser(context, projectUrl) }
    )
}

/**
 * Builds the folder picker for one download-path preference.
 *
 * @param repository where the chosen folder is stored
 * @param key the preference the folder is stored under
 * @param onStored run on the main thread once a folder was accepted, so the row can refresh
 * @return call it to open the picker
 */
@Composable
private fun rememberFolderPicker(
    repository: SettingsRepository,
    key: String,
    onStored: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val picked = result.data?.data
        if (picked == null) {
            toast(context, context.getString(R.string.invalid_directory))
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val error = storeDownloadFolder(context, repository, key, picked)
            if (error == null) {
                onStored()
            } else {
                toast(context, context.getString(error))
            }
        }
    }

    return {
        val picker: Intent = StoredDirectoryHelper.getPicker(context)
        runCatching { launcher.launch(picker) }.onFailure {
            Log.e(TAG, "No app on this device can pick a folder", it)
            toast(context, context.getString(R.string.no_available_dir))
        }
    }
}

/**
 * Takes ownership of a folder the user just picked and stores it.
 *
 * Picking is not simply writing a string: a SAF tree has to have its permission granted and made
 * persistable, and either kind of folder has to be proved writable before it is worth remembering.
 * That is all disk and ContentResolver work, hence [Dispatchers.IO].
 *
 * @return a message to show the user, or null when the folder was accepted
 */
@StringRes
private suspend fun storeDownloadFolder(
    context: Context,
    repository: SettingsRepository,
    key: String,
    picked: Uri
): Int? = withContext(Dispatchers.IO) {
    val accepted = runCatching {
        if (FilePickerActivityHelper.isOwnFileUri(context, picked)) {
            val target = Utils.getFileForUri(picked)
            if (!target.canWrite()) {
                throw IOException("Cannot write to " + target.absolutePath)
            }
            Uri.fromFile(target)
        } else {
            context.grantUriPermission(
                context.packageName,
                picked,
                StoredDirectoryHelper.PERMISSION_FLAGS
            )
            if (!StoredDirectoryHelper(context, picked, null).canWrite()) {
                throw IOException("No write permission on " + picked)
            }
            picked
        }
    }.onFailure {
        Log.e(TAG, "Could not take the download folder " + picked, it)
    }.getOrNull()

    if (accepted == null) {
        R.string.no_available_dir
    } else {
        repository.setText(key, accepted.toString())
        null
    }
}

/**
 * Stores a theme choice and makes it visible immediately.
 *
 * `MusicActivity` resolves its colour scheme once, in `onCreate`, from these keys, so recreating
 * the activity is what actually repaints the app - the same thing the old preference screen did.
 */
private fun applyTheme(
    context: Context,
    repository: SettingsRepository,
    key: String,
    value: String
) {
    repository.setBoolean(KEY_THEME_CHANGE, true)
    repository.setText(key, value)
    // Day/night mode follows the main theme, never the night variant: pinning the app to dark
    // because a dark night theme was picked would defeat "Automatic (device theme)".
    ThemeHelper.setDayNightMode(
        context,
        repository.text(repository.themeKey, repository.themeDefault)
    )
    context.findActivity()?.let { ActivityCompat.recreate(it) }
}

/** A section heading such as "Appearance". */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * One settings row: a title over an optional grey summary, with an optional control on the right.
 *
 * A row whose dependency is off is dimmed and inert rather than hidden, so the setting still tells
 * the user it exists and its summary can explain what would bring it back.
 */
@Composable
private fun SettingRow(
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val clickable = if (onClick != null && enabled) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/** A boolean setting: the whole row toggles, and the switch shows the state. */
@Composable
private fun SwitchSetting(
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SettingRow(
        title = title,
        summary = summary,
        modifier = modifier,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

/**
 * A setting with a fixed set of values: the row shows the current choice and opens a radio dialog.
 *
 * @param summary overrides the default summary, which is the chosen option's own label
 */
@Composable
private fun ListSetting(
    title: String,
    options: List<SettingOption>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingRow(
        title = title,
        summary = summary ?: options.firstOrNull { it.value == value }?.label,
        modifier = modifier,
        enabled = enabled,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        OptionDialog(
            title = title,
            options = options,
            selected = value,
            onSelect = onValueChange,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * The radio-button picker for a [ListSetting].
 *
 * The list is lazy and opens already scrolled to the current choice, because the content country
 * list runs to 248 entries and scrolling to find the selected one would be absurd.
 */
@Composable
private fun OptionDialog(
    title: String,
    options: List<SettingOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedIndex = options.indexOfFirst { it.value == selected }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = DIALOG_MAX_HEIGHT)
            ) {
                items(options) { option ->
                    OptionRow(
                        option = option,
                        selected = option.value == selected,
                        onSelect = {
                            onSelect(option.value)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

/** One radio row inside an [OptionDialog]. */
@Composable
private fun OptionRow(option: SettingOption, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Centres a spinner or a message, so a loading or failed screen is never blank. */
@Composable
private fun StatusBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

/** A centred grey line for an empty or failed load. */
@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}

/** Walks out of whatever [ContextWrapper] Compose handed over to reach the hosting activity. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
