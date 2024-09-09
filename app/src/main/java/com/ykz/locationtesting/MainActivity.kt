package com.ykz.locationtesting

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.ykz.locationtesting.databinding.ActivityMainBinding
import com.ykz.locationtesting.ui.theme.viewmodel.LocationViewModel
import com.ykz.locationtesting.ui.theme.viewmodel.UiState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var mBinding : ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: LocationViewModel by viewModels()
    private var latitude: Double? = null
    private var longitude: Double? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted, proceed with the action
                //checkLocationSettings()
                viewModel.fetchCurrentLocation(this)
            } else {
                // Permission is denied, show a message or alternative actions
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkAndRequestLocationPermission()
        observeViewModel()

        mBinding.btnTryAgain.setOnClickListener{
            viewModel.fetchCurrentLocation(this)
        }
        mBinding.btnShowMap.setOnClickListener {
            showLocationInGoogleMaps()
        }

    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                //checkLocationSettings()
                viewModel.fetchCurrentLocation(this)
            }
            else -> {
                // Request the permission
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun observeViewModel(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            mBinding.rlErrorState.visibility = View.GONE
                            mBinding.rlSuccessState.visibility = View.GONE
                            mBinding.progressLoading.visibility = View.VISIBLE
                            mBinding.btnShowMap.visibility = View.GONE
                        }
                        is UiState.Success -> {
                            mBinding.progressLoading.visibility = View.GONE
                            mBinding.rlErrorState.visibility = View.GONE
                            mBinding.rlSuccessState.visibility = View.VISIBLE
                            mBinding.btnShowMap.setBackgroundColor(getColor(R.color.purple_700))
                            mBinding.btnShowMap.visibility = View.VISIBLE
                            latitude = state.latitude
                            longitude = state.longitude
                            mBinding.tvLat.text = buildString {
                                append("Latitude :")
                                append(latitude)
                            }
                            mBinding.tvLong.text = buildString {
                                append("Longitude :" +
                                        "")
                                append(longitude)
                            }
                        }
                        is UiState.Error -> {
                            mBinding.progressLoading.visibility = View.GONE
                            mBinding.rlErrorState.visibility = View.VISIBLE
                            mBinding.rlSuccessState.visibility = View.GONE
                            mBinding.btnShowMap.visibility = View.GONE
                            mBinding.tvErrorText.text = state.message
                        }
                    }
                }
            }
        }
    }
//    private fun accessLocation() {
//        // Check for location permission before accessing location
//        if (ContextCompat.checkSelfPermission(
//                this,
//                android.Manifest.permission.ACCESS_FINE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
//                .addOnSuccessListener { location ->
//                    if (location != null) {
//                        latitude = location.latitude
//                        longitude = location.longitude
//                        mBinding.tvLat.text = latitude.toString()
//                        mBinding.tvLong.text = longitude.toString()
//                        // Use the latitude and longitude values
//                        Toast.makeText(
//                            this,
//                            "Lat: $latitude, Lon: $longitude",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    } else {
//                        Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                .addOnFailureListener {
//                    //Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
//                }
//
////                val locationRequest = LocationRequest.create().apply {
////                interval = 10000 // Set the interval in milliseconds for active location updates
////                fastestInterval = 5000 // Set the fastest interval in milliseconds
////                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
////            }
////            fusedLocationClient.requestLocationUpdates(
////                locationRequest,
////                locationCallback,
////                mainLooper
////            )
//        }
//    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // All location settings are satisfied
            viewModel.fetchCurrentLocation(this)
        }.addOnFailureListener { exception ->
            // Prompt user to enable location settings
            Toast.makeText(this, "Please enable location ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLocationInGoogleMaps() {
        if (latitude != null && longitude != null) {
            // Create a Uri with the geo scheme and the coordinates
            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            // Create an Intent to open Google Maps
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            // Check if Google Maps is available on the device
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
        }
    }
}
