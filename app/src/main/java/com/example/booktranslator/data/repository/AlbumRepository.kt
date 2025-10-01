package com.example.booktranslator.data.repository

import com.example.booktranslator.data.local.Album
import com.example.booktranslator.data.local.AlbumDao
import com.example.booktranslator.data.local.Photo
import com.example.booktranslator.data.local.PhotoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

class AlbumRepository(private val albumDao: AlbumDao, private val photoDao: PhotoDao) {

    fun getAlbums(): Flow<List<Album>> = albumDao.getAllAlbums()

    fun getPhotos(albumId: Long): Flow<List<Photo>> = photoDao.getPhotosByAlbum(albumId)

    fun getPhotosFlow(albumId: Long): Flow<List<Photo>> {
        return photoDao.getPhotosByAlbum(albumId)
    }

    suspend fun addAlbum(title: String, firestoreId: String? = null): Long {
        val album = Album(title = title, firestoreId = firestoreId)
        return albumDao.insertAlbum(album)
    }

    suspend fun saveImageFromUrl(context: Context, url: String, relativePath: String): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, relativePath)
        file.parentFile?.mkdirs()

        java.net.URL(url).openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        file.absolutePath
    }

    suspend fun getAlbumById(albumId: Long): Album? {
        return albumDao.getAlbumById(albumId)
    }

    suspend fun deleteAlbum(album: Album) {
        val photos = photoDao.getPhotosByAlbum(album.id).first()
        photos.forEach { photoDao.deletePhoto(it) }
        albumDao.deleteAlbum(album)
    }

    suspend fun addPhoto(albumId: Long, imageUri: String) {
        photoDao.insertPhoto(Photo(albumId = albumId, imageUri = imageUri))
    }

    suspend fun deletePhoto(photo: Photo) {
        photoDao.deletePhoto(photo)
    }

    suspend fun saveTranslation(photo: Photo, translatedText: String) {
        photoDao.updatePhoto(photo.copy(translatedText = translatedText))
    }
}