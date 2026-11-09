package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PdfReport
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM pdf_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<PdfReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: PdfReport): Long

    @Delete
    suspend fun deleteReport(report: PdfReport)

    @Query("DELETE FROM pdf_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)
}
