package com.example.booktranslator.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import coil.compose.rememberAsyncImagePainter
import com.example.booktranslator.viewmodel.AlbumViewModel
import com.example.booktranslator.data.local.Photo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: AlbumViewModel,
    onAddPhotoFromGallery: () -> Unit,
    onAddPhotoFromCamera: () -> Unit,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit
) {
    val photos by viewModel.getPhotosFlow(albumId).collectAsState(initial = emptyList())
    var menuExpanded by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Album") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cofnij")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.uploadAlbumToCloud(albumId, context) }) {
                        Text("Wgraj")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { menuExpanded = true },
                    content = { Text("+ Dodaj zdjęcie") }
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Z galerii") },
                        onClick = {
                            menuExpanded = false
                            onAddPhotoFromGallery()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Z aparatu") },
                        onClick = {
                            menuExpanded = false
                            onAddPhotoFromCamera()
                        }
                    )
                }
            }
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
            items(
                items = photos,
                key = { photo -> photo.id }
            ) { photo ->
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onPhotoClick(photo) },
                            onLongClick = { photoToDelete = photo }
                        ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(photo.imageUri),
                        contentDescription = null,
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Usuń zdjęcie?") },
            text = { Text("Czy na pewno chcesz usunąć to zdjęcie?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePhoto(photo)
                    photoToDelete = null
                }) { Text("Tak") }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) { Text("Nie") }
            }
        )
    }
}