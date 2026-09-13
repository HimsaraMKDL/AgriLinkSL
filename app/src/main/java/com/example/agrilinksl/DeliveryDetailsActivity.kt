package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class DeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    private var bookingId = ""
    private var orderId = ""

    private var currentStatus = "Accepted"

    private lateinit var btnUpdate: MaterialButton

    private lateinit var cardAccepted: MaterialCardView
    private lateinit var cardOnTheWay: MaterialCardView
    private lateinit var cardPickedUp: MaterialCardView
    private lateinit var cardDelivered: MaterialCardView

    private lateinit var iconAccepted: TextView
    private lateinit var iconOnTheWay: TextView
    private lateinit var iconPickedUp: TextView
    private lateinit var iconDelivered: TextView

    private lateinit var tvAcceptedStatus: TextView
    private lateinit var tvOnTheWayStatus: TextView
    private lateinit var tvPickedUpStatus: TextView
    private lateinit var tvDeliveredStatus: TextView

    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_details)

        firestore = FirebaseFirestore.getInstance()

        bookingId =
            intent.getStringExtra("BOOKING_ID") ?: ""

        if (bookingId.isBlank()) {
            finish()
            return
        }

        initializeViews()

        findViewById<TextView>(
            R.id.tvBackDelivery
        ).setOnClickListener {
            finish()
        }

        btnUpdate.setOnClickListener {

            if (currentStatus == "Delivered") {

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

            } else {

                updateToNextStatus()
            }
        }

        listenToBooking()
    }

    override fun onDestroy() {

        listener?.remove()

        super.onDestroy()
    }

    private fun initializeViews() {

        btnUpdate =
            findViewById(R.id.btnDeliveryUpdateStatus)

        cardAccepted =
            findViewById(R.id.cardStatusAccepted)

        cardOnTheWay =
            findViewById(R.id.cardStatusOnTheWay)

        cardPickedUp =
            findViewById(R.id.cardStatusPickedUp)

        cardDelivered =
            findViewById(R.id.cardStatusDelivered)

        iconAccepted =
            findViewById(R.id.iconStatusAccepted)

        iconOnTheWay =
            findViewById(R.id.iconStatusOnTheWay)

        iconPickedUp =
            findViewById(R.id.iconStatusPickedUp)

        iconDelivered =
            findViewById(R.id.iconStatusDelivered)

        tvAcceptedStatus =
            findViewById(R.id.tvAcceptedStatus)

        tvOnTheWayStatus =
            findViewById(R.id.tvOnTheWayStatus)

        tvPickedUpStatus =
            findViewById(R.id.tvPickedUpStatus)

        tvDeliveredStatus =
            findViewById(R.id.tvDeliveredStatus)
    }

    private fun listenToBooking() {

        listener =
            firestore.collection("transportBookings")
                .document(bookingId)
                .addSnapshotListener { document, error ->

                    if (error != null) {

                        Toast.makeText(
                            this,
                            "Unable to load delivery: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }

                    if (
                        document == null ||
                        !document.exists()
                    ) {
                        return@addSnapshotListener
                    }

                    orderId =
                        document.getString("orderId") ?: ""

                    currentStatus =
                        document.getString("status")
                            ?: "Accepted"

                    val farmerId =
                        document.getString("farmerId") ?: ""

                    val buyerId =
                        document.getString("buyerId") ?: ""

                    val productName =
                        document.getString("productName")
                            ?: "Product"

                    val quantity =
                        document.getDouble("quantity")
                            ?: 0.0

                    val unit =
                        document.getString("unit")
                            ?: "kg"

                    findViewById<TextView>(
                        R.id.tvBackDelivery
                    ).text =
                        "← Delivery #${bookingId.takeLast(5).uppercase()}"

                    findViewById<TextView>(
                        R.id.tvDeliveryProductInfo
                    ).text =
                        "🌿 $productName • ${number(quantity)} $unit"

                    loadFarmer(farmerId)
                    loadBuyer(buyerId)

                    updateStatusUI(
                        document.getTimestamp("acceptedAt"),
                        document.getTimestamp("onTheWayAt"),
                        document.getTimestamp("pickedUpAt"),
                        document.getTimestamp("deliveredAt")
                    )
                }
    }

    private fun loadFarmer(
        farmerId: String
    ) {

        if (farmerId.isBlank()) {
            return
        }

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener { document ->

                val name =
                    document.getString("name")
                        ?: "Farmer"

                val farmName =
                    document.getString("farmName")
                        ?: ""

                val address =
                    document.getString("address")
                        ?: ""

                findViewById<TextView>(
                    R.id.tvDeliveryFarmerName
                ).text =
                    farmName.ifBlank {
                        name
                    }

                findViewById<TextView>(
                    R.id.tvDeliveryPickup
                ).text =
                    if (address.isBlank()) {
                        "Pickup Point"
                    } else {
                        "Pickup Point • $address"
                    }
            }
    }

    private fun loadBuyer(
        buyerId: String
    ) {

        if (buyerId.isBlank()) {
            return
        }

        firestore.collection("users")
            .document(buyerId)
            .get()
            .addOnSuccessListener { document ->

                val name =
                    document.getString("name")
                        ?: "Buyer"

                val address =
                    document.getString("address")
                        ?: ""

                findViewById<TextView>(
                    R.id.tvDeliveryBuyerName
                ).text =
                    "Buyer Location ($name)"

                findViewById<TextView>(
                    R.id.tvDeliveryDestination
                ).text =
                    if (address.isBlank()) {
                        "Delivery Point"
                    } else {
                        "Delivery Point • $address"
                    }
            }
    }

    private fun updateStatusUI(
        acceptedAt: Timestamp?,
        onTheWayAt: Timestamp?,
        pickedUpAt: Timestamp?,
        deliveredAt: Timestamp?
    ) {

        resetStatusCards()

        when (currentStatus) {

            "Accepted" -> {

                completeStatus(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedStatus,
                    acceptedAt
                )

                btnUpdate.text =
                    "START JOURNEY"
            }

            "On the Way" -> {

                completeStatus(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedStatus,
                    acceptedAt
                )

                completeStatus(
                    cardOnTheWay,
                    iconOnTheWay,
                    tvOnTheWayStatus,
                    onTheWayAt
                )

                btnUpdate.text =
                    "MARK AS PICKED UP"
            }

            "Picked Up" -> {

                completeStatus(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedStatus,
                    acceptedAt
                )

                completeStatus(
                    cardOnTheWay,
                    iconOnTheWay,
                    tvOnTheWayStatus,
                    onTheWayAt
                )

                completeStatus(
                    cardPickedUp,
                    iconPickedUp,
                    tvPickedUpStatus,
                    pickedUpAt
                )

                btnUpdate.text =
                    "MARK AS DELIVERED"
            }

            "Delivered" -> {

                completeStatus(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedStatus,
                    acceptedAt
                )

                completeStatus(
                    cardOnTheWay,
                    iconOnTheWay,
                    tvOnTheWayStatus,
                    onTheWayAt
                )

                completeStatus(
                    cardPickedUp,
                    iconPickedUp,
                    tvPickedUpStatus,
                    pickedUpAt
                )

                completeStatus(
                    cardDelivered,
                    iconDelivered,
                    tvDeliveredStatus,
                    deliveredAt
                )

                btnUpdate.text =
                    "DELIVERY COMPLETED"

                btnUpdate.isEnabled =
                    true

                findViewById<TextView>(
                    R.id.tvDeliveryTopStatus
                ).apply {

                    text =
                        "✓ Completed"

                    setTextColor(
                        "#2E7D32".toColorInt()
                    )
                }
            }

            // Support old test data
            "In Transit" -> {

                completeStatus(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedStatus,
                    acceptedAt
                )

                completeStatus(
                    cardOnTheWay,
                    iconOnTheWay,
                    tvOnTheWayStatus,
                    onTheWayAt
                )

                completeStatus(
                    cardPickedUp,
                    iconPickedUp,
                    tvPickedUpStatus,
                    pickedUpAt
                )

                btnUpdate.text =
                    "MARK AS DELIVERED"
            }
        }
    }

    private fun resetStatusCards() {

        btnUpdate.isEnabled =
            true

        findViewById<TextView>(
            R.id.tvDeliveryTopStatus
        ).text =
            "● In Progress"

        pendingStatus(
            cardAccepted,
            iconAccepted,
            tvAcceptedStatus
        )

        pendingStatus(
            cardOnTheWay,
            iconOnTheWay,
            tvOnTheWayStatus
        )

        pendingStatus(
            cardPickedUp,
            iconPickedUp,
            tvPickedUpStatus
        )

        pendingStatus(
            cardDelivered,
            iconDelivered,
            tvDeliveredStatus
        )
    }

    private fun pendingStatus(
        card: MaterialCardView,
        icon: TextView,
        statusText: TextView
    ) {

        card.setCardBackgroundColor(
            "#FFFFFF".toColorInt()
        )

        card.strokeColor =
            "#E5E7EB".toColorInt()

        icon.text =
            "○"

        icon.setTextColor(
            "#CBD5E1".toColorInt()
        )

        statusText.text =
            "Pending"

        statusText.setTextColor(
            "#6B7280".toColorInt()
        )
    }

    private fun completeStatus(
        card: MaterialCardView,
        icon: TextView,
        statusText: TextView,
        timestamp: Timestamp?
    ) {

        card.setCardBackgroundColor(
            "#EAF6EA".toColorInt()
        )

        card.strokeColor =
            "#A5D6A7".toColorInt()

        icon.text =
            "●"

        icon.setTextColor(
            "#2E7D32".toColorInt()
        )

        statusText.text =
            timestamp?.let {
                formatTime(it)
            } ?: "Done"

        statusText.setTextColor(
            "#2E7D32".toColorInt()
        )
    }

    private fun updateToNextStatus() {

        val newStatus =
            when (currentStatus) {

                "Accepted" ->
                    "On the Way"

                "On the Way" ->
                    "Picked Up"

                "Picked Up",
                "In Transit" ->
                    "Delivered"

                else ->
                    return
            }

        btnUpdate.isEnabled =
            false

        /*
         * Explicit variable type is declared here.
         * This removes the "Explicit type arguments can be inferred"
         * warning that was appearing around line 548.
         */
        val bookingUpdates: MutableMap<String, Any> =
            mutableMapOf(
                "status" to newStatus,
                "updatedAt" to
                        FieldValue.serverTimestamp()
            )

        when (newStatus) {

            "On the Way" -> {

                bookingUpdates["onTheWayAt"] =
                    FieldValue.serverTimestamp()
            }

            "Picked Up" -> {

                bookingUpdates["pickedUpAt"] =
                    FieldValue.serverTimestamp()
            }

            "Delivered" -> {

                bookingUpdates["deliveredAt"] =
                    FieldValue.serverTimestamp()
            }
        }

        val bookingRef =
            firestore.collection(
                "transportBookings"
            ).document(bookingId)

        firestore.runBatch { batch ->

            batch.update(
                bookingRef,
                bookingUpdates
            )

            if (orderId.isNotBlank()) {

                val orderRef =
                    firestore.collection("orders")
                        .document(orderId)

                /*
                 * Same fix here for the warning that was
                 * appearing around line 588.
                 */
                val orderUpdates: MutableMap<String, Any> =
                    mutableMapOf(
                        "transportStatus" to newStatus,
                        "updatedAt" to
                                FieldValue.serverTimestamp()
                    )

                if (newStatus == "Delivered") {

                    orderUpdates["orderStatus"] =
                        "Delivered"
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

                btnUpdate.isEnabled =
                    true

                Toast.makeText(
                    this,
                    "Update failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun formatTime(
        timestamp: Timestamp
    ): String {

        val formatter =
            SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
            )

        return formatter.format(
            timestamp.toDate()
        )
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
                Locale.getDefault(),
                "%.2f",
                value
            )
        }
    }
}