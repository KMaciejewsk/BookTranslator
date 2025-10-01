package com.example.booktranslator.data.repository

import android.net.Uri
import com.example.booktranslator.data.local.Album
import com.example.booktranslator.data.local.Photo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import android.util.Log
import android.content.Context

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadAlbum(album: Album, photos: List<Photo>, context: Context) {
        try {
            val albumRef = db.collection("albums").document() // 🔑 nowy losowy ID
            val firestoreId = albumRef.id
            albumRef.set(mapOf("title" to album.title)).await()

            for (photo in photos) {
                try {
                    val uri = Uri.parse(photo.imageUri)
                    val extension = if (photo.imageUri.endsWith(".png")) ".png" else ".jpg"
                    val storageRef = storage.reference.child("Pictures/${photo.id}$extension")

                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw Exception("Pusty plik albo brak dostępu do $uri")

                    storageRef.putBytes(bytes).await()
                    val downloadUrl = storageRef.downloadUrl.await()

                    albumRef.collection("photos").document(photo.id.toString())
                        .set(
                            mapOf(
                                "id" to photo.id,
                                "imageUrl" to downloadUrl.toString()
                            )
                        ).await()
                } catch (e: Exception) {
                    Log.e("FirebaseRepo", "Błąd przy uploadzie zdjęcia ${photo.id}: ${e.message}", e)
                }
            }

            Log.d("FirebaseRepo", "Album ${album.title} wgrany do chmury z ID $firestoreId")
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Błąd przy uploadzie albumu: ${e.message}", e)
        }
    }

    suspend fun downloadAlbum(albumId: String): Pair<Album, List<Photo>> {
        val albumDoc = db.collection("albums").document(albumId).get().await()
        val title = albumDoc.getString("title") ?: "Bez nazwy"
        val localAlbumId = System.currentTimeMillis()
        val album = Album(id = localAlbumId, title = title)

        val photosSnap = db.collection("albums").document(albumId)
            .collection("photos").get().await()

        val photos = photosSnap.map {
            val photoId = it.getLong("id") ?: 0L
            val url = it.getString("imageUrl") ?: ""
            Photo(id = photoId, albumId = album.id, imageUri = url)
        }

        return album to photos
    }

    suspend fun getAllAlbums(): List<Pair<String, String>> {
        val snapshot = db.collection("albums").get().await()
        return snapshot.documents.map { doc ->
            doc.id to (doc.getString("title") ?: "Bez nazwy")
        }
    }
}