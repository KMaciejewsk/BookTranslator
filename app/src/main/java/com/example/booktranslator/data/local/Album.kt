package com.example.booktranslator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val firestoreId: String? = null
)