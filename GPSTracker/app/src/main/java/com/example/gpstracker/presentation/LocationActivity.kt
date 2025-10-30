package com.example.gpstracker.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationAvailability

class LocationActivity : ComponentActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Use mutableStateOf to track permission status for recomposition
    private var hasLocationPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initial permission check
        hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        setContent {
            // Use the WearApp composable to manage the UI based on permission status
            WearApp(hasPermission = hasLocationPermission)
        }

        // Request permission on startup if not granted
        if (!hasLocationPermission) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Handles the result of the runtime permission request.
     * Crucial for triggering a UI update after the user grants permission.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Update the state variable, which causes setContent to implicitly re-render
            hasLocationPermission = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

            // Re-call setContent to force the recomposition with the new state
            setContent {
                WearApp(hasPermission = hasLocationPermission)
            }
        }
    }

    @Composable
    fun hasGps(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }

    @Composable
    fun WearApp(hasPermission: Boolean) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasPermission) {
                // Only show the LocationScreen if permission is granted
                LocationScreen()
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.secondary,
                    text = "GPS permission pending or denied."
                )
            }
        }
    }

    // ⭐ New Composable for Real-Time Location Tracking
    @Composable
    fun LocationScreen() {
        if (!hasGps()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = "NO GPS ON DEVICE"
            )
            return
        }

        // State to hold the most recent location data (updates UI automatically)
        var location by remember { mutableStateOf<Location?>(null) }
        val context = LocalContext.current
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
        var isLocationAvailable by remember { mutableStateOf(true) }

        // 1. Define the LocationCallback to handle incoming updates
        val locationCallback = remember {
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { loc ->
                        location = loc // Updates the 'location' state, triggering UI refresh
                    }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    super.onLocationAvailability(locationAvailability)
                    isLocationAvailable = locationAvailability.isLocationAvailable
                }
            }
        }

        // 2. Use DisposableEffect to start/stop the update stream
        DisposableEffect(key1 = fusedLocationClient) {
            // Create the request parameters (e.g., 3-second interval, high accuracy)
            val locationRequest = LocationRequest.create().apply {
                interval = 3000L
                fastestInterval = 1000L
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            // Start the continuous location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Cleanup block: Stop updates when the Composable is removed (e.g., app paused)
            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }

        // 3. Display the real-time data
        val locationText = when {
            location != null -> {
                "Lat: ${String.format("%.4f", location?.latitude)}\n" +
                        "Lon: ${String.format("%.4f", location?.longitude)}\n" +
                        "Alt: ${String.format("%.1f", location?.altitude)} m\n" +
                        "Acc: ${String.format("%.1f", location?.accuracy)} m"
            }
            !isLocationAvailable -> "Searching for GPS Signal..."
            else -> "Fetching initial fix..."
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = if (location != null) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
            text = locationText,
            style = MaterialTheme.typography.caption1
        )
    }
}