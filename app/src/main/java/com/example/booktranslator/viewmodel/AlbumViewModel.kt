package com.example.booktranslator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booktranslator.data.local.Photo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.booktranslator.data.repository.AlbumRepository
import com.example.booktranslator.data.local.Album
import com.example.booktranslator.data.repository.FirebaseRepository
import kotlinx.coroutines.runBlocking
import android.util.Log
import android.content.Context
import java.io.File

class AlbumViewModel(private val repository: AlbumRepository) : ViewModel() {

    val albums = repository.getAlbums().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    private val _translations = MutableStateFlow<Map<Long, String>>(emptyMap())
    val translations: StateFlow<Map<Long, String>> = _translations

    private val firebaseRepository = FirebaseRepository()

    fun getPhotos(albumId: Long): Flow<List<Photo>> = repository.getPhotos(albumId)

    fun getPhotosFlow(albumId: Long): Flow<List<Photo>> {
        return repository.getPhotosFlow(albumId)
    }

    fun addAlbum(title: String) {
        viewModelScope.launch {
            repository.addAlbum(title)
        }
    }

    fun deleteAlbum(album: Album) {
        viewModelScope.launch {
            repository.deleteAlbum(album)
        }
    }

    fun addPhoto(albumId: Long, imageUri: String) {
        viewModelScope.launch {
            repository.addPhoto(albumId, imageUri)
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }

    fun saveTranslation(photo: Photo, translatedText: String) {
        viewModelScope.launch {
            repository.saveTranslation(photo, translatedText)
            _translations.value = _translations.value.toMutableMap().apply {
                put(photo.id, translatedText)
            }
        }
    }

    fun uploadAlbumToCloud(albumId: Long, context: Context) {
        viewModelScope.launch {
            val album = repository.getAlbumById(albumId)
            if (album != null) {
                val photos = repository.getPhotos(albumId).first()
                try {
                    firebaseRepository.uploadAlbum(album, photos, context)
                    Log.d("AlbumVM", "Album ${album.id} wgrany do chmury")
                } catch (e: Exception) {
                    Log.e("AlbumVM", "Błąd przy wgrywaniu albumu: ${e.message}")
                }
            } else {
                Log.e("AlbumVM", "Album nie istnieje lokalnie")
            }
        }
    }

    fun downloadAlbumFromCloud(firestoreId: String, context: Context) {
        viewModelScope.launch {
            try {
                val (cloudAlbum, cloudPhotos) = firebaseRepository.downloadAlbum(firestoreId)

                val newAlbumId = repository.addAlbum(cloudAlbum.title, firestoreId)

                cloudPhotos.forEach { cloudPhoto ->
                    try {
                        val localFileName = "${cloudPhoto.id}.jpg"
                        val relativePath = "album_$newAlbumId/$localFileName"

                        val localPath = repository.saveImageFromUrl(context, cloudPhoto.imageUri, relativePath)

                        repository.addPhoto(newAlbumId, localPath)
                        Log.d("AlbumVM", "Zdjęcie ${cloudPhoto.id} pobrane i zapisane jako $localPath")
                    } catch (e: Exception) {
                        Log.e("AlbumVM", "Błąd przy pobieraniu zdjęcia ${cloudPhoto.id}: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("AlbumVM", "Błąd przy pobieraniu albumu z chmury: ${e.message}", e)
            }
        }
    }

    suspend fun getCloudAlbums(): List<Album> {
        val cloudData: List<Pair<String, String>> = firebaseRepository.getAllAlbums() // Pair<firestoreId, title>
        return cloudData.map { (id, title) ->
            Album(title = title, firestoreId = id) // id = lokalne Room id zostanie nadane przy dodaniu
        }
    }
}