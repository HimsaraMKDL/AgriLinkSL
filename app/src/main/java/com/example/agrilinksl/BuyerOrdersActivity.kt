package com.example.agrilinksl

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class BuyerOrdersActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var containerOrders: LinearLayout
    private lateinit var tvCount: TextView

    private lateinit var btnActive: MaterialButton
    private lateinit var btnCompleted: MaterialButton

    private var selectedFilter =
        "Active"

    private var listener:
            ListenerRegistration? = null

    private val allOrders =
        mutableListOf<BuyerOrderItem>()

    data class BuyerOrderItem(
        val orderId: String,
        val farmerId: String,
        val productId: String,
        val productName: String,
        val quantity: Double,
        val unit: String,
        val totalAmount: Double,
        val transportFee: Double,
        val grandTotal: Double,
        val fulfillmentMethod: String,
        val orderStatus: String,
        val transportStatus: String,
        val bookingId: String
    )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_buyer_orders
        )

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        containerOrders =
            findViewById(
                R.id.containerBuyerOrders
            )

        tvCount =
            findViewById(
                R.id.tvBuyerOrdersCount
            )

        btnActive =
            findViewById(
                R.id.btnBuyerOrdersActive
            )

        btnCompleted =
            findViewById(
                R.id.btnBuyerOrdersCompleted
            )

        setupFilters()
        setupBottomNavigation()
        listenToBuyerOrders()
    }

    override fun onDestroy() {

        listener?.remove()

        super.onDestroy()
    }

    private fun setupFilters() {

        btnActive.setOnClickListener {

            selectedFilter =
                "Active"

            updateFilterUI()
            displayOrders()
        }

        btnCompleted.setOnClickListener {

            selectedFilter =
                "Completed"

            updateFilterUI()
            displayOrders()
        }

        updateFilterUI()
    }

    private fun updateFilterUI() {

        val green =
            "#2E7D32".toColorInt()

        val white =
            "#FFFFFF".toColorInt()

        val dark =
            "#1F2937".toColorInt()

        if (selectedFilter == "Active") {

            btnActive.backgroundTintList =
                ColorStateList.valueOf(
                    green
                )

            btnActive.setTextColor(
                white
            )

            btnCompleted.backgroundTintList =
                ColorStateList.valueOf(
                    white
                )

            btnCompleted.setTextColor(
                dark
            )

        } else {

            btnCompleted.backgroundTintList =
                ColorStateList.valueOf(
                    green
                )

            btnCompleted.setTextColor(
                white
            )

            btnActive.backgroundTintList =
                ColorStateList.valueOf(
                    white
                )

            btnActive.setTextColor(
                dark
            )
        }
    }

    private fun listenToBuyerOrders() {

        val buyerId =
            auth.currentUser?.uid

        if (buyerId == null) {

            finish()
            return
        }

        listener =
            firestore.collection("orders")
                .whereEqualTo(
                    "buyerId",
                    buyerId
                )
                .addSnapshotListener {
                        snapshot,
                        error ->

                    if (error != null) {

                        Toast.makeText(
                            this,
                            "Unable to load orders: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }

                    allOrders.clear()

                    if (snapshot != null) {

                        for (
                        document
                        in snapshot.documents
                        ) {

                            val total =
                                document.getDouble(
                                    "totalAmount"
                                ) ?: 0.0

                            val fee =
                                document.getDouble(
                                    "transportFee"
                                ) ?: 0.0

                            val grandTotal =
                                document.getDouble(
                                    "grandTotal"
                                )
                                    ?: (total + fee)

                            allOrders.add(
                                BuyerOrderItem(
                                    orderId =
                                        document.id,

                                    farmerId =
                                        document.getString(
                                            "farmerId"
                                        ) ?: "",

                                    productId =
                                        document.getString(
                                            "productId"
                                        ) ?: "",

                                    productName =
                                        document.getString(
                                            "productName"
                                        ) ?: "Product",

                                    quantity =
                                        document.getDouble(
                                            "quantity"
                                        ) ?: 0.0,

                                    unit =
                                        document.getString(
                                            "unit"
                                        ) ?: "kg",

                                    totalAmount =
                                        total,

                                    transportFee =
                                        fee,

                                    grandTotal =
                                        grandTotal,

                                    fulfillmentMethod =
                                        document.getString(
                                            "fulfillmentMethod"
                                        ) ?: "Self Pickup",

                                    orderStatus =
                                        document.getString(
                                            "orderStatus"
                                        ) ?: "Pending",

                                    transportStatus =
                                        document.getString(
                                            "transportStatus"
                                        ) ?: "Not Required",

                                    bookingId =
                                        document.getString(
                                            "transportBookingId"
                                        ) ?: ""
                                )
                            )
                        }
                    }

                    displayOrders()
                }
    }

    private fun displayOrders() {

        containerOrders.removeAllViews()

        val filtered =
            if (selectedFilter == "Active") {

                allOrders.filter {

                    it.orderStatus != "Delivered" &&
                            it.orderStatus != "Cancelled"
                }

            } else {

                allOrders.filter {

                    it.orderStatus == "Delivered" ||
                            it.orderStatus == "Cancelled"
                }
            }

        tvCount.text =
            "${filtered.size} Orders"

        if (filtered.isEmpty()) {

            showEmptyMessage()
            return
        }

        for (order in filtered) {

            addOrderCard(
                order
            )
        }
    }

    private fun addOrderCard(
        order: BuyerOrderItem
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_buyer_order,
                    containerOrders,
                    false
                )

        val tvId =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderId
            )

        val tvStatus =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderStatus
            )

        val tvEmoji =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderEmoji
            )

        val tvProduct =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderProduct
            )

        val tvQuantity =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderQuantity
            )

        val tvFarmer =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderFarmer
            )

        val tvTotal =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderTotal
            )

        val tvMethod =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderMethod
            )

        val tvTransportStatus =
            view.findViewById<TextView>(
                R.id.tvBuyerOrderTransportStatus
            )

        val btnTrack =
            view.findViewById<MaterialButton>(
                R.id.btnBuyerOrderTrack
            )

        tvId.text =
            "Order #${order.orderId.takeLast(6).uppercase()}"

        tvStatus.text =
            order.orderStatus

        tvEmoji.text =
            productEmoji(
                order.productName
            )

        tvProduct.text =
            order.productName

        tvQuantity.text =
            "${number(order.quantity)} ${order.unit}"

        tvTotal.text =
            "Rs. ${number(order.grandTotal)}"

        loadFarmerName(
            order.farmerId,
            tvFarmer
        )

        if (
            order.fulfillmentMethod ==
            "Transport"
        ) {

            tvMethod.text =
                "🚚 Transport"

            tvTransportStatus.text =
                order.transportStatus

            btnTrack.visibility =
                View.VISIBLE

            btnTrack.text =
                if (
                    order.orderStatus ==
                    "Delivered"
                ) {
                    "VIEW ORDER"
                } else {
                    "TRACK ORDER"
                }

            // Updated Section
            btnTrack.setOnClickListener {

                val intent =
                    Intent(
                        this,
                        BuyerOrderTrackingActivity::class.java
                    )

                intent.putExtra(
                    "ORDER_ID",
                    order.orderId
                )

                intent.putExtra(
                    "BOOKING_ID",
                    order.bookingId
                )

                startActivity(intent)
            }

        } else {

            tvMethod.text =
                "🏡 Self Pickup"

            tvTransportStatus.text =
                order.orderStatus

            btnTrack.visibility =
                View.VISIBLE

            btnTrack.text =
                "VIEW ORDER"

            btnTrack.setOnClickListener {

                Toast.makeText(
                    this,
                    "Order details screen is next",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        containerOrders.addView(
            view
        )
    }

    private fun loadFarmerName(
        farmerId: String,
        textView: TextView
    ) {

        if (farmerId.isBlank()) {

            textView.text =
                "From Local Farmer"

            return
        }

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener {

                val farmName =
                    it.getString("farmName")
                        ?: ""

                val farmerName =
                    it.getString("name")
                        ?: "Local Farmer"

                textView.text =
                    "From ${farmName.ifBlank { farmerName }}"
            }
    }

    private fun showEmptyMessage() {

        val textView =
            TextView(this)

        textView.text =
            if (
                selectedFilter ==
                "Active"
            ) {

                "You don't have any active orders."

            } else {

                "You don't have any completed orders yet."
            }

        textView.setTextColor(
            "#6B7280".toColorInt()
        )

        textView.textSize =
            12f

        textView.setPadding(
            5,
            30,
            5,
            30
        )

        containerOrders.addView(
            textView
        )
    }

    private fun setupBottomNavigation() {

        findViewById<TextView>(
            R.id.navBuyerOrdersHome
        ).setOnClickListener {

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

        findViewById<TextView>(
            R.id.navBuyerOrdersProfile
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BuyerProfileActivity::class.java
                )
            )

            finish()
        }
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