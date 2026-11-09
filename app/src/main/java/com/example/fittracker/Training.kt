package com.example.fittracker


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trainings")
data class Training(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val activity: String,

    val distance: Float,

    val duration: Long,

    val calories: Float
)
