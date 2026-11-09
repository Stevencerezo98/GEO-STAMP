package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_reports")
data class PdfReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val reportType: String = "PHOTO_REPORT", // PHOTO_REPORT, DOC_CONVERT, COLLAGE
    val pageCount: Int = 1,
    val photoCount: Int = 0,
    val fileSizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
