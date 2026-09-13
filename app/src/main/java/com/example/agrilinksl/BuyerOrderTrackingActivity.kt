package com.example.agrilinksl

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class BuyerOrderTrackingActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    private var orderId = ""
    private var bookingId = ""

    private var bookingListener: ListenerRegistration? = null

    private lateinit var cardRequested: MaterialCardView
    private lateinit var cardAccepted: MaterialCardView
    private lateinit var cardOnTheWay: MaterialCardView
    private lateinit var cardPickedUp: MaterialCardView
    private lateinit var cardDelivered: MaterialCardView

    private lateinit var iconRequested: TextView
    private lateinit var iconAccepted: TextView
    private lateinit var iconOnTheWay: TextView
    private lateinit var iconPickedUp: TextView
    private lateinit var iconDelivered: TextView

    private lateinit var tvRequestedTime: TextView
    private lateinit var tvAcceptedTime: TextView
    private lateinit var tvOnTheWayTime: TextView
    private lateinit var tvPickedUpTime: TextView
    private lateinit var tvDeliveredTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_buyer_order_tracking
        )

        firestore =
            FirebaseFirestore.getInstance()

        orderId =
            intent.getStringExtra("ORDER_ID") ?: ""

        bookingId =
            intent.getStringExtra("BOOKING_ID") ?: ""

        if (orderId.isBlank()) {
            finish()
            return
        }

        initializeViews()

        findViewById<TextView>(
            R.id.tvBuyerTrackingBack
        ).setOnClickListener {
            finish()
        }

        loadOrder()
    }

    override fun onDestroy() {
        bookingListener?.remove()
        super.onDestroy()
    }

    private fun initializeViews() {

        cardRequested =
            findViewById(R.id.cardBuyerStatusRequested)

        cardAccepted =
            findViewById(R.id.cardBuyerStatusAccepted)

        cardOnTheWay =
            findViewById(R.id.cardBuyerStatusOnTheWay)

        cardPickedUp =
            findViewById(R.id.cardBuyerStatusPickedUp)

        cardDelivered =
            findViewById(R.id.cardBuyerStatusDelivered)

        iconRequested =
            findViewById(R.id.iconBuyerRequested)

        iconAccepted =
            findViewById(R.id.iconBuyerAccepted)

        iconOnTheWay =
            findViewById(R.id.iconBuyerOnTheWay)

        iconPickedUp =
            findViewById(R.id.iconBuyerPickedUp)

        iconDelivered =
            findViewById(R.id.iconBuyerDelivered)

        tvRequestedTime =
            findViewById(R.id.tvBuyerRequestedTime)

        tvAcceptedTime =
            findViewById(R.id.tvBuyerAcceptedTime)

        tvOnTheWayTime =
            findViewById(R.id.tvBuyerOnTheWayTime)

        tvPickedUpTime =
            findViewById(R.id.tvBuyerPickedUpTime)

        tvDeliveredTime =
            findViewById(R.id.tvBuyerDeliveredTime)
    }

    private fun loadOrder() {

        firestore.collection("orders")
            .document(orderId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Order not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                val productName =
                    document.getString("productName")
                        ?: "Product"

                val quantity =
                    document.getDouble("quantity")
                        ?: 0.0

                val unit =
                    document.getString("unit")
                        ?: "kg"

                val farmerId =
                    document.getString("farmerId")
                        ?: ""

                val totalAmount =
                    document.getDouble("totalAmount")
                        ?: 0.0

                val transportFee =
                    document.getDouble("transportFee")
                        ?: 0.0

                val grandTotal =
                    document.getDouble("grandTotal")
                        ?: (totalAmount + transportFee)

                val providerId =
                    document.getString(
                        "transportProviderId"
                    ) ?: ""

                if (bookingId.isBlank()) {

                    bookingId =
                        document.getString(
                            "transportBookingId"
                        ) ?: ""
                }

                findViewById<TextView>(
                    R.id.tvBuyerTrackingOrderId
                ).text =
                    "Order #${orderId.takeLast(6).uppercase()}"

                findViewById<TextView>(
                    R.id.tvBuyerTrackingProduct
                ).text =
                    productName

                findViewById<TextView>(
                    R.id.tvBuyerTrackingEmoji
                ).text =
                    productEmoji(productName)

                findViewById<TextView>(
                    R.id.tvBuyerTrackingQuantity
                ).text =
                    "${number(quantity)} $unit"

                findViewById<TextView>(
                    R.id.tvBuyerTrackingTotal
                ).text =
                    "Rs. ${number(grandTotal)}"

                findViewById<TextView>(
                    R.id.tvBuyerTrackingFee
                ).text =
                    "Rs. ${number(transportFee)}"

                loadFarmerName(farmerId)
                loadProviderName(providerId)

                if (bookingId.isNotBlank()) {

                    listenToBooking()

                } else {

                    findViewById<TextView>(
                        R.id.tvBuyerTrackingTopStatus
                    ).text =
                        "Waiting"
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load order: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadFarmerName(
        farmerId: String
    ) {

        if (farmerId.isBlank()) {
            return
        }

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener {

                val farmName =
                    it.getString("farmName")
                        ?: ""

                val name =
                    it.getString("name")
                        ?: "Farmer"

                findViewById<TextView>(
                    R.id.tvBuyerTrackingFarmer
                ).text =
                    "From ${farmName.ifBlank { name }}"
            }
    }

    private fun loadProviderName(
        providerId: String
    ) {

        if (providerId.isBlank()) {

            findViewById<TextView>(
                R.id.tvBuyerTrackingProvider
            ).text =
                "Waiting for provider"

            return
        }

        firestore.collection("users")
            .document(providerId)
            .get()
            .addOnSuccessListener {

                val name =
                    it.getString("name")
                        ?: "Transport Provider"

                val vehicle =
                    it.getString("vehicleType")
                        ?: ""

                findViewById<TextView>(
                    R.id.tvBuyerTrackingProvider
                ).text =
                    if (vehicle.isBlank()) {
                        name
                    } else {
                        "$name • $vehicle"
                    }
            }
    }

    private fun listenToBooking() {

        bookingListener?.remove()

        bookingListener =
            firestore.collection("transportBookings")
                .document(bookingId)
                .addSnapshotListener {
                        document,
                        error ->

                    if (error != null) {

                        Toast.makeText(
                            this,
                            "Tracking update failed: ${error.message}",
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

                    val status =
                        document.getString("status")
                            ?: "Requested"

                    updateStatusUI(
                        status = status,
                        requestedAt =
                            document.getTimestamp("createdAt"),
                        acceptedAt =
                            document.getTimestamp("acceptedAt"),
                        onTheWayAt =
                            document.getTimestamp("onTheWayAt"),
                        pickedUpAt =
                            document.getTimestamp("pickedUpAt"),
                        deliveredAt =
                            document.getTimestamp("deliveredAt")
                    )
                }
    }

    private fun updateStatusUI(
        status: String,
        requestedAt: Timestamp?,
        acceptedAt: Timestamp?,
        onTheWayAt: Timestamp?,
        pickedUpAt: Timestamp?,
        deliveredAt: Timestamp?
    ) {

        resetTimeline()

        val topStatus =
            findViewById<TextView>(
                R.id.tvBuyerTrackingTopStatus
            )

        topStatus.text =
            status

        when (status) {

            "Requested" -> {

                completeTimelineItem(
                    cardRequested,
                    iconRequested,
                    tvRequestedTime,
                    requestedAt
                )
            }

            "Accepted" -> {

                completeTimelineItem(
                    cardRequested,
                    iconRequested,
                    tvRequestedTime,
                    requestedAt
                )

                completeTimelineItem(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedTime,
                    acceptedAt
                )
            }

            "On the Way" -> {

                completeTimelineItem(
                    cardRequested,
                    iconRequested,
                    tvRequestedTime,
                    requestedAt
                )

                completeTimelineItem(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedTime,
                    acceptedAt
                )

                completeTimelineItem(
                    cardOnTheWay,
                    iconOnTheWay,
                    tvOnTheWayTime,
                    onTheWayAt
                )
            }

            "Picked Up" -> {

                completeTimelineItem(
                    cardRequested,
                    iconRequested,
                    tvRequestedTime,
                    requestedAt
                )

                completeTimelineItem(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedTime,
                    acceptedAt
                )

                completeTimelineItem(
                    cardOnTheWay,
                    iconOnTheWay,
                    tvOnTheWayTime,
                    onTheWayAt
                )

                completeTimelineItem(
                    cardPickedUp,
                    iconPickedUp,
                    tvPickedUpTime,
                    pickedUpAt
                )
            }

            "In Transit" -> {

                completeTimelineItem(
                    cardRequested,
                    iconRequested,
                    tvRequestedTime,
                    requestedAt
                )

                completeTimelineItem(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedTime,
                    acceptedAt
                )

                completeTimelineItem(
                    cardOnTheWay,
                    iconOnTheWay,
                    tvOnTheWayTime,
                    onTheWayAt
                )

                completeTimelineItem(
                    cardPickedUp,
                    iconPickedUp,
                    tvPickedUpTime,
                    pickedUpAt
                )
            }

            "Delivered" -> {

                completeTimelineItem(
                    cardRequested,
                    iconRequested,
                    tvRequestedTime,
                    requestedAt
                )

                completeTimelineItem(
                    cardAccepted,
                    iconAccepted,
                    tvAcceptedTime,
                    acceptedAt
                )

                completeTimelineItem(
                    cardOnTheWay,
                    iconOnTheWay,
                    tvOnTheWayTime,
                    onTheWayAt
                )

                completeTimelineItem(
                    cardPickedUp,
                    iconPickedUp,
                    tvPickedUpTime,
                    pickedUpAt
                )

                completeTimelineItem(
                    cardDelivered,
                    iconDelivered,
                    tvDeliveredTime,
                    deliveredAt
                )

                topStatus.text =
                    "✓ Delivered"

                topStatus.setTextColor(
                    "#2E7D32".toColorInt()
                )
            }

            "Rejected" -> {

                topStatus.text =
                    "Rejected"

                topStatus.setTextColor(
                    "#D32F2F".toColorInt()
                )

                completeTimelineItem(
                    cardRequested,
                    iconRequested,
                    tvRequestedTime,
                    requestedAt
                )
            }
        }
    }

    private fun resetTimeline() {

        pendingTimelineItem(
            cardRequested,
            iconRequested,
            tvRequestedTime
        )

        pendingTimelineItem(
            cardAccepted,
            iconAccepted,
            tvAcceptedTime
        )

        pendingTimelineItem(
            cardOnTheWay,
            iconOnTheWay,
            tvOnTheWayTime
        )

        pendingTimelineItem(
            cardPickedUp,
            iconPickedUp,
            tvPickedUpTime
        )

        pendingTimelineItem(
            cardDelivered,
            iconDelivered,
            tvDeliveredTime
        )
    }

    private fun pendingTimelineItem(
        card: MaterialCardView,
        icon: TextView,
        text: TextView
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

        text.text =
            "Pending"

        text.setTextColor(
            "#6B7280".toColorInt()
        )
    }

    private fun completeTimelineItem(
        card: MaterialCardView,
        icon: TextView,
        text: TextView,
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

        text.text =
            timestamp?.let {
                formatTime(it)
            } ?: "Done"

        text.setTextColor(
            "#2E7D32".toColorInt()
        )
    }

    private fun formatTime(
        timestamp: Timestamp
    ): String {

        val formatter =
            SimpleDateFormat(
                "dd MMM • hh:mm a",
                Locale.getDefault()
            )

        return formatter.format(
            timestamp.toDate()
        )
    }

    private fun productEmoji(
        name: String
    ): String {

        val value =
            name.lowercase()

        return when {

            "tomato" in value ->
                "🍅"

            "carrot" in value ->
                "🥕"

            "cucumber" in value ->
                "🥒"

            "onion" in value ->
                "🧅"

            "chili" in value ||
                    "chilli" in value ->
                "🌶️"

            "rice" in value ->
                "🌾"

            else ->
                "🌱"
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
                Locale.getDefault(),
                "%.2f",
                value
            )
        }
    }
}