
package com.example.fittracker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {

    // 1. Déclaration des composants
    private lateinit var txtDuration: TextView
    private lateinit var txtDistance: TextView
    private lateinit var txtCalories: TextView
    private lateinit var txtStatus: TextView

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    // 2. GPS
    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    // 3. Variables de l'entraînement
    private var distance = 0f
    private var startTime = 0L
    private var lastLocation: Location? = null

    private var isTraining = false

    // 4. Coroutine pour calculer la durée
    private var timerJob: Job? = null

    companion object {
        private const val LOCATION_REQUEST_CODE = 100
    }

    // 5. Réception des positions GPS
    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(
            result: LocationResult
        ) {
            super.onLocationResult(result)

            if (!isTraining) return

            for (location in result.locations) {

                // Ignorer les positions imprécises
                if (location.accuracy > 50f) continue

                val previousLocation = lastLocation

                if (previousLocation != null) {

                    val distanceStep =
                        previousLocation.distanceTo(location)

                    // Éviter les déplacements GPS incorrects
                    if (distanceStep > 0 && distanceStep < 100) {
                        distance += distanceStep
                    }
                }

                lastLocation = location

                updateDistanceAndCalories()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // 6. Initialisation des composants
        txtDuration = findViewById(R.id.txtDuration)
        txtDistance = findViewById(R.id.txtDistance)
        txtCalories = findViewById(R.id.txtCalories)
        txtStatus = findViewById(R.id.txtStatus)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        // 7. Initialisation du GPS
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(3000L)
            .build()

        // 8. État initial
        btnStop.isEnabled = false

        txtDuration.text = "Durée : 00:00"
        txtDistance.text = "Distance : 0.00 km"
        txtCalories.text = "Calories : 0 kcal"
        txtStatus.text = "En attente..."

        // 9. Bouton démarrer
        btnStart.setOnClickListener {
            requestLocationPermission()
        }

        // 10. Bouton arrêter
        btnStop.setOnClickListener {
            stopTraining()
        }
    }

    // 11. Demander les permissions GPS
    private fun requestLocationPermission() {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {

            startTraining()

        } else {

            EasyPermissions.requestPermissions(
                this,
                "L'application a besoin de votre localisation " +
                        "pour calculer la distance de votre entraînement.",
                LOCATION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    // 12. Recevoir le résultat de la permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this
        )

        if (requestCode == LOCATION_REQUEST_CODE) {

            val fineGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            val coarseGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            if (fineGranted || coarseGranted) {
                startTraining()
            } else {
                Toast.makeText(
                    this,
                    "Permission de localisation refusée",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // 13. Démarrer l'entraînement
    private fun startTraining() {

        if (isTraining) return

        distance = 0f
        lastLocation = null
        startTime = System.currentTimeMillis()

        isTraining = true

        btnStart.isEnabled = false
        btnStop.isEnabled = true

        txtStatus.text = "Entraînement en cours..."

        updateDistanceAndCalories()

        startTimer()

        startLocationUpdates()
    }

    // 14. Démarrer les mises à jour GPS
    private fun startLocationUpdates() {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            stopTraining()
            return
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Permission GPS non disponible",
                Toast.LENGTH_SHORT
            ).show()

            stopTraining()
        }
    }

    // 15. Calculer la durée avec Coroutine
    private fun startTimer() {

        timerJob?.cancel()

        timerJob = lifecycleScope.launch {

            while (isActive && isTraining) {

                val elapsed =
                    (System.currentTimeMillis() - startTime) / 1000

                val minutes = elapsed / 60
                val seconds = elapsed % 60

                txtDuration.text = String.format(
                    "Durée : %02d:%02d",
                    minutes,
                    seconds
                )

                delay(1000)
            }
        }
    }

    // 16. Calculer la distance et les calories
    private fun updateDistanceAndCalories() {

        val km = distance / 1000f

        // Estimation simple : 60 kcal par km
        val calories = km * 60f

        txtDistance.text = String.format(
            java.util.Locale.US,
            "Distance : %.2f km",
            km
        )

        txtCalories.text = String.format(
            java.util.Locale.US,
            "Calories : %.0f kcal",
            calories
        )
    }

    // 17. Arrêter l'entraînement
    private fun stopTraining() {

        if (!isTraining) return

        isTraining = false

        timerJob?.cancel()

        fusedLocationClient.removeLocationUpdates(
            locationCallback
        )

        btnStart.isEnabled = true
        btnStop.isEnabled = false

        txtStatus.text = "Entraînement terminé"

        Toast.makeText(
            this,
            "Entraînement enregistré temporairement",
            Toast.LENGTH_SHORT
        ).show()
    }

    // 18. Nettoyer les ressources
    override fun onDestroy() {

        isTraining = false

        timerJob?.cancel()

        fusedLocationClient.removeLocationUpdates(
            locationCallback
        )

        super.onDestroy()
    }
}
