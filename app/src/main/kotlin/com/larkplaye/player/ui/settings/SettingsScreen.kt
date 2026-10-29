package com.larkplaye.player.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.BuildConfig
import com.larkplaye.player.R
import com.larkplaye.player.lyrics.LyricsOverlayService
import com.larkplaye.player.ui.common.LarkDialog
import com.larkplaye.player.ui.common.openUrl
import com.larkplaye.player.ui.common.shareText
import com.larkplaye.player.ui.library.LibraryViewModel
import com.larkplaye.player.ui.navigation.Routes
import com.larkplaye.player.ui.theme.LocalLarkPalette

private const val DIALOG_DARK = "dark"
private const val DIALOG_SPEED = "speed"
private const val DIALOG_LANGUAGE = "language"
private const val DIALOG_LIBRARY = "library"
private const val DIALOG_CROSSFADE = "crossfade"
private const val DIALOG_PRIVACY = "privacy"
private const val DIALOG_OPEN_SOURCE = "open_source"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val context = LocalContext.current
    val prefs = ui.prefs
    val shareMessage = stringResource(R.string.share_app_message)
    var dialog by remember { mutableStateOf<String?>(null) }
    var lyricsSettings by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                prefs.floatingLyrics &&
                Settings.canDrawOverlays(context)
            ) {
                LyricsOverlayService.start(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = palette.text,
                actionIconContentColor = palette.text,
            ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 14.dp, end = 14.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsGroup(stringResource(R.string.appearance)) {
                    SettingRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.theme),
                        subtitle = themeName(prefs.themeId),
                        onClick = { navController.navigate(Routes.THEMES) },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.DarkMode,
                        title = stringResource(R.string.dark_mode),
                        subtitle = when (prefs.darkMode) {
                            "light" -> stringResource(R.string.light)
                            "dark" -> stringResource(R.string.dark)
                            else -> stringResource(R.string.follow_system)
                        },
                        onClick = { dialog = DIALOG_DARK },
                    )
                }
            }
            item {
                SettingsGroup(stringResource(R.string.scan_settings)) {
                    SettingRow(
                        icon = Icons.Rounded.LibraryMusic,
                        title = stringResource(R.string.scan_settings),
                        subtitle = stringResource(R.string.scan_settings_desc),
                        onClick = { dialog = DIALOG_LIBRARY },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.VisibilityOff,
                        title = stringResource(R.string.hidden_files),
                        subtitle = "${ui.hiddenTracks.size} ${stringResource(R.string.songs).lowercase()} · ${ui.hiddenFolders.size} ${stringResource(R.string.folders).lowercase()}",
                        onClick = { navController.navigate(Routes.HIDDEN) },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Refresh,
                        title = stringResource(R.string.refresh),
                        subtitle = if (ui.scanning) stringResource(R.string.refreshing) else stringResource(R.string.library_updated),
                        onClick = { viewModel.refresh() },
                    )
                }
            }
            item {
                SettingsGroup(stringResource(R.string.playback_settings)) {
                    SettingRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = stringResource(R.string.equalizer),
                        subtitle = stringResource(R.string.equalizer_desc),
                        onClick = { navController.navigate(Routes.EQUALIZER) },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Timer,
                        title = stringResource(R.string.sleep_timer),
                        subtitle = stringResource(R.string.sleep_timer_desc),
                        onClick = { navController.navigate(Routes.SLEEP) },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.playback_speed),
                        subtitle = if (prefs.playbackSpeed == 1f) stringResource(R.string.speed_normal) else "${prefs.playbackSpeed}×",
                        onClick = { dialog = DIALOG_SPEED },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SwitchRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = stringResource(R.string.skip_silence),
                        subtitle = stringResource(R.string.skip_silence_desc),
                        checked = prefs.skipSilence,
                        onCheckedChange = { viewModel.setSkipSilence(it) },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SwitchRow(
                        icon = Icons.Rounded.Headphones,
                        title = stringResource(R.string.gapless),
                        subtitle = stringResource(R.string.gapless_desc),
                        checked = prefs.gapless,
                        onCheckedChange = { viewModel.setGapless(it) },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SwitchRow(
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.crossfade),
                        subtitle = if (prefs.crossfade) {
                            "${prefs.crossfadeSeconds}s · ${stringResource(R.string.crossfade_desc)}"
                        } else {
                            stringResource(R.string.crossfade_desc)
                        },
                        checked = prefs.crossfade,
                        onCheckedChange = { viewModel.setCrossfade(it) },
                        extra = if (prefs.crossfade) {
                            {
                                TextButton(onClick = { dialog = DIALOG_CROSSFADE }) {
                                    Text("${prefs.crossfadeSeconds}s", color = palette.accent)
                                }
                            }
                        } else null,
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SwitchRow(
                        icon = Icons.Rounded.Headphones,
                        title = stringResource(R.string.headset_resume),
                        subtitle = stringResource(R.string.headset_resume_desc),
                        checked = prefs.headsetResume,
                        onCheckedChange = { viewModel.setHeadsetResume(it) },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SwitchRow(
                        icon = Icons.Rounded.LibraryMusic,
                        title = stringResource(R.string.play_together),
                        subtitle = stringResource(R.string.play_together_desc),
                        checked = prefs.playTogether,
                        onCheckedChange = { viewModel.setPlayTogether(it) },
                    )
                }
            }
            item {
                SettingsGroup(stringResource(R.string.lyrics)) {
                    SwitchRow(
                        icon = Icons.Rounded.Notes,
                        title = stringResource(R.string.floating_lyrics),
                        subtitle = stringResource(R.string.floating_lyrics_desc),
                        checked = prefs.floatingLyrics,
                        onCheckedChange = { enabled ->
                            viewModel.setFloatingLyrics(enabled)
                            if (enabled) {
                                if (Settings.canDrawOverlays(context)) {
                                    LyricsOverlayService.start(context)
                                } else {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}"),
                                            ),
                                        )
                                    }
                                }
                            } else {
                                LyricsOverlayService.stop(context)
                            }
                        },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.lyrics_font_size),
                        subtitle = "${(prefs.lyricsFontScale * 100).toInt()}%",
                        onClick = { lyricsSettings = true },
                    )
                }
            }
            item {
                SettingsGroup(stringResource(R.string.notifications)) {
                    SettingRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.notification_settings),
                        subtitle = stringResource(R.string.notification_settings_desc),
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    },
                                )
                            }
                        },
                    )
                }
            }
            item {
                SettingsGroup(stringResource(R.string.backup_restore)) {
                    SettingRow(
                        icon = Icons.Rounded.Save,
                        title = stringResource(R.string.backup_restore),
                        subtitle = stringResource(R.string.backup_restore_desc),
                        onClick = { navController.navigate(Routes.BACKUP) },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Translate,
                        title = stringResource(R.string.language),
                        subtitle = when (prefs.language) {
                            "fr" -> stringResource(R.string.language_french)
                            "en" -> stringResource(R.string.language_english)
                            else -> stringResource(R.string.language_system)
                        },
                        onClick = { dialog = DIALOG_LANGUAGE },
                    )
                }
            }
            item {
                SettingsGroup(stringResource(R.string.about)) {
                    SettingRow(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.version),
                        subtitle = "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}",
                        onClick = { },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Shield,
                        title = stringResource(R.string.privacy),
                        subtitle = stringResource(R.string.privacy_desc),
                        onClick = { dialog = DIALOG_PRIVACY },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Share,
                        title = stringResource(R.string.share_app),
                        subtitle = stringResource(R.string.tab_music),
                        onClick = {
                            shareText(context, shareMessage)
                        },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Star,
                        title = stringResource(R.string.rate_app),
                        subtitle = stringResource(R.string.about_desc),
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=${context.packageName}"),
                                    ),
                                )
                            }.onFailure {
                                openUrl(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
                            }
                        },
                    )
                    HorizontalDivider(color = palette.surfaceHigh.copy(alpha = 0.6f))
                    SettingRow(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.open_source),
                        subtitle = "Kotlin · Jetpack Compose · Media3",
                        onClick = { dialog = DIALOG_OPEN_SOURCE },
                    )
                }
            }
        }
    }

    when (dialog) {
        DIALOG_DARK -> RadioDialog(
            title = stringResource(R.string.dark_mode),
            options = listOf(
                "system" to stringResource(R.string.follow_system),
                "light" to stringResource(R.string.light),
                "dark" to stringResource(R.string.dark),
            ),
            selected = prefs.darkMode,
            onSelect = { viewModel.setDarkMode(it); dialog = null },
            onDismiss = { dialog = null },
        )
        DIALOG_SPEED -> SpeedDialog(
            current = prefs.playbackSpeed,
            onSelect = { speed ->
                viewModel.setPlaybackSpeed(speed)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        DIALOG_LANGUAGE -> RadioDialog(
            title = stringResource(R.string.language),
            options = listOf(
                "system" to stringResource(R.string.language_system),
                "en" to stringResource(R.string.language_english),
                "fr" to stringResource(R.string.language_french),
            ),
            selected = prefs.language,
            onSelect = { code ->
                viewModel.setLanguage(code)
                val locales = if (code == "system") {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(code)
                }
                AppCompatDelegate.setApplicationLocales(locales)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        DIALOG_LIBRARY -> LibraryFilterDialog(
            minDuration = prefs.minDurationSec,
            minSize = prefs.minSizeKb,
            onApply = { duration, size ->
                viewModel.setMinDuration(duration)
                viewModel.setMinSize(size)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        DIALOG_CROSSFADE -> CrossfadeDialog(
            seconds = prefs.crossfadeSeconds,
            onApply = { viewModel.setCrossfadeSeconds(it); dialog = null },
            onDismiss = { dialog = null },
        )
        DIALOG_PRIVACY -> LarkDialog(
            title = stringResource(R.string.privacy),
            message = stringResource(R.string.privacy_desc),
            confirmLabel = stringResource(R.string.close),
            onConfirm = { dialog = null },
            onDismiss = { dialog = null },
        )
        DIALOG_OPEN_SOURCE -> LarkDialog(
            title = stringResource(R.string.open_source),
            message = stringResource(R.string.open_source_desc),
            confirmLabel = stringResource(R.string.close),
            onConfirm = { dialog = null },
            onDismiss = { dialog = null },
        )
    }

    if (lyricsSettings) {
        var scale by remember { mutableStateOf(prefs.lyricsFontScale) }
        AlertDialog(
            onDismissRequest = { lyricsSettings = false },
            title = { Text(stringResource(R.string.lyrics_font_size)) },
            text = {
                Column {
                    Text("${(scale * 100).toInt()}%")
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.7f..1.6f,
                        onValueChangeFinished = { viewModel.setLyricsFontScale(scale) },
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { lyricsSettings = false }) { Text(stringResource(R.string.done)) }
            },
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalLarkPalette.current
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSoft,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalLarkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.text)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textSoft.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val palette = LocalLarkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.text)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        extra?.invoke()
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF17130A),
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.textSoft,
                uncheckedTrackColor = palette.surfaceHigh,
            ),
        )
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    val palette = LocalLarkPalette.current
    Surface(
        color = palette.accentSoft,
        shape = CircleShape,
        modifier = Modifier.size(36.dp),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun themeName(id: String): String = when (id) {
    "sunset" -> stringResource(R.string.theme_sunset)
    "ocean" -> stringResource(R.string.theme_ocean)
    "forest" -> stringResource(R.string.theme_forest)
    "candy" -> stringResource(R.string.theme_candy)
    "mono" -> stringResource(R.string.theme_mono)
    else -> stringResource(R.string.theme_aurora)
}

@Composable
private fun RadioDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun SpeedDialog(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalLarkPalette.current
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playback_speed)) },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(speed) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (speed == 1f) stringResource(R.string.speed_normal) else "$speed×",
                            color = if (speed == current) palette.accent else palette.text,
                        )
                        if (speed == current) Text("✓", color = palette.accent)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun LibraryFilterDialog(
    minDuration: Int,
    minSize: Int,
    onApply: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var duration by remember { mutableStateOf(minDuration) }
    var size by remember { mutableStateOf(minSize) }
    val palette = LocalLarkPalette.current
    val durations = listOf(0, 5, 10, 30, 60)
    val sizes = listOf(0, 100, 500, 1024)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scan_settings)) },
        text = {
            Column {
                Text(stringResource(R.string.duration), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durations.forEach { seconds ->
                        FilterChipText(
                            text = if (seconds == 0) stringResource(R.string.off) else "${seconds}s",
                            selected = duration == seconds,
                            onClick = { duration = seconds },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.file_size), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizes.forEach { kb ->
                        FilterChipText(
                            text = if (kb == 0) stringResource(R.string.off) else if (kb >= 1024) "1 MB" else "$kb KB",
                            selected = size == kb,
                            onClick = { size = kb },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(duration, size) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun FilterChipText(text: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalLarkPalette.current
    Surface(
        color = if (selected) palette.accent else palette.surfaceHigh,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color(0xFF17130A) else palette.textSoft,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun CrossfadeDialog(seconds: Int, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(seconds.toFloat()) }
    val palette = LocalLarkPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crossfade)) },
        text = {
            Column {
                Text("${value.toInt()} s", color = palette.accent)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 1f..15f,
                    steps = 13,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(value.toInt()) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
