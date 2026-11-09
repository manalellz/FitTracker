package com.example.fittracker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        return try {

            // Synchronisation des données
            // Exemple : préparation des données
            // pour le serveur

            Result.success()

        } catch (e: Exception) {

            Result.retry()
        }
    }
}
