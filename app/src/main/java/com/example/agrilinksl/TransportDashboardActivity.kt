package com.example.agrilinksl

import android.content.Intent
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
import com.google.firebase.firestore.ListenerRegistration

class TransportDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvGreeting: TextView
    private lateinit var tvTransportInfo: TextView
    private lateinit var tvNewRequestCount: TextView
    private lateinit var tvCompletedTrips: TextView
    private lateinit var tvTransportEarnings: TextView

    private lateinit var requestContainer: LinearLayout
    private lateinit var activeContainer: LinearLayout

    private var bookingListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transport_dashboard)

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        tvGreeting =
            findViewById(R.id.tvTransportGreeting)

        tvTransportInfo =
            findViewById(R.id.tvTransportInfo)

        tvNewRequestCount =
            findViewById(R.id.tvNewRequestCount)

        tvCompletedTrips =
            findViewById(R.id.tvCompletedTrips)

        tvTransportEarnings =
            findViewById(R.id.tvTransportEarnings)

        requestContainer =
            findViewById(R.id.containerTransportRequests)

        activeContainer =
            findViewById(R.id.containerActiveDeliveries)

        loadProviderProfile()
        listenToTransportBookings()

        findViewById<TextView>(
            R.id.navTransportRequests
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportRequestsActivity::class.java
                )
            )
        }

        // navTransportEarnings click listener
        findViewById<TextView>(
            R.id.navTransportEarnings
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportEarningsActivity::class.java
                )
            )
        }

        // Added navTransportProfile click listener here
        findViewById<TextView>(
            R.id.navTransportProfile
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportProfileActivity::class.java
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        bookingListener?.remove()
    }

    private fun loadProviderProfile() {

        val uid =
            auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                val fullName =
                    document.getString("name")
                        ?: "Transporter"

                val firstName =
                    fullName.trim()
                        .split(" ")
                        .firstOrNull()
                        ?: "Transporter"

                val vehicleType =
                    document.getString("vehicleType")
                        ?: "Transport Provider"

                val address =
                    document.getString("address")
                        ?: ""

                tvGreeting.text =
                    "Hello, $firstName 👋"

                tvTransportInfo.text =
                    if (address.isBlank()) {
                        vehicleType
                    } else {
                        "$vehicleType • $address"
                    }
            }
    }

    private fun listenToTransportBookings() {

        val providerId =
            auth.currentUser?.uid ?: return

        bookingListener =
            firestore.collection("transportBookings")
                .whereEqualTo(
                    "providerId",
                    providerId
                )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {

                        Toast.makeText(
                            this,
                            "Failed to load requests: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }

                    requestContainer.removeAllViews()
                    activeContainer.removeAllViews()

                    if (snapshot == null) {
                        return@addSnapshotListener
                    }

                    val requested =
                        snapshot.documents.filter {
                            it.getString("status") ==
                                    "Requested"
                        }

                    val active =
                        snapshot.documents.filter {

                            val status =
                                it.getString("status")

                            status == "Accepted" ||
                                    status == "On the Way" ||
                                    status == "Picked Up" ||
                                    status == "In Transit"
                        }

                    val delivered =
                        snapshot.documents.filter {
                            it.getString("status") ==
                                    "Delivered"
                        }

                    tvNewRequestCount.text =
                        "${requested.size} New"

                    tvCompletedTrips.text =
                        "${delivered.size} Deliveries"

                    /*
                     * Transport fee is not yet stored in bookings.
                     * Keep earnings at zero until pricing logic is added.
                     */
                    val earnings =
                        delivered.sumOf {
                            it.getDouble(
                                "transportFee"
                            ) ?: 0.0
                        }

                    tvTransportEarnings.text =
                        "Rs. ${number(earnings)}"

                    if (requested.isEmpty()) {
                        showNoRequests()
                    } else {

                        for (document in requested) {

                            addRequestCard(
                                bookingId =
                                    document.id,

                                orderId =
                                    document.getString("orderId")
                                        ?: "",

                                farmerId =
                                    document.getString("farmerId")
                                        ?: "",

                                buyerId =
                                    document.getString("buyerId")
                                        ?: "",

                                productName =
                                    document.getString("productName")
                                        ?: "Product",

                                quantity =
                                    document.getDouble("quantity")
                                        ?: 0.0,

                                unit =
                                    document.getString("unit")
                                        ?: "kg"
                            )
                        }
                    }

                    if (active.isEmpty()) {
                        showNoActiveDelivery()
                    } else {

                        for (document in active) {

                            addActiveDeliveryCard(
                                bookingId =
                                    document.id,

                                orderId =
                                    document.getString("orderId")
                                        ?: "",

                                farmerId =
                                    document.getString("farmerId")
                                        ?: "",

                                buyerId =
                                    document.getString("buyerId")
                                        ?: "",

                                productName =
                                    document.getString("productName")
                                        ?: "Product",

                                status =
                                    document.getString("status")
                                        ?: "Accepted"
                            )
                        }
                    }
                }
    }

    private fun addRequestCard(
        bookingId: String,
        orderId: String,
        farmerId: String,
        buyerId: String,
        productName: String,
        quantity: Double,
        unit: String
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_transport_request,
                    requestContainer,
                    false
                )

        val tvRoute =
            view.findViewById<TextView>(
                R.id.tvRequestRoute
            )

        val tvProduct =
            view.findViewById<TextView>(
                R.id.tvRequestProduct
            )

        val btnAccept =
            view.findViewById<MaterialButton>(
                R.id.btnAcceptTransportRequest
            )

        val btnReject =
            view.findViewById<MaterialButton>(
                R.id.btnRejectTransportRequest
            )

        tvProduct.text =
            "🌿 $productName • ${number(quantity)} $unit"

        loadUserNames(
            farmerId,
            buyerId,
            tvRoute
        )

        btnAccept.setOnClickListener {

            btnAccept.isEnabled = false
            btnReject.isEnabled = false

            updateBookingStatus(
                bookingId = bookingId,
                orderId = orderId,
                newStatus = "Accepted"
            )
        }

        btnReject.setOnClickListener {

            btnAccept.isEnabled = false
            btnReject.isEnabled = false

            updateBookingStatus(
                bookingId = bookingId,
                orderId = orderId,
                newStatus = "Rejected"
            )
        }

        requestContainer.addView(view)
    }

    private fun loadUserNames(
        farmerId: String,
        buyerId: String,
        textView: TextView
    ) {

        var farmerName =
            "Farmer"

        var buyerName =
            "Buyer"

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener { farmerDoc ->

                farmerName =
                    farmerDoc.getString("name")
                        ?: "Farmer"

                textView.text =
                    "📍 $farmerName  →  🏡 $buyerName"
            }

        firestore.collection("users")
            .document(buyerId)
            .get()
            .addOnSuccessListener { buyerDoc ->

                buyerName =
                    buyerDoc.getString("name")
                        ?: "Buyer"

                textView.text =
                    "📍 $farmerName  →  🏡 $buyerName"
            }
    }

    private fun updateBookingStatus(
        bookingId: String,
        orderId: String,
        newStatus: String
    ) {

        val bookingRef =
            firestore.collection(
                "transportBookings"
            ).document(bookingId)

        val orderRef =
            firestore.collection("orders")
                .document(orderId)

        firestore.runBatch { batch ->

            val bookingUpdates: MutableMap<String, Any> =
                mutableMapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

            if (newStatus == "Accepted") {
                bookingUpdates["acceptedAt"] =
                    FieldValue.serverTimestamp()
            }

            batch.update(
                bookingRef,
                bookingUpdates
            )

            if (orderId.isNotBlank()) {

                batch.update(
                    orderRef,
                    mapOf(
                        "transportStatus" to newStatus,
                        "updatedAt" to
                                FieldValue.serverTimestamp()
                    )
                )
            }
        }
            .addOnSuccessListener {

                val message =
                    if (newStatus == "Accepted") {
                        "Transport request accepted"
                    } else {
                        "Transport request rejected"
                    }

                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Update failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun addActiveDeliveryCard(
        bookingId: String,
        orderId: String,
        farmerId: String,
        buyerId: String,
        productName: String,
        status: String
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_active_delivery,
                    activeContainer,
                    false
                )

        val shortId =
            bookingId.takeLast(6)
                .uppercase()

        val tvId =
            view.findViewById<TextView>(
                R.id.tvActiveDeliveryId
            )

        val tvStatus =
            view.findViewById<TextView>(
                R.id.tvActiveDeliveryStatus
            )

        val tvInfo =
            view.findViewById<TextView>(
                R.id.tvActiveDeliveryInfo
            )

        val tvProgress =
            view.findViewById<TextView>(
                R.id.tvActiveDeliveryProgress
            )

        val btnUpdate =
            view.findViewById<MaterialButton>(
                R.id.btnUpdateDeliveryStatus
            )

        tvId.text =
            "Delivery #$shortId"

        tvStatus.text =
            "🚚 $status"

        tvInfo.text =
            productName

        loadUserNamesForDelivery(
            farmerId,
            buyerId,
            productName,
            tvInfo
        )

        when (status) {

            "Accepted" -> {
                tvProgress.text =
                    "● Pickup   ○ In Transit   ○ Delivery"
            }

            "On the Way" -> {
                tvProgress.text =
                    "● Pickup   ○ In Transit   ○ Delivery"
            }

            "Picked Up" -> {
                tvProgress.text =
                    "● Pickup   ● In Transit   ○ Delivery"
            }

            "In Transit" -> {
                tvProgress.text =
                    "● Pickup   ● In Transit   ○ Delivery"
            }
        }

        btnUpdate.text =
            "UPDATE STATUS ↻"

        btnUpdate.setOnClickListener {

            val intent =
                Intent(
                    this,
                    DeliveryDetailsActivity::class.java
                )

            intent.putExtra(
                "BOOKING_ID",
                bookingId
            )

            startActivity(intent)
        }

        activeContainer.addView(view)
    }

    private fun loadUserNamesForDelivery(
        farmerId: String,
        buyerId: String,
        productName: String,
        textView: TextView
    ) {

        var farmerName =
            "Farmer"

        var buyerName =
            "Buyer"

        fun refresh() {

            textView.text =
                "$farmerName → $buyerName • $productName"
        }

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener {

                farmerName =
                    it.getString("name")
                        ?: "Farmer"

                refresh()
            }

        firestore.collection("users")
            .document(buyerId)
            .get()
            .addOnSuccessListener {

                buyerName =
                    it.getString("name")
                        ?: "Buyer"

                refresh()
            }
    }

    private fun updateDeliveryStatus(
        bookingId: String,
        orderId: String,
        status: String
    ) {

        val bookingRef =
            firestore.collection(
                "transportBookings"
            ).document(bookingId)

        val orderRef =
            firestore.collection("orders")
                .document(orderId)

        firestore.runBatch { batch ->

            batch.update(
                bookingRef,
                mapOf(
                    "status" to status,
                    "updatedAt" to
                            FieldValue.serverTimestamp()
                )
            )

            if (orderId.isNotBlank()) {

                val orderUpdates: MutableMap<String, Any> =
                    mutableMapOf(
                        "transportStatus" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                if (status == "Delivered") {
                    orderUpdates["orderStatus"] = "Delivered"
                }

                batch.update(
                    orderRef,
                    orderUpdates
                )

                if (status == "Delivered") {

                    orderUpdates[
                        "orderStatus"
                    ] = "Delivered"
                }

                batch.update(
                    orderRef,
                    orderUpdates
                )
            }
        }
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Delivery status updated",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Status update failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun showNoRequests() {

        val text =
            TextView(this)

        text.text =
            "No new transport requests."

        text.textSize =
            12f

        text.setPadding(
            8,
            20,
            8,
            20
        )

        requestContainer.addView(text)
    }

    private fun showNoActiveDelivery() {

        val text =
            TextView(this)

        text.text =
            "No active delivery right now."

        text.textSize =
            12f

        text.setPadding(
            8,
            20,
            8,
            20
        )

        activeContainer.addView(text)
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