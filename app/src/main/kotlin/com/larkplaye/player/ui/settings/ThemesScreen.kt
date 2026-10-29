package com.larkplaye.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.ui.library.LibraryViewModel
import com.larkplaye.player.ui.theme.LocalLarkPalette
import androidx.compose.material.icons.rounded.ArrowBack

private data class ThemeOption(
    val id: String,
    val labelRes: Int,
    val accent: Long,
    val start: Color,
    val end: Color,
)

private val themeOptions = listOf(
    ThemeOption("aurora", R.string.theme_aurora, 0xFFFFD54A, Color(0xFF4A3B0E), Color(0xFF0E0F12)),
    ThemeOption("sunset", R.string.theme_sunset, 0xFFFF9A62, Color(0xFF4A2415), Color(0xFF160F0D)),
    ThemeOption("ocean", R.string.theme_ocean, 0xFF5DC2FF, Color(0xFF12384B), Color(0xFF0C1118)),
    ThemeOption("forest", R.string.theme_forest, 0xFF55DDA1, Color(0xFF123A2D), Color(0xFF0C1512)),
    ThemeOption("candy", R.string.theme_candy, 0xFFFF86BE, Color(0xFF481A2F), Color(0xFF170D12)),
    ThemeOption("mono", R.string.theme_mono, 0xFFE4E4E4, Color(0xFF333333), Color(0xFF101010)),
)

private val accentOptions = listOf(
    0xFFFFD54A,
    0xFFFF9A62,
    0xFFFF86BE,
    0xFF5DC2FF,
    0xFF55DDA1,
    0xFFB39DFF,
    0xFFE4E4E4,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(navController: NavController, viewModel: LibraryViewModel) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val prefs = ui.prefs

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.theme)) },
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column {
                    Text(
                        stringResource(R.string.dark_mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "system" to stringResource(R.string.follow_system),
                            "light" to stringResource(R.string.light),
                            "dark" to stringResource(R.string.dark),
                        ).forEach { (value, label) ->
                            Surface(
                                color = if (prefs.darkMode == value) palette.accent else palette.surface,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.clickable { viewModel.setDarkMode(value) },
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (prefs.darkMode == value) Color(0xFF17130A) else palette.textSoft,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.accent_color),
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        accentOptions.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .then(
                                        if (prefs.accentColor == color) {
                                            Modifier.border(2.dp, palette.text, CircleShape)
                                        } else Modifier,
                                    )
                                    .clickable { viewModel.setAccent(color) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (prefs.accentColor == color) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF17130A),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.theme_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSoft,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
            items(themeOptions, key = { it.id }) { option ->
                val selected = prefs.themeId == option.id && prefs.accentColor == option.accent
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTheme(option.id, option.accent) }
                        .then(
                            if (selected) Modifier.border(2.dp, palette.accent, RoundedCornerShape(20.dp))
                            else Modifier,
                        ),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(option.start, option.end),
                                    ),
                                ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(option.accent)),
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .size(width = 54.dp, height = 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.55f)),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(option.labelRes),
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.text,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
