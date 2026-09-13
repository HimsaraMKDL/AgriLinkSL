package com.example.agrilinksl

import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TransportProviderSelectionActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var containerProviders: LinearLayout
    private lateinit var tvArea: TextView

    private var productId = ""
    private var farmerId = ""
    private var productName = ""
    private var unit = "kg"

    private var quantity = 1
    private var pricePerUnit = 0.0
    private var totalAmount = 0.0

    private var farmerLatitude: Double? = null
    private var farmerLongitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            R.layout.activity_transport_provider_selection
        )

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        productId =
            intent.getStringExtra("PRODUCT_ID") ?: ""

        quantity =
            intent.getIntExtra(
                "QUANTITY",
                1
            )

        if (productId.isBlank()) {
            finish()
            return
        }

        containerProviders =
            findViewById(
                R.id.containerTransportProviders
            )

        tvArea =
            findViewById(
                R.id.tvTransportArea
            )

        findViewById<TextView>(
            R.id.tvBackTransportProviders
        ).setOnClickListener {
            finish()
        }

        loadProduct()
    }

    private fun loadProduct() {

        firestore.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { productDoc ->

                if (!productDoc.exists()) {

                    Toast.makeText(
                        this,
                        "Product not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                productName =
                    productDoc.getString("name")
                        ?: "Product"

                unit =
                    productDoc.getString("unit")
                        ?: "kg"

                pricePerUnit =
                    productDoc.getDouble("price")
                        ?: 0.0

                farmerId =
                    productDoc.getString("farmerId")
                        ?: ""

                totalAmount =
                    pricePerUnit * quantity

                loadFarmer()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load product: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadFarmer() {

        if (farmerId.isBlank()) {
            loadProviders()
            return
        }

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener { document ->

                farmerLatitude =
                    document.getDouble("latitude")

                farmerLongitude =
                    document.getDouble("longitude")

                val address =
                    document.getString("address")
                        ?: ""

                tvArea.text =
                    if (address.isBlank()) {
                        "📍 Nearby Area"
                    } else {
                        "📍 $address"
                    }

                loadProviders()
            }
            .addOnFailureListener {
                loadProviders()
            }
    }

    private fun loadProviders() {

        containerProviders.removeAllViews()

        /*
         * Supports both values in case an older Transport account
         * was saved using "Transport".
         */
        firestore.collection("users")
            .whereIn(
                "role",
                listOf(
                    "Transport Provider",
                    "Transport"
                )
            )
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {

                    showNoProviders()
                    return@addOnSuccessListener
                }

                data class ProviderData(
                    val id: String,
                    val name: String,
                    val phone: String,
                    val address: String,
                    val vehicleType: String,
                    val capacityKg: Double,
                    val rating: Double,
                    val latitude: Double?,
                    val longitude: Double?,
                    val distanceKm: Float?
                )

                val providers =
                    mutableListOf<ProviderData>()

                for (document in snapshot.documents) {

                    val available =
                        document.getBoolean("available")
                            ?: true

                    if (!available) {
                        continue
                    }

                    val lat =
                        document.getDouble("latitude")

                    val lng =
                        document.getDouble("longitude")

                    providers.add(
                        ProviderData(
                            id = document.id,

                            name =
                                document.getString("name")
                                    ?: "Transport Provider",

                            phone =
                                document.getString("phone")
                                    ?: "",

                            address =
                                document.getString("address")
                                    ?: "",

                            vehicleType =
                                document.getString("vehicleType")
                                    ?: "Transport Service",

                            capacityKg =
                                document.getDouble("capacityKg")
                                    ?: 0.0,

                            rating =
                                document.getDouble("rating")
                                    ?: 4.8,

                            latitude = lat,
                            longitude = lng,

                            distanceKm =
                                calculateDistance(
                                    lat,
                                    lng
                                )
                        )
                    )
                }

                val sorted =
                    providers.sortedWith(
                        compareBy<ProviderData> {
                            it.distanceKm == null
                        }.thenBy {
                            it.distanceKm
                        }
                    )

                for (provider in sorted) {

                    addProviderCard(
                        providerId =
                            provider.id,

                        providerName =
                            provider.name,

                        vehicleType =
                            provider.vehicleType,

                        capacityKg =
                            provider.capacityKg,

                        rating =
                            provider.rating,

                        address =
                            provider.address,

                        distanceKm =
                            provider.distanceKm
                    )
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load providers: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun addProviderCard(
        providerId: String,
        providerName: String,
        vehicleType: String,
        capacityKg: Double,
        rating: Double,
        address: String,
        distanceKm: Float?
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_transport_provider,
                    containerProviders,
                    false
                )

        val tvName =
            view.findViewById<TextView>(
                R.id.tvTransportProviderName
            )

        val tvVehicle =
            view.findViewById<TextView>(
                R.id.tvTransportVehicleInfo
            )

        val tvRating =
            view.findViewById<TextView>(
                R.id.tvTransportRating
            )

        val tvDistance =
            view.findViewById<TextView>(
                R.id.tvTransportProviderDistance
            )

        val tvPrice =
            view.findViewById<TextView>(
                R.id.tvEstimatedTransportPrice
            )

        val btnSelect =
            view.findViewById<MaterialButton>(
                R.id.btnSelectTransportProvider
            )

        tvName.text =
            providerName

        tvVehicle.text =
            if (capacityKg > 0) {

                "$vehicleType • Max ${number(capacityKg)} kg"

            } else {

                vehicleType
            }

        tvRating.text =
            "⭐ ${String.format("%.1f", rating)}"

        tvDistance.text =
            when {

                distanceKm != null ->
                    "📍 ${String.format("%.1f", distanceKm)} km"

                address.isNotBlank() ->
                    "📍 $address"

                else ->
                    "📍 Location not set"
            }

        val estimatedFee =
            calculateTransportFee(
                distanceKm
            )

        tvPrice.text =
            "Est. Rs. ${number(estimatedFee)}"

        btnSelect.setOnClickListener {

            createTransportOrder(
                providerId = providerId,
                providerName = providerName,
                transportFee = estimatedFee,
                button = btnSelect
            )
        }

        containerProviders.addView(
            view
        )
    }

    private fun createTransportOrder(
        providerId: String,
        providerName: String,
        transportFee: Double,
        button: MaterialButton
    ) {

        val buyerId =
            auth.currentUser?.uid

        if (buyerId == null) {

            Toast.makeText(
                this,
                "Please login again",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (farmerId.isBlank()) {

            Toast.makeText(
                this,
                "Farmer information unavailable",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        button.isEnabled = false
        button.text = "SENDING..."

        val productRef =
            firestore.collection("products")
                .document(productId)

        val orderRef =
            firestore.collection("orders")
                .document()

        val bookingRef =
            firestore.collection(
                "transportBookings"
            ).document()

        firestore.runTransaction { transaction ->

            val productSnapshot =
                transaction.get(productRef)

            val stock =
                productSnapshot.getDouble(
                    "quantity"
                ) ?: 0.0

            val requested =
                quantity.toDouble()

            if (stock < requested) {

                throw IllegalStateException(
                    "Only ${number(stock)} $unit available"
                )
            }

            val remaining =
                stock - requested

            transaction.update(
                productRef,
                "quantity",
                remaining
            )

            if (remaining <= 0) {

                transaction.update(
                    productRef,
                    "status",
                    "Out of Stock"
                )
            }

            val order: HashMap<String, Any?> = hashMapOf(
                "orderId" to orderRef.id,
                "buyerId" to buyerId,
                "farmerId" to farmerId,
                "productId" to productId,
                "productName" to productName,
                "quantity" to requested,
                "unit" to unit,
                "pricePerUnit" to pricePerUnit,
                "totalAmount" to totalAmount,
                "transportFee" to transportFee,
                "grandTotal" to (totalAmount + transportFee),
                "fulfillmentMethod" to "Transport",
                "orderStatus" to "Pending",
                "transportStatus" to "Requested",
                "transportProviderId" to providerId,
                "transportBookingId" to bookingRef.id,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val booking: HashMap<String, Any?> = hashMapOf(
                "bookingId" to bookingRef.id,
                "orderId" to orderRef.id,
                "buyerId" to buyerId,
                "farmerId" to farmerId,
                "providerId" to providerId,
                "providerName" to providerName,
                "productId" to productId,
                "productName" to productName,
                "quantity" to requested,
                "unit" to unit,
                "transportFee" to transportFee,
                "pickupLatitude" to farmerLatitude,
                "pickupLongitude" to farmerLongitude,
                "status" to "Requested",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            transaction.set(
                orderRef,
                order
            )

            transaction.set(
                bookingRef,
                booking
            )

            orderRef.id
        }
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Transport request sent successfully!",
                    Toast.LENGTH_LONG
                ).show()

                val intent =
                    Intent(
                        this,
                        BuyerHomeActivity::class.java
                    )

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->

                button.isEnabled = true
                button.text = "SELECT"

                Toast.makeText(
                    this,
                    exception.message
                        ?: "Unable to create transport request",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun calculateDistance(
        latitude: Double?,
        longitude: Double?
    ): Float? {

        val pickupLat =
            farmerLatitude ?: return null

        val pickupLng =
            farmerLongitude ?: return null

        if (
            latitude == null ||
            longitude == null
        ) {
            return null
        }

        val results =
            FloatArray(1)

        Location.distanceBetween(
            pickupLat,
            pickupLng,
            latitude,
            longitude,
            results
        )

        return results[0] / 1000f
    }

    private fun showNoProviders() {

        val text =
            TextView(this)

        text.text =
            "No transport providers are registered yet."

        text.setTextColor(
            Color.parseColor("#6B7280")
        )

        text.textSize = 13f

        text.setPadding(
            4,
            20,
            4,
            30
        )

        containerProviders.addView(text)
    }

    private fun calculateTransportFee(
        distanceKm: Float?
    ): Double {

        if (distanceKm == null) {
            return 500.0
        }

        val rawFee =
            300.0 + (distanceKm * 100.0)

        // Round to nearest Rs. 50
        return kotlin.math.round(
            rawFee / 50.0
        ) * 50.0
    }

    private fun number(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {
            value.toInt().toString()
        } else {
            String.format(
                "%.2f",
                value
            )
        }
    }
}