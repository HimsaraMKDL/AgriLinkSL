package com.example.agrilinksl

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class OrderSummaryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var productId: String
    private lateinit var farmerId: String

    private var productName = ""
    private var category = ""
    private var unit = "kg"

    private var quantity = 1
    private var pricePerUnit = 0.0
    private var totalAmount = 0.0

    private var selectedMethod =
        "Transport"

    private lateinit var cardPickup: MaterialCardView
    private lateinit var cardTransport: MaterialCardView

    private lateinit var tvPickupSelected: TextView
    private lateinit var tvTransportSelected: TextView

    private lateinit var btnContinue: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_summary)

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        productId =
            intent.getStringExtra("PRODUCT_ID")
                ?: ""

        quantity =
            intent.getIntExtra(
                "QUANTITY",
                1
            )

        if (productId.isBlank()) {
            finish()
            return
        }

        cardPickup =
            findViewById(R.id.cardSelfPickup)

        cardTransport =
            findViewById(R.id.cardRequestTransport)

        tvPickupSelected =
            findViewById(R.id.tvPickupSelected)

        tvTransportSelected =
            findViewById(R.id.tvTransportSelected)

        btnContinue =
            findViewById(R.id.btnOrderContinue)

        findViewById<TextView>(
            R.id.tvOrderSummaryBack
        ).setOnClickListener {
            finish()
        }

        cardPickup.setOnClickListener {

            selectedMethod =
                "Self Pickup"

            updateMethodUI()
        }

        cardTransport.setOnClickListener {

            selectedMethod =
                "Transport"

            updateMethodUI()
        }

        btnContinue.setOnClickListener {

            when (selectedMethod) {

                "Self Pickup" -> {
                    createSelfPickupOrder()
                }

                "Transport" -> {

                    val intent =
                        Intent(
                            this,
                            TransportProviderSelectionActivity::class.java
                        )

                    intent.putExtra(
                        "PRODUCT_ID",
                        productId
                    )

                    intent.putExtra(
                        "QUANTITY",
                        quantity
                    )

                    startActivity(intent)
                }
            }
        }

        updateMethodUI()
        loadProduct()
    }

    private fun loadProduct() {

        firestore.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Product not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                productName =
                    document.getString("name")
                        ?: "Product"

                category =
                    document.getString("category")
                        ?: ""

                pricePerUnit =
                    document.getDouble("price")
                        ?: 0.0

                unit =
                    document.getString("unit")
                        ?: "kg"

                farmerId =
                    document.getString("farmerId")
                        ?: ""

                totalAmount =
                    pricePerUnit * quantity

                findViewById<TextView>(
                    R.id.tvSummaryEmoji
                ).text =
                    productEmoji(
                        productName,
                        category
                    )

                findViewById<TextView>(
                    R.id.tvSummaryProductName
                ).text =
                    productName

                findViewById<TextView>(
                    R.id.tvSummaryCalculation
                ).text =
                    "$quantity $unit × Rs. ${number(pricePerUnit)}"

                findViewById<TextView>(
                    R.id.tvSummaryItemTotal
                ).text =
                    "Rs. ${number(totalAmount)}"

                findViewById<TextView>(
                    R.id.tvSummaryTotal
                ).text =
                    "Rs. ${number(totalAmount)}"

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
            return
        }

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener { document ->

                val farmerName =
                    document.getString("name")
                        ?: "Farmer"

                findViewById<TextView>(
                    R.id.tvPickupFarmer
                ).text =
                    "Pick up directly from $farmerName"
            }
    }

    private fun createSelfPickupOrder() {

        val buyerId =
            auth.currentUser?.uid

        if (buyerId == null) {

            Toast.makeText(
                this,
                "Please login again",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (farmerId.isBlank()) {

            Toast.makeText(
                this,
                "Farmer information unavailable",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        btnContinue.isEnabled =
            false

        btnContinue.text =
            "PLACING ORDER..."

        val productRef =
            firestore.collection("products")
                .document(productId)

        val orderRef =
            firestore.collection("orders")
                .document()

        firestore.runTransaction { transaction ->

            val productSnapshot =
                transaction.get(productRef)

            val currentQuantity =
                productSnapshot.getDouble("quantity")
                    ?: 0.0

            val requestedQuantity =
                quantity.toDouble()

            if (currentQuantity < requestedQuantity) {

                throw IllegalStateException(
                    "Only ${number(currentQuantity)} $unit available"
                )
            }

            val remainingQuantity =
                currentQuantity -
                        requestedQuantity

            /*
             * Update available product stock.
             */
            transaction.update(
                productRef,
                "quantity",
                remainingQuantity
            )

            /*
             * If stock reaches zero, mark product unavailable.
             */
            if (remainingQuantity <= 0) {

                transaction.update(
                    productRef,
                    "status",
                    "Out of Stock"
                )
            }

            val order =
                hashMapOf<String, Any?>(
                    "orderId" to orderRef.id,
                    "buyerId" to buyerId,
                    "farmerId" to farmerId,
                    "productId" to productId,
                    "productName" to productName,
                    "quantity" to requestedQuantity,
                    "unit" to unit,
                    "pricePerUnit" to pricePerUnit,
                    "totalAmount" to totalAmount,
                    "fulfillmentMethod" to "Self Pickup",
                    "orderStatus" to "Pending",
                    "transportStatus" to "Not Required",
                    "transportProviderId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

            transaction.set(
                orderRef,
                order
            )

            orderRef.id
        }
            .addOnSuccessListener { orderId ->

                Toast.makeText(
                    this,
                    "Order placed successfully!",
                    Toast.LENGTH_LONG
                ).show()

                /*
                 * Temporary:
                 * Buyer Orders screen will be connected next.
                 */
                val intent =
                    Intent(
                        this,
                        BuyerHomeActivity::class.java
                    )

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                intent.putExtra(
                    "ORDER_ID",
                    orderId
                )

                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->

                btnContinue.isEnabled =
                    true

                btnContinue.text =
                    "CONTINUE"

                Toast.makeText(
                    this,
                    exception.message
                        ?: "Unable to place order",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateMethodUI() {

        val green =
            Color.parseColor("#2E7D32")

        val paleGreen =
            Color.parseColor("#E8F5E9")

        val white =
            Color.parseColor("#FFFFFF")

        val gray =
            Color.parseColor("#D1D5DB")

        if (selectedMethod == "Transport") {

            cardTransport.setCardBackgroundColor(
                paleGreen
            )

            cardTransport.strokeColor =
                green

            cardTransport.strokeWidth =
                2

            tvTransportSelected.text =
                "●"

            tvTransportSelected.setTextColor(
                green
            )

            cardPickup.setCardBackgroundColor(
                white
            )

            cardPickup.strokeColor =
                gray

            cardPickup.strokeWidth =
                1

            tvPickupSelected.text =
                "○"

            tvPickupSelected.setTextColor(
                gray
            )

        } else {

            cardPickup.setCardBackgroundColor(
                paleGreen
            )

            cardPickup.strokeColor =
                green

            cardPickup.strokeWidth =
                2

            tvPickupSelected.text =
                "●"

            tvPickupSelected.setTextColor(
                green
            )

            cardTransport.setCardBackgroundColor(
                white
            )

            cardTransport.strokeColor =
                gray

            cardTransport.strokeWidth =
                1

            tvTransportSelected.text =
                "○"

            tvTransportSelected.setTextColor(
                gray
            )
        }
    }

    private fun productEmoji(
        name: String,
        category: String
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

            category.equals(
                "Fruits",
                true
            ) ->
                "🍎"

            category.equals(
                "Rice",
                true
            ) ->
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
                "%.2f",
                value
            )
        }
    }
}