package com.example.booktranslator.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE albumId = :albumId")
    fun getPhotosByAlbum(albumId: Long): Flow<List<Photo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo)

    @Delete
    suspend fun deletePhoto(photo: Photo)

    @Update
    suspend fun updatePhoto(photo: Photo)
}