package com.example.fittracker

import android.app.job.JobParameters
import android.app.job.JobService

class MyJobService : JobService() {

    override fun onStartJob(
        params: JobParameters
    ): Boolean {

        Thread {

            try {

                // Exemple : travail de synchronisation

                jobFinished(params, false)

            } catch (e: Exception) {

                jobFinished(params, true)
            }

        }.start()

        return true
    }

    override fun onStopJob(
        params: JobParameters
    ): Boolean {

        return true
    }
}
