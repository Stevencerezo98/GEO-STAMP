package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stamped_photos")
data class StampedPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val title: String = "",
    val notes: String = "",
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDateTime: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val address: String = "",
    val author: String = "",
    val project: String = "",
    val stampStyle: String = "MODERN_CARD",
    val isSelectedForReport: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
