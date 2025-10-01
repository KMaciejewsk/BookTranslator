package com.example.booktranslator.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums")
    fun getAllAlbums(): Flow<List<Album>>

    @Query("SELECT MAX(id) FROM albums")
    suspend fun getMaxId(): Long?

    @Insert
    suspend fun insertAlbum(album: Album): Long

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Query("SELECT * FROM albums WHERE id = :albumId LIMIT 1")
    suspend fun getAlbumById(albumId: Long): Album?
}