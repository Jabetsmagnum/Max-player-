package com.larkplaye.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.theme.LocalLarkPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    trackId: Long,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val track = ui.rawLibrary.tracks.firstOrNull { it.id == trackId }

    if (track == null) {
        Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
            DetailTopBar(
                title = stringResource(R.string.edit_tags),
                onBack = { navController.popBackStack() },
            )
            EmptyState(
                icon = Icons.Rounded.Edit,
                title = stringResource(R.string.no_results),
            )
        }
        return
    }

    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    var genre by remember(track.id) { mutableStateOf(track.genre.orEmpty()) }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.edit_tags), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        viewModel.updateTags(
                            track = track,
                            title = title.trim().ifBlank { track.title },
                            artist = artist.trim(),
                            album = album.trim(),
                            genre = genre.trim(),
                            onSuccess = { navController.popBackStack() },
                        )
                    },
                ) {
                    Text(stringResource(R.string.save), color = palette.accent)
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
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.edit_tags_desc),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSoft,
            )
            Spacer(Modifier.height(18.dp))
            TagField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.tag_title),
            )
            Spacer(Modifier.height(12.dp))
            TagField(
                value = artist,
                onValueChange = { artist = it },
                label = stringResource(R.string.tag_artist),
            )
            Spacer(Modifier.height(12.dp))
            TagField(
                value = album,
                onValueChange = { album = it },
                label = stringResource(R.string.tag_album),
            )
            Spacer(Modifier.height(12.dp))
            TagField(
                value = genre,
                onValueChange = { genre = it },
                label = stringResource(R.string.tag_genre),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = track.path,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSoft,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TagField(value: String, onValueChange: (String) -> Unit, label: String) {
    val palette = LocalLarkPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.surfaceHigh,
            focusedLabelColor = palette.accent,
            unfocusedLabelColor = palette.textSoft,
            focusedTextColor = palette.text,
            unfocusedTextColor = palette.text,
            cursorColor = palette.accent,
        ),
    )
}
