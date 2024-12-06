package com.ykz.locationtesting

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
        mBinding.btnCalculateTime.setOnClickListener{
            //getDistanceMatrix(latitude ?: 0.0,longitude ?: 0.0,21.9788,96.0879)
            calculateDistanceAndTime(latitude ?: 0.0,longitude ?: 0.0)
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

    @SuppressLint("SetTextI18n")
    private fun calculateDistanceAndTime(userLat: Double, userLng: Double) {
        val restaurantLat = 21.9688  // Example latitude
        val restaurantLng = 96.0879  // Example longitude

        val userLocation = Location("User Location").apply {
            latitude = userLat
            longitude = userLng
        }

        val restaurantLocation = Location("Restaurant Location").apply {
            latitude = restaurantLat
            longitude = restaurantLng
        }

        val distanceInMeters = userLocation.distanceTo(restaurantLocation)
        val distanceInKm = distanceInMeters / 1000

        // Example: Assume average driving speed is 40 km/h
        val estimatedTimeInMinutes = (distanceInKm / 30) * 60

        Toast.makeText(this, "Distance : $distanceInKm km", Toast.LENGTH_SHORT).show()
        println("Distance: $distanceInKm km")
        mBinding.distanceAndTime.text = "Estimated Time: $estimatedTimeInMinutes mins"
        println("Estimated Time: $estimatedTimeInMinutes minutes")
    }

    fun getDistanceMatrix(userLat: Double, userLng: Double, restaurantLat: Double, restaurantLng: Double) {
        val apiKey = "AIzaSyBgCbFWzm1iT2vlpRfIml31M3NnmeQ1lHE"
        val origin = "$userLat,$userLng"
        val destination = "$restaurantLat,$restaurantLng"
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=$origin&destinations=$destination&mode=driving&key=$apiKey"

        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            { response ->
                val rows = response.getJSONArray("rows")
                val elements = rows.getJSONObject(0).getJSONArray("elements")
                val duration = elements.getJSONObject(0).getJSONObject("duration").getString("text")
                Toast.makeText(this, "Estimated Time: $duration", Toast.LENGTH_LONG).show()
            },
            { _ ->
                // Handle API request error
            })

        requestQueue.add(jsonObjectRequest)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun showLocationInGoogleMaps() {
        if (latitude != null && longitude != null) {
            // Create a Uri with the geo scheme and the coordinates
            val gmmIntentUri = Uri.parse("geo:0,0?q=$latitude,$longitude")
            //val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            // Create an Intent to open Google Maps
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            // Check if Google Maps is available on the device
            if (packageManager.queryIntentActivities(mapIntent, 0).isNotEmpty()) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(this, "No Map application found", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
        }
    }
}
