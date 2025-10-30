package com.example.gpstracker.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class LocationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationScreen()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1001
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    text = "Requesting location permission..."
                )
            }
        }
    }

    @Composable
    fun hasGps(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Composable
    fun LocationScreen() {
        if (hasGps()) {
            var location by remember { mutableStateOf<Location?>(null) }
            val context = LocalContext.current
            val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

            val locationCallback = remember {
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        for (loc in locationResult.locations) {
                            location = loc
                        }
                    }
                }
            }

            DisposableEffect(key1 = fusedLocationClient) {
                val locationRequest = LocationRequest.create().apply {
                    interval = 1000
                    fastestInterval = 500
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                onDispose {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }

            val locationText = if (location != null) {
                "Lat: ${location?.latitude}
Lon: ${location?.longitude}
Alt: ${location?.altitude}"
            } else {
                "Fetching location..."
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = locationText
            )

        } else {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = "NO GPS"
            )
        }
    }
}
