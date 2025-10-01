package com.example.booktranslator.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.booktranslator.data.local.Photo
import com.example.booktranslator.utils.extractTextFromImage
import com.example.booktranslator.utils.translateText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.booktranslator.viewmodel.AlbumViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoTranslationPagerScreen(
    albumId: Long,
    selectedPhoto: Photo,
    viewModel: AlbumViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val photos by viewModel.getPhotosFlow(albumId).collectAsState(initial = emptyList())

    val pagerState = rememberPagerState(
        pageCount = { photos.size },
        initialPage = 0
    )

    LaunchedEffect(photos) {
        val index = photos.indexOfFirst { it.id == selectedPhoto.id }
        if (index >= 0) {
            pagerState.scrollToPage(index)
        }
    }

    var extractedText by remember { mutableStateOf("Wykrywanie tekstu...") }
    var translatedText by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Polski") }
    val languages = listOf("Polski" to "pl", "Angielski" to "en", "Niemiecki" to "de")
    var languageMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState, photos) {
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                if (photos.isNotEmpty()) {
                    val photo = photos[page]
                    extractedText = "Wykrywanie tekstu..."
                    translatedText = ""
                    extractTextFromImage(context, Uri.parse(photo.imageUri)) { text ->
                        extractedText = text
                        translateText(text, "pl") { translated ->
                            translatedText = translated
                        }
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tłumaczenie zdjęcia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cofnij")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { languageMenuExpanded = true },
                    content = { Text("Język: $selectedLanguage") }
                )
                DropdownMenu(
                    expanded = languageMenuExpanded,
                    onDismissRequest = { languageMenuExpanded = false }
                ) {
                    languages.forEach { (langName, langCode) ->
                        DropdownMenuItem(
                            text = { Text(langName) },
                            onClick = {
                                selectedLanguage = langName
                                languageMenuExpanded = false
                                translateText(extractedText, langCode) { translated ->
                                    translatedText = translated
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (photos.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) { page ->
                    val photo = photos[page]
                    Image(
                        painter = rememberAsyncImagePainter(photo.imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Wykryty tekst:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = extractedText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tłumaczenie:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = translatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Brak zdjęć w albumie")
            }
        }
    }
}