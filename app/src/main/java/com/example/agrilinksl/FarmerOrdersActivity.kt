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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class FarmerOrdersActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var containerOrders: LinearLayout
    private lateinit var tvCount: TextView

    private lateinit var btnNew: MaterialButton
    private lateinit var btnActive: MaterialButton
    private lateinit var btnCompleted: MaterialButton

    private var selectedFilter = "New"

    private var ordersListener: ListenerRegistration? = null

    private val allOrders =
        mutableListOf<FarmerOrderItem>()

    data class FarmerOrderItem(
        val orderId: String,
        val buyerId: String,
        val productName: String,
        val quantity: Double,
        val unit: String,
        val totalAmount: Double,
        val fulfillmentMethod: String,
        val orderStatus: String,
        val transportStatus: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_farmer_orders
        )

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        containerOrders =
            findViewById(
                R.id.containerFarmerOrders
            )

        tvCount =
            findViewById(
                R.id.tvFarmerOrdersCount
            )

        btnNew =
            findViewById(
                R.id.btnFarmerOrdersNew
            )

        btnActive =
            findViewById(
                R.id.btnFarmerOrdersActive
            )

        btnCompleted =
            findViewById(
                R.id.btnFarmerOrdersCompleted
            )

        setupFilters()
        setupBottomNavigation()
        listenToOrders()
    }

    override fun onDestroy() {
        ordersListener?.remove()
        super.onDestroy()
    }

    private fun setupFilters() {

        btnNew.setOnClickListener {
            selectedFilter = "New"
            updateFilterUI()
            displayOrders()
        }

        btnActive.setOnClickListener {
            selectedFilter = "Active"
            updateFilterUI()
            displayOrders()
        }

        btnCompleted.setOnClickListener {
            selectedFilter = "Completed"
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

        val buttons =
            listOf(
                "New" to btnNew,
                "Active" to btnActive,
                "Completed" to btnCompleted
            )

        for ((name, button) in buttons) {

            if (name == selectedFilter) {

                button.backgroundTintList =
                    ColorStateList.valueOf(green)

                button.setTextColor(white)

            } else {

                button.backgroundTintList =
                    ColorStateList.valueOf(white)

                button.setTextColor(dark)
            }
        }
    }

    private fun listenToOrders() {

        val farmerId =
            auth.currentUser?.uid ?: return

        ordersListener =
            firestore.collection("orders")
                .whereEqualTo(
                    "farmerId",
                    farmerId
                )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {

                        Toast.makeText(
                            this,
                            "Failed to load orders: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }

                    allOrders.clear()

                    snapshot?.documents?.forEach { document ->

                        allOrders.add(
                            FarmerOrderItem(
                                orderId = document.id,

                                buyerId =
                                    document.getString("buyerId") ?: "",

                                productName =
                                    document.getString("productName") ?: "Product",

                                quantity =
                                    document.getDouble("quantity") ?: 0.0,

                                unit =
                                    document.getString("unit") ?: "kg",

                                totalAmount =
                                    document.getDouble("totalAmount") ?: 0.0,

                                fulfillmentMethod =
                                    document.getString("fulfillmentMethod")
                                        ?: "Self Pickup",

                                orderStatus =
                                    document.getString("orderStatus")
                                        ?: "Pending",

                                transportStatus =
                                    document.getString("transportStatus")
                                        ?: ""
                            )
                        )
                    }

                    displayOrders()
                }
    }

    private fun displayOrders() {

        containerOrders.removeAllViews()

        val filtered =
            when (selectedFilter) {

                "New" ->
                    allOrders.filter {
                        it.orderStatus == "Pending"
                    }

                "Active" ->
                    allOrders.filter {
                        it.orderStatus == "Confirmed" ||
                                it.orderStatus == "Ready for Pickup"
                    }

                else ->
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

        filtered.forEach {
            addOrderCard(it)
        }
    }

    private fun addOrderCard(
        order: FarmerOrderItem
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_farmer_order,
                    containerOrders,
                    false
                )

        val tvId =
            view.findViewById<TextView>(
                R.id.tvFarmerOrderId
            )

        val tvStatus =
            view.findViewById<TextView>(
                R.id.tvFarmerOrderStatus
            )

        val tvProduct =
            view.findViewById<TextView>(
                R.id.tvFarmerOrderProduct
            )

        val tvBuyer =
            view.findViewById<TextView>(
                R.id.tvFarmerOrderBuyer
            )

        val tvQuantity =
            view.findViewById<TextView>(
                R.id.tvFarmerOrderQuantity
            )

        val tvMethod =
            view.findViewById<TextView>(
                R.id.tvFarmerOrderMethod
            )

        val tvTotal =
            view.findViewById<TextView>(
                R.id.tvFarmerOrderTotal
            )

        val btnAction =
            view.findViewById<MaterialButton>(
                R.id.btnFarmerOrderAction
            )

        tvId.text =
            "Order #${order.orderId.takeLast(6).uppercase()}"

        tvStatus.text =
            order.orderStatus

        tvProduct.text =
            "${productEmoji(order.productName)} ${order.productName}"

        tvQuantity.text =
            "Quantity: ${number(order.quantity)} ${order.unit}"

        tvTotal.text =
            "Rs. ${number(order.totalAmount)}"

        tvMethod.text =
            if (order.fulfillmentMethod == "Transport") {
                "🚚 Transport • ${order.transportStatus}"
            } else {
                "🏡 Self Pickup"
            }

        loadBuyerName(
            order.buyerId,
            tvBuyer
        )

        configureAction(
            order,
            btnAction
        )

        containerOrders.addView(view)
    }

    private fun configureAction(
        order: FarmerOrderItem,
        button: MaterialButton
    ) {

        when (order.orderStatus) {

            "Pending" -> {

                button.visibility =
                    View.VISIBLE

                button.text =
                    "CONFIRM ORDER"

                button.setOnClickListener {
                    updateOrderStatus(
                        order.orderId,
                        "Confirmed"
                    )
                }
            }

            "Confirmed" -> {

                button.visibility =
                    View.VISIBLE

                button.text =
                    "MARK READY FOR PICKUP"

                button.setOnClickListener {
                    updateOrderStatus(
                        order.orderId,
                        "Ready for Pickup"
                    )
                }
            }

            "Ready for Pickup" -> {

                if (
                    order.fulfillmentMethod ==
                    "Self Pickup"
                ) {

                    button.visibility =
                        View.VISIBLE

                    button.text =
                        "MARK COMPLETED"

                    button.setOnClickListener {
                        updateOrderStatus(
                            order.orderId,
                            "Delivered"
                        )
                    }

                } else {

                    button.visibility =
                        View.GONE
                }
            }

            else -> {
                button.visibility =
                    View.GONE
            }
        }
    }

    private fun updateOrderStatus(
        orderId: String,
        status: String
    ) {

        firestore.collection("orders")
            .document(orderId)
            .update(
                mapOf(
                    "orderStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Order updated to $status",
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

    private fun loadBuyerName(
        buyerId: String,
        textView: TextView
    ) {

        if (buyerId.isBlank()) {

            textView.text =
                "Buyer: Customer"

            return
        }

        firestore.collection("users")
            .document(buyerId)
            .get()
            .addOnSuccessListener {

                val name =
                    it.getString("name")
                        ?: "Customer"

                textView.text =
                    "Buyer: $name"
            }
    }

    private fun showEmptyMessage() {

        val text =
            TextView(this)

        text.text =
            when (selectedFilter) {

                "New" ->
                    "No new orders."

                "Active" ->
                    "No active orders."

                else ->
                    "No completed orders yet."
            }

        text.setTextColor(
            "#6B7280".toColorInt()
        )

        text.textSize =
            12f

        text.setPadding(
            8,
            30,
            8,
            30
        )

        containerOrders.addView(text)
    }

    private fun setupBottomNavigation() {

        findViewById<TextView>(
            R.id.navFarmerOrdersHome
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    FarmerHomeActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
        }

        findViewById<TextView>(
            R.id.navFarmerOrdersProducts
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    FarmerProductsActivity::class.java
                )
            )

            finish()
        }

        findViewById<TextView>(
            R.id.navFarmerOrdersProfile
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    FarmerProfileActivity::class.java
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

            "tomato" in value -> "🍅"
            "carrot" in value -> "🥕"
            "cucumber" in value -> "🥒"
            "onion" in value -> "🧅"
            "potato" in value -> "🥔"
            "chili" in value ||
                    "chilli" in value -> "🌶️"
            "rice" in value -> "🌾"
            else -> "🌱"
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