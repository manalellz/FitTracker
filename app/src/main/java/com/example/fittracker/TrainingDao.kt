package com.example.fittracker


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrainingDao {

    @Insert
    suspend fun insertTraining(training: Training)

    @Query("SELECT * FROM trainings ORDER BY id DESC")
    suspend fun getAllTrainings(): List<Training>
}
