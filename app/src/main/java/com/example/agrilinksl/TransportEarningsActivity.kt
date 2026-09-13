package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class TransportEarningsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvTotal: TextView
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvTrips: TextView
    private lateinit var tvAverage: TextView
    private lateinit var containerHistory: LinearLayout

    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transport_earnings)

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        tvTotal =
            findViewById(R.id.tvEarningsTotal)

        tvCompletedCount =
            findViewById(R.id.tvEarningsCompletedCount)

        tvTrips =
            findViewById(R.id.tvEarningsTrips)

        tvAverage =
            findViewById(R.id.tvAverageEarning)

        containerHistory =
            findViewById(R.id.containerEarningsHistory)

        setupBottomNavigation()
        listenToEarnings()
    }

    override fun onDestroy() {

        listener?.remove()

        super.onDestroy()
    }

    private fun listenToEarnings() {

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
                            "Unable to load earnings: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }

                    containerHistory.removeAllViews()

                    if (snapshot == null) {
                        updateSummary(
                            0,
                            0.0
                        )
                        return@addSnapshotListener
                    }

                    val delivered =
                        snapshot.documents
                            .filter {
                                it.getString("status") ==
                                        "Delivered"
                            }
                            .sortedByDescending {
                                it.getTimestamp("deliveredAt")
                                    ?.seconds
                                    ?: 0L
                            }

                    var totalEarnings =
                        0.0

                    for (document in delivered) {

                        val transportFee =
                            document.getDouble(
                                "transportFee"
                            ) ?: 0.0

                        totalEarnings +=
                            transportFee

                        val productName =
                            document.getString(
                                "productName"
                            ) ?: "Product"

                        val quantity =
                            document.getDouble(
                                "quantity"
                            ) ?: 0.0

                        val unit =
                            document.getString(
                                "unit"
                            ) ?: "kg"

                        val deliveredAt =
                            document.getTimestamp(
                                "deliveredAt"
                            )

                        addHistoryItem(
                            bookingId =
                                document.id,
                            productName =
                                productName,
                            quantity =
                                quantity,
                            unit =
                                unit,
                            transportFee =
                                transportFee,
                            deliveredAt =
                                deliveredAt
                        )
                    }

                    updateSummary(
                        delivered.size,
                        totalEarnings
                    )

                    if (delivered.isEmpty()) {
                        showEmptyMessage()
                    }
                }
    }

    private fun updateSummary(
        completedTrips: Int,
        totalEarnings: Double
    ) {

        tvTotal.text =
            "Rs. ${number(totalEarnings)}"

        tvCompletedCount.text =
            "$completedTrips completed deliveries"

        tvTrips.text =
            "$completedTrips Trips"

        val average =
            if (completedTrips > 0) {
                totalEarnings /
                        completedTrips
            } else {
                0.0
            }

        tvAverage.text =
            "Avg. Rs. ${number(average)}"
    }

    private fun addHistoryItem(
        bookingId: String,
        productName: String,
        quantity: Double,
        unit: String,
        transportFee: Double,
        deliveredAt: Timestamp?
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_transport_earning,
                    containerHistory,
                    false
                )

        view.findViewById<TextView>(
            R.id.tvEarningDeliveryId
        ).text =
            "Delivery #${bookingId.takeLast(6).uppercase()}"

        view.findViewById<TextView>(
            R.id.tvEarningAmount
        ).text =
            "Rs. ${number(transportFee)}"

        view.findViewById<TextView>(
            R.id.tvEarningProduct
        ).text =
            "🌿 $productName • ${number(quantity)} $unit"

        view.findViewById<TextView>(
            R.id.tvEarningDate
        ).text =
            if (deliveredAt != null) {
                formatDate(deliveredAt)
            } else {
                "Completed"
            }

        containerHistory.addView(
            view
        )
    }

    private fun showEmptyMessage() {

        val textView =
            TextView(this)

        textView.text =
            "No completed deliveries yet."

        textView.setTextColor(
            "#6B7280".toColorInt()
        )

        textView.textSize =
            12f

        textView.setPadding(
            5,
            25,
            5,
            25
        )

        containerHistory.addView(
            textView
        )
    }

    private fun setupBottomNavigation() {

        findViewById<TextView>(
            R.id.navEarningsDashboard
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
            R.id.navEarningsRequests
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportRequestsActivity::class.java
                )
            )

            finish()
        }

        // New changes added here
        findViewById<TextView>(
            R.id.navEarningsProfile
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

    private fun formatDate(
        timestamp: Timestamp
    ): String {

        val formatter =
            SimpleDateFormat(
                "dd MMM yyyy • hh:mm a",
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