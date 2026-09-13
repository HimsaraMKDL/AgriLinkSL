package com.example.agrilinksl

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TransportRequestsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var container: LinearLayout
    private lateinit var tvCount: TextView

    private lateinit var btnNew: MaterialButton
    private lateinit var btnActive: MaterialButton
    private lateinit var btnCompleted: MaterialButton

    private var selectedFilter = "New"

    private var listener: ListenerRegistration? = null

    private val allBookings =
        mutableListOf<BookingItem>()

    data class BookingItem(
        val bookingId: String,
        val orderId: String,
        val farmerId: String,
        val buyerId: String,
        val productName: String,
        val quantity: Double,
        val unit: String,
        val status: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transport_requests)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        container =
            findViewById(R.id.containerAllTransportRequests)

        tvCount =
            findViewById(R.id.tvRequestsCount)

        btnNew =
            findViewById(R.id.btnRequestsNew)

        btnActive =
            findViewById(R.id.btnRequestsActive)

        btnCompleted =
            findViewById(R.id.btnRequestsCompleted)

        setupFilters()
        setupBottomNavigation()
        listenForBookings()
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }

    private fun setupFilters() {

        btnNew.setOnClickListener {
            selectedFilter = "New"
            updateFilterUI()
            displayFilteredBookings()
        }

        btnActive.setOnClickListener {
            selectedFilter = "Active"
            updateFilterUI()
            displayFilteredBookings()
        }

        btnCompleted.setOnClickListener {
            selectedFilter = "Completed"
            updateFilterUI()
            displayFilteredBookings()
        }

        updateFilterUI()
    }

    private fun updateFilterUI() {

        val green =
            Color.parseColor("#2E7D32")

        val white =
            Color.parseColor("#FFFFFF")

        val dark =
            Color.parseColor("#1F2937")

        val buttons =
            listOf(
                "New" to btnNew,
                "Active" to btnActive,
                "Completed" to btnCompleted
            )

        for ((name, button) in buttons) {

            if (name == selectedFilter) {

                button.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        green
                    )

                button.setTextColor(white)

            } else {

                button.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        white
                    )

                button.setTextColor(dark)
            }
        }
    }

    private fun listenForBookings() {

        val providerId =
            auth.currentUser?.uid

        if (providerId == null) {
            finish()
            return
        }

        listener =
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

                    allBookings.clear()

                    if (snapshot != null) {

                        for (document in snapshot.documents) {

                            allBookings.add(
                                BookingItem(
                                    bookingId = document.id,
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
                                            ?: "kg",
                                    status =
                                        document.getString("status")
                                            ?: "Requested"
                                )
                            )
                        }
                    }

                    displayFilteredBookings()
                }
    }

    private fun displayFilteredBookings() {

        container.removeAllViews()

        val filtered =
            when (selectedFilter) {

                "New" ->
                    allBookings.filter {
                        it.status == "Requested"
                    }

                "Active" ->
                    allBookings.filter {
                        it.status == "Accepted" ||
                                it.status == "On the Way" ||
                                it.status == "Picked Up" ||
                                it.status == "In Transit"
                    }

                "Completed" ->
                    allBookings.filter {
                        it.status == "Delivered" ||
                                it.status == "Rejected"
                    }

                else ->
                    emptyList()
            }

        tvCount.text =
            "${filtered.size} Requests"

        if (filtered.isEmpty()) {
            showEmptyMessage()
            return
        }

        for (booking in filtered) {
            addBookingCard(booking)
        }
    }

    private fun addBookingCard(
        booking: BookingItem
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_transport_request_history,
                    container,
                    false
                )

        val tvId =
            view.findViewById<TextView>(
                R.id.tvHistoryDeliveryId
            )

        val tvStatus =
            view.findViewById<TextView>(
                R.id.tvHistoryStatus
            )

        val tvRoute =
            view.findViewById<TextView>(
                R.id.tvHistoryRoute
            )

        val tvProduct =
            view.findViewById<TextView>(
                R.id.tvHistoryProduct
            )

        val actionLayout =
            view.findViewById<LinearLayout>(
                R.id.layoutRequestActions
            )

        val btnAccept =
            view.findViewById<MaterialButton>(
                R.id.btnHistoryAccept
            )

        val btnReject =
            view.findViewById<MaterialButton>(
                R.id.btnHistoryReject
            )

        val btnView =
            view.findViewById<MaterialButton>(
                R.id.btnHistoryViewDelivery
            )

        tvId.text =
            "Delivery #${booking.bookingId.takeLast(6).uppercase()}"

        tvStatus.text =
            booking.status

        tvProduct.text =
            "🌿 ${booking.productName} • ${number(booking.quantity)} ${booking.unit}"

        loadRouteNames(
            booking.farmerId,
            booking.buyerId,
            tvRoute
        )

        if (booking.status == "Requested") {

            actionLayout.visibility =
                View.VISIBLE

            btnView.visibility =
                View.GONE

            btnAccept.setOnClickListener {

                btnAccept.isEnabled = false
                btnReject.isEnabled = false

                updateRequestStatus(
                    booking,
                    "Accepted"
                )
            }

            btnReject.setOnClickListener {

                btnAccept.isEnabled = false
                btnReject.isEnabled = false

                updateRequestStatus(
                    booking,
                    "Rejected"
                )
            }

        } else {

            actionLayout.visibility =
                View.GONE

            if (
                booking.status != "Rejected" &&
                booking.status != "Delivered"
            ) {

                btnView.visibility =
                    View.VISIBLE

                btnView.setOnClickListener {

                    val intent =
                        Intent(
                            this,
                            DeliveryDetailsActivity::class.java
                        )

                    intent.putExtra(
                        "BOOKING_ID",
                        booking.bookingId
                    )

                    startActivity(intent)
                }

            } else {

                btnView.visibility =
                    View.GONE
            }
        }

        container.addView(view)
    }

    private fun loadRouteNames(
        farmerId: String,
        buyerId: String,
        textView: TextView
    ) {

        var farmerName = "Farmer"
        var buyerName = "Buyer"

        fun updateText() {
            textView.text =
                "📍 $farmerName  →  🏡 $buyerName"
        }

        if (farmerId.isNotBlank()) {

            firestore.collection("users")
                .document(farmerId)
                .get()
                .addOnSuccessListener {

                    farmerName =
                        it.getString("name")
                            ?: "Farmer"

                    updateText()
                }
        }

        if (buyerId.isNotBlank()) {

            firestore.collection("users")
                .document(buyerId)
                .get()
                .addOnSuccessListener {

                    buyerName =
                        it.getString("name")
                            ?: "Buyer"

                    updateText()
                }
        }

        updateText()
    }

    private fun updateRequestStatus(
        booking: BookingItem,
        newStatus: String
    ) {

        val bookingRef =
            firestore.collection(
                "transportBookings"
            ).document(booking.bookingId)

        val orderRef =
            firestore.collection("orders")
                .document(booking.orderId)

        firestore.runBatch { batch ->

            val bookingUpdates:
                    MutableMap<String, Any> =
                mutableMapOf(
                    "status" to newStatus,
                    "updatedAt" to
                            FieldValue.serverTimestamp()
                )

            if (newStatus == "Accepted") {
                bookingUpdates["acceptedAt"] =
                    FieldValue.serverTimestamp()
            }

            batch.update(
                bookingRef,
                bookingUpdates
            )

            if (booking.orderId.isNotBlank()) {

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

                Toast.makeText(
                    this,
                    if (newStatus == "Accepted")
                        "Request accepted"
                    else
                        "Request rejected",
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

    private fun showEmptyMessage() {

        val text =
            TextView(this)

        text.text =
            when (selectedFilter) {

                "New" ->
                    "No new transport requests."

                "Active" ->
                    "No active deliveries."

                else ->
                    "No completed requests yet."
            }

        text.setTextColor(
            Color.parseColor("#6B7280")
        )

        text.textSize =
            12f

        text.setPadding(
            5,
            25,
            5,
            25
        )

        container.addView(text)
    }

    private fun setupBottomNavigation() {

        findViewById<TextView>(
            R.id.navRequestsDashboard
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    TransportDashboardActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
        }

        findViewById<TextView>(
            R.id.navRequestsEarnings
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportEarningsActivity::class.java
                )
            )

            finish()
        }

        // Added Profile Navigation Code
        findViewById<TextView>(
            R.id.navRequestsProfile
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportProfileActivity::class.java
                )
            )

            finish()
        }
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
                java.util.Locale.getDefault(),
                "%.2f",
                value
            )
        }
    }
}