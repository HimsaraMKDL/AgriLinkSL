package com.example.agrilinksl

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FarmLocationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private lateinit var farmerId: String

    private lateinit var tvLocationFarmerName: TextView
    private lateinit var tvLocationFarmName: TextView
    private lateinit var tvFarmAddress: TextView
    private lateinit var tvMapStatus: TextView
    private lateinit var tvCoordinates: TextView

    private lateinit var btnUseCurrentLocation: MaterialButton
    private lateinit var btnGetDirections: MaterialButton

    private var latitude: Double? = null
    private var longitude: Double? = null

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farm_location)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews()

        farmerId =
            intent.getStringExtra("FARMER_ID")
                ?: auth.currentUser?.uid
                        ?: ""

        if (farmerId.isBlank()) {
            Toast.makeText(
                this,
                "Farmer information not available",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        setupScreenForUser()
        setupClickListeners()
        loadFarmLocation()
    }

    private fun initializeViews() {
        tvLocationFarmerName =
            findViewById(R.id.tvLocationFarmerName)

        tvLocationFarmName =
            findViewById(R.id.tvLocationFarmName)

        tvFarmAddress =
            findViewById(R.id.tvFarmAddress)

        tvMapStatus =
            findViewById(R.id.tvMapStatus)

        tvCoordinates =
            findViewById(R.id.tvCoordinates)

        btnUseCurrentLocation =
            findViewById(R.id.btnUseCurrentLocation)

        btnGetDirections =
            findViewById(R.id.btnGetDirections)
    }

    private fun setupScreenForUser() {
        val currentUserId =
            auth.currentUser?.uid

        if (currentUserId != farmerId) {
            btnUseCurrentLocation.visibility =
                View.GONE
        }
    }

    private fun setupClickListeners() {

        findViewById<TextView>(
            R.id.tvBackFarmLocation
        ).setOnClickListener {
            finish()
        }

        btnUseCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        btnGetDirections.setOnClickListener {
            openDirections()
        }
    }

    private fun checkLocationPermission() {

        val finePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            finePermission == PackageManager.PERMISSION_GRANTED ||
            coarsePermission == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getCurrentLocation() {

        val finePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            finePermission != PackageManager.PERMISSION_GRANTED &&
            coarsePermission != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        btnUseCurrentLocation.isEnabled = false
        btnUseCurrentLocation.text = "GETTING LOCATION..."

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    latitude =
                        location.latitude

                    longitude =
                        location.longitude

                    saveLocationToFirestore()

                } else {

                    btnUseCurrentLocation.isEnabled = true
                    btnUseCurrentLocation.text = "📍 USE CURRENT LOCATION"

                    Toast.makeText(
                        this,
                        "Location not available. Turn on location and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->

                btnUseCurrentLocation.isEnabled = true
                btnUseCurrentLocation.text = "📍 USE CURRENT LOCATION"

                Toast.makeText(
                    this,
                    "Failed to get location: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun saveLocationToFirestore() {

        val lat =
            latitude ?: return

        val lng =
            longitude ?: return

        val updates =
            hashMapOf<String, Any>(
                "latitude" to lat,
                "longitude" to lng
            )

        firestore.collection("users")
            .document(farmerId)
            .update(updates)
            .addOnSuccessListener {

                btnUseCurrentLocation.isEnabled = true
                btnUseCurrentLocation.text = "📍 UPDATE CURRENT LOCATION"

                updateLocationStatus()

                Toast.makeText(
                    this,
                    "Farm location saved successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                btnUseCurrentLocation.isEnabled = true
                btnUseCurrentLocation.text = "📍 USE CURRENT LOCATION"

                Toast.makeText(
                    this,
                    "Failed to save location: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadFarmLocation() {

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    return@addOnSuccessListener
                }

                val farmerName =
                    document.getString("name")
                        ?: "Farmer"

                val farmName =
                    document.getString("farmName")
                        ?: ""

                val address =
                    document.getString("address")
                        ?: ""

                latitude =
                    document.getDouble("latitude")

                longitude =
                    document.getDouble("longitude")

                tvLocationFarmerName.text =
                    farmerName

                tvLocationFarmName.text =
                    farmName.ifBlank {
                        "Farm / Shop name not set"
                    }

                tvFarmAddress.text =
                    address.ifBlank {
                        "Location not set"
                    }

                updateLocationStatus()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to load farm location: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateLocationStatus() {

        val lat =
            latitude

        val lng =
            longitude

        if (lat != null && lng != null) {

            tvMapStatus.text =
                "Farm location available"

            tvCoordinates.text =
                "Latitude: $lat   Longitude: $lng"

        } else {

            tvMapStatus.text =
                "Farm location not set"

            tvCoordinates.text =
                "Latitude: --   Longitude: --"
        }
    }

    private fun openDirections() {

        val lat =
            latitude

        val lng =
            longitude

        if (lat == null || lng == null) {

            Toast.makeText(
                this,
                "Farmer has not added a location yet",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val navigationUri =
            "google.navigation:q=$lat,$lng".toUri()

        val mapsIntent =
            Intent(
                Intent.ACTION_VIEW,
                navigationUri
            ).apply {
                setPackage(
                    "com.google.android.apps.maps"
                )
            }

        try {

            startActivity(mapsIntent)

        } catch (exception: ActivityNotFoundException) {

            val browserUri =
                "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                    .toUri()

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    browserUri
                )
            )
        }
    }
}