package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class FarmerHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvFarmerGreeting: TextView
    private lateinit var tvFarmerLocation: TextView
    private lateinit var containerProducts: LinearLayout

    // Recent Order Variables
    private lateinit var cardRecentOrder: MaterialCardView
    private lateinit var tvRecentBuyerName: TextView
    private lateinit var tvRecentOrderInfo: TextView
    private lateinit var tvRecentOrderStatus: TextView
    private lateinit var tvNoRecentOrders: TextView
    private var recentOrderListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farmer_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        tvFarmerGreeting = findViewById(R.id.tvFarmerGreeting)
        tvFarmerLocation = findViewById(R.id.tvFarmerLocation)
        containerProducts = findViewById(R.id.containerProducts)

        // Initialize Recent Order Views
        cardRecentOrder = findViewById(R.id.cardRecentOrder)
        tvRecentBuyerName = findViewById(R.id.tvRecentBuyerName)
        tvRecentOrderInfo = findViewById(R.id.tvRecentOrderInfo)
        tvRecentOrderStatus = findViewById(R.id.tvRecentOrderStatus)
        tvNoRecentOrders = findViewById(R.id.tvNoRecentOrders)

        val btnAddProduct = findViewById<MaterialButton>(R.id.btnAddProduct)

        btnAddProduct.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    AddProductActivity::class.java
                )
            )
        }

        // Open Farmer Orders from Quick Action button
        findViewById<MaterialButton>(R.id.btnOrders).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    FarmerOrdersActivity::class.java
                )
            )
        }

        // Open Farmer Orders from Bottom Navigation
        findViewById<LinearLayout>(R.id.navOrders).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    FarmerOrdersActivity::class.java
                )
            )
        }

        // Open Farmer Orders from Recent Order card
        cardRecentOrder.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    FarmerOrdersActivity::class.java
                )
            )
        }

        // Connect View All Products
        findViewById<TextView>(R.id.tvViewAllProducts).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    FarmerProductsActivity::class.java
                )
            )
        }

        // Connect Bottom Navigation Products Tab
        findViewById<LinearLayout>(R.id.navProducts).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    FarmerProductsActivity::class.java
                )
            )
        }

        // Connect Bottom Navigation Profile Tab
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            val intent = Intent(
                this,
                FarmerProfileActivity::class.java
            )
            intent.putExtra(
                "FARMER_ID",
                auth.currentUser?.uid
            )
            startActivity(intent)
        }

        loadFarmerProfile()
        loadFarmerProducts()
        listenToRecentOrder()
    }

    override fun onResume() {
        super.onResume()
        // Reload products when returning from Add Product screen
        loadFarmerProducts()
    }

    override fun onDestroy() {
        recentOrderListener?.remove()
        super.onDestroy()
    }

    private fun loadFarmerProfile() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("name") ?: "Farmer"
                    val firstName = fullName.trim().split(" ").firstOrNull() ?: "Farmer"
                    val formattedName = firstName.replaceFirstChar { it.uppercase() }

                    tvFarmerGreeting.text = "Good Morning, $formattedName 👋"

                    val address = document.getString("address") ?: ""

                    if (address.isNotBlank()) {
                        tvFarmerLocation.text = "📍 $address"
                    } else {
                        tvFarmerLocation.text = "📍 Location not set"
                    }
                } else {
                    Toast.makeText(this, "Farmer profile not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load profile: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadFarmerProducts() {
        val currentUser = auth.currentUser ?: return

        containerProducts.removeAllViews()

        firestore.collection("products")
            .whereEqualTo("farmerId", currentUser.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                containerProducts.removeAllViews()

                if (querySnapshot.isEmpty) {
                    showNoProductsMessage()
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    val productView = LayoutInflater.from(this)
                        .inflate(R.layout.item_farmer_product, containerProducts, false)

                    val tvEmoji = productView.findViewById<TextView>(R.id.tvProductEmoji)
                    val tvName = productView.findViewById<TextView>(R.id.tvProductName)
                    val tvPrice = productView.findViewById<TextView>(R.id.tvProductPrice)
                    val tvQuantity = productView.findViewById<TextView>(R.id.tvProductQuantity)

                    val name = document.getString("name") ?: "Product"
                    val category = document.getString("category") ?: ""
                    val price = document.getDouble("price") ?: 0.0
                    val quantity = document.getDouble("quantity") ?: 0.0
                    val unit = document.getString("unit") ?: "kg"

                    tvEmoji.text = getProductEmoji(name, category)
                    tvName.text = name
                    tvPrice.text = "Rs. ${formatNumber(price)} /$unit"
                    tvQuantity.text = "${formatNumber(quantity)} $unit"

                    containerProducts.addView(productView)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load products: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showNoProductsMessage() {
        val emptyText = TextView(this)
        emptyText.text = "No products added yet.\nTap + Add Product to publish your first product."
        emptyText.setTextColor(android.graphics.Color.parseColor("#6B7280"))
        emptyText.textSize = 12f
        emptyText.setPadding(8, 25, 8, 25)

        containerProducts.addView(emptyText)
    }

    private fun getProductEmoji(name: String, category: String): String {
        val lowerName = name.lowercase()

        return when {
            "tomato" in lowerName -> "🍅"
            "carrot" in lowerName -> "🥕"
            "cucumber" in lowerName -> "🥒"
            "potato" in lowerName -> "🥔"
            "corn" in lowerName -> "🌽"
            "chili" in lowerName || "chilli" in lowerName -> "🌶️"
            "apple" in lowerName -> "🍎"
            "banana" in lowerName -> "🍌"
            "orange" in lowerName -> "🍊"
            category.equals("Fruits", ignoreCase = true) -> "🍎"
            category.equals("Rice", ignoreCase = true) -> "🌾"
            category.equals("Spices", ignoreCase = true) -> "🌶️"
            category.equals("Vegetables", ignoreCase = true) -> "🥬"
            else -> "🌱"
        }
    }

    private fun listenToRecentOrder() {
        val farmerId = auth.currentUser?.uid ?: return

        recentOrderListener?.remove()

        recentOrderListener = firestore.collection("orders")
            .whereEqualTo("farmerId", farmerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load recent order: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    showNoRecentOrders()
                    return@addSnapshotListener
                }

                /*
                 * Sort locally so Firestore does not
                 * require an additional composite index.
                 */
                val latestOrder = snapshot.documents.maxByOrNull { document ->
                    document.getTimestamp("createdAt")?.seconds ?: 0L
                }

                if (latestOrder == null) {
                    showNoRecentOrders()
                    return@addSnapshotListener
                }

                cardRecentOrder.visibility = View.VISIBLE
                tvNoRecentOrders.visibility = View.GONE

                val buyerId = latestOrder.getString("buyerId") ?: ""
                val productName = latestOrder.getString("productName") ?: "Product"
                val quantity = latestOrder.getDouble("quantity") ?: 0.0
                val unit = latestOrder.getString("unit") ?: "kg"
                val totalAmount = latestOrder.getDouble("totalAmount") ?: 0.0
                val orderStatus = latestOrder.getString("orderStatus") ?: "Pending"
                val fulfillmentMethod = latestOrder.getString("fulfillmentMethod") ?: "Self Pickup"
                val transportStatus = latestOrder.getString("transportStatus") ?: ""

                tvRecentOrderInfo.text = "${formatNumber(quantity)} $unit $productName · Rs. ${formatNumber(totalAmount)}"

                val displayStatus = getRecentDisplayStatus(orderStatus, fulfillmentMethod, transportStatus)
                tvRecentOrderStatus.text = displayStatus
                updateRecentStatusStyle(displayStatus)
                loadRecentBuyerName(buyerId)
            }
    }

    private fun loadRecentBuyerName(buyerId: String) {
        if (buyerId.isBlank()) {
            tvRecentBuyerName.text = "Customer"
            return
        }

        firestore.collection("users")
            .document(buyerId)
            .get()
            .addOnSuccessListener { document ->
                tvRecentBuyerName.text = document.getString("name") ?: "Customer"
            }
            .addOnFailureListener {
                tvRecentBuyerName.text = "Customer"
            }
    }

    private fun getRecentDisplayStatus(
        orderStatus: String,
        fulfillmentMethod: String,
        transportStatus: String
    ): String {
        if (orderStatus.equals("Pending", ignoreCase = true)) {
            return "Pending"
        }

        if (orderStatus.equals("Delivered", ignoreCase = true)) {
            return "Delivered"
        }

        if (fulfillmentMethod.equals("Transport", ignoreCase = true)) {
            return when (transportStatus) {
                "Accepted" -> "Accepted"
                "On the Way" -> "On the Way"
                "Picked Up" -> "Picked Up"
                "Delivered" -> "Delivered"
                else -> orderStatus
            }
        }

        return orderStatus
    }

    private fun updateRecentStatusStyle(status: String) {
        when (status) {
            "Delivered" -> {
                tvRecentOrderStatus.setTextColor("#2E7D32".toColorInt())
                tvRecentOrderStatus.setBackgroundColor("#E8F5E9".toColorInt())
            }
            "Cancelled",
            "Rejected" -> {
                tvRecentOrderStatus.setTextColor("#D32F2F".toColorInt())
                tvRecentOrderStatus.setBackgroundColor("#FDECEC".toColorInt())
            }
            "On the Way",
            "Picked Up",
            "Accepted",
            "Confirmed",
            "Ready for Pickup" -> {
                tvRecentOrderStatus.setTextColor("#1565C0".toColorInt())
                tvRecentOrderStatus.setBackgroundColor("#EAF2FF".toColorInt())
            }
            else -> {
                tvRecentOrderStatus.setTextColor("#F59E0B".toColorInt())
                tvRecentOrderStatus.setBackgroundColor("#FFF3D6".toColorInt())
            }
        }
    }

    private fun showNoRecentOrders() {
        cardRecentOrder.visibility = View.GONE
        tvNoRecentOrders.visibility = View.VISIBLE
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", value)
        }
    }
}