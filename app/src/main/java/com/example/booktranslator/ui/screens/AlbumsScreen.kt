package com.example.booktranslator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.booktranslator.viewmodel.AlbumViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.booktranslator.data.local.Album
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumViewModel,
    onAlbumClick: (Long) -> Unit,
    onAddAlbum: () -> Unit
) {
    val albums by viewModel.albums.collectAsState(initial = emptyList())
    var albumToDelete by remember { mutableStateOf<Album?>(null) }
    var cloudAlbums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var showCloudDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Album") },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            cloudAlbums = viewModel.getCloudAlbums()
                            showCloudDialog = true
                        }
                    }) {
                        Text("Pobierz z chmury")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlbum) { Text("+") }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(albums) { album ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .combinedClickable(
                            onClick = { onAlbumClick(album.id) },
                            onLongClick = { albumToDelete = album }
                        ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Text(
                        text = album.title,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    albumToDelete?.let { album ->
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text("Usuń album?") },
            text = { Text("Czy na pewno chcesz usunąć cały album?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAlbum(album)
                    albumToDelete = null
                }) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) { Text("Anuluj") }
            }
        )
    }

    if (showCloudDialog) {
        AlertDialog(
            onDismissRequest = { showCloudDialog = false },
            title = { Text("Wybierz album z chmury") },
            text = {
                Column {
                    cloudAlbums.forEach { album ->
                        TextButton(onClick = {
                            viewModel.downloadAlbumFromCloud(album.firestoreId ?: return@TextButton, context)
                            showCloudDialog = false
                        }) {
                            Text(album.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCloudDialog = false }) { Text("Anuluj") }
            }
        )
    }
}