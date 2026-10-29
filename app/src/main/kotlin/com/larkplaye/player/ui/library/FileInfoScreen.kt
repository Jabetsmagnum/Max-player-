package com.larkplaye.player.ui.library

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.ui.common.Artwork
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.common.formatBytes
import com.larkplaye.player.ui.common.formatDate
import com.larkplaye.player.ui.common.formatDuration
import com.larkplaye.player.ui.common.shareTrack
import com.larkplaye.player.ui.navigation.Routes
import com.larkplaye.player.ui.theme.LocalLarkPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    trackId: Long,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val context = LocalContext.current
    val track = ui.rawLibrary.tracks.firstOrNull { it.id == trackId }

    if (track == null) {
        Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
            DetailTopBar(
                title = stringResource(R.string.file_info),
                onBack = { navController.popBackStack() },
            )
            EmptyState(icon = Icons.Rounded.Info, title = stringResource(R.string.no_results))
        }
        return
    }

    val extension = track.path.substringAfterLast('.', "").uppercase()
    val format = track.mimeType?.substringAfter('/')?.uppercase()
        ?.takeIf { it.isNotBlank() }
        ?: extension.ifBlank { "—" }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.file_info), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = palette.text,
                navigationIconContentColor = palette.text,
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Artwork(
                uri = track.artworkUri,
                modifier = Modifier.size(180.dp),
                corner = 22.dp,
                contentDescription = track.title,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                track.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))

            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    InfoRow(stringResource(R.string.tag_album), track.album)
                    InfoRow(stringResource(R.string.tag_genre), track.genre.orEmpty().ifBlank { "—" })
                    InfoRow(stringResource(R.string.track_number), track.trackNumber.takeIf { it > 0 }?.toString() ?: "—")
                    InfoRow(stringResource(R.string.year), track.year.takeIf { it > 0 }?.toString() ?: "—")
                    InfoDivider()
                    InfoRow(stringResource(R.string.file_format), format)
                    InfoRow(stringResource(R.string.file_size), formatBytes(track.sizeBytes))
                    InfoRow(stringResource(R.string.duration), formatDuration(track.durationMs))
                    InfoRow(
                        stringResource(R.string.bitrate),
                        if (track.bitrate > 0) "${track.bitrate / 1000} kbps" else "—",
                    )
                    InfoRow(stringResource(R.string.sample_rate), "—")
                    InfoDivider()
                    InfoRow(stringResource(R.string.date_added), formatDate(track.dateAdded))
                    InfoRow(stringResource(R.string.date_modified), formatDate(track.dateModified))
                    InfoRow(stringResource(R.string.file_path), track.path.ifBlank { "—" })
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { shareTrack(context, track) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share))
                }
                OutlinedButton(
                    onClick = {
                        if (!viewModel.canWriteSettings()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.write_settings_needed),
                                Toast.LENGTH_LONG,
                            ).show()
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                        } else {
                            val ok = viewModel.setRingtone(track)
                            Toast.makeText(
                                context,
                                context.getString(
                                    if (ok) R.string.ringtone_set else R.string.ringtone_failed,
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.RingVolume, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.set_ringtone), maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { navController.navigate(Routes.folder(track.folderPath)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.go_to_folder), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { navController.navigate(Routes.tags(track.id)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.edit_tags), maxLines = 1)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalLarkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSoft,
            modifier = Modifier.width(118.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        color = LocalLarkPalette.current.surfaceHigh,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
