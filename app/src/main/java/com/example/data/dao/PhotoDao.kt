package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StampedPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM stamped_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<StampedPhoto>>

    @Query("SELECT * FROM stamped_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): StampedPhoto?

    @Query("SELECT * FROM stamped_photos WHERE category = :category ORDER BY timestamp DESC")
    fun getPhotosByCategory(category: String): Flow<List<StampedPhoto>>

    @Query("SELECT DISTINCT category FROM stamped_photos WHERE category != ''")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: StampedPhoto): Long

    @Update
    suspend fun updatePhoto(photo: StampedPhoto)

    @Delete
    suspend fun deletePhoto(photo: StampedPhoto)

    @Query("DELETE FROM stamped_photos WHERE id = :id")
    suspend fun deletePhotoById(id: Long)

    @Query("SELECT COUNT(*) FROM stamped_photos")
    fun getPhotoCount(): Flow<Int>

    @Query("DELETE FROM stamped_photos WHERE filePath LIKE '%SAMPLE%'")
    suspend fun cleanOldSampleData()
}
