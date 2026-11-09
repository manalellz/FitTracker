package com.example.fittracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Training::class],
    version = 1
)
abstract class TrainingDatabase : RoomDatabase() {

    abstract fun trainingDao(): TrainingDao

    companion object {

        @Volatile
        private var INSTANCE: TrainingDatabase? = null

        fun getDatabase(context: Context): TrainingDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrainingDatabase::class.java,
                    "fittracker_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
