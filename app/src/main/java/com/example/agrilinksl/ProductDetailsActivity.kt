package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.min

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    private lateinit var productId: String
    private var farmerId: String = ""

    private var productName = ""
    private var category = ""
    private var unit = "kg"

    private var price = 0.0
    private var availableQuantity = 0.0
    private var selectedQuantity = 1

    private lateinit var tvQuantity: TextView
    private lateinit var btnOrderNow: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        firestore =
            FirebaseFirestore.getInstance()

        productId =
            intent.getStringExtra("PRODUCT_ID")
                ?: ""

        if (productId.isBlank()) {
            finish()
            return
        }

        tvQuantity =
            findViewById(R.id.tvOrderQuantity)

        btnOrderNow =
            findViewById(R.id.btnOrderNow)

        findViewById<TextView>(
            R.id.tvProductDetailsBack
        ).setOnClickListener {
            finish()
        }

        findViewById<TextView>(
            R.id.btnQuantityMinus
        ).setOnClickListener {

            if (selectedQuantity > 1) {
                selectedQuantity--
                updateQuantityText()
            }
        }

        findViewById<TextView>(
            R.id.btnQuantityPlus
        ).setOnClickListener {

            if (
                selectedQuantity <
                availableQuantity.toInt()
            ) {
                selectedQuantity++
                updateQuantityText()
            } else {
                Toast.makeText(
                    this,
                    "Maximum available quantity reached",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnOrderNow.setOnClickListener {
            openOrderSummary()
        }

        loadProduct()
    }

    private fun loadProduct() {

        firestore.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    finish()
                    return@addOnSuccessListener
                }

                productName =
                    document.getString("name")
                        ?: "Product"

                category =
                    document.getString("category")
                        ?: ""

                farmerId =
                    document.getString("farmerId")
                        ?: ""

                price =
                    document.getDouble("price")
                        ?: 0.0

                availableQuantity =
                    document.getDouble("quantity")
                        ?: 0.0

                unit =
                    document.getString("unit")
                        ?: "kg"

                val description =
                    document.getString("description")
                        ?: ""

                selectedQuantity =
                    min(
                        5,
                        availableQuantity.toInt()
                    ).coerceAtLeast(1)

                findViewById<TextView>(
                    R.id.tvDetailsProductEmoji
                ).text =
                    productEmoji(
                        productName,
                        category
                    )

                findViewById<TextView>(
                    R.id.tvDetailsProductName
                ).text =
                    productName

                findViewById<TextView>(
                    R.id.tvDetailsPrice
                ).text =
                    "Rs. ${number(price)} /$unit"

                findViewById<TextView>(
                    R.id.tvDetailsAvailable
                ).text =
                    "Available: ${number(availableQuantity)} $unit"

                findViewById<TextView>(
                    R.id.tvDetailsDescription
                ).text =
                    description

                updateQuantityText()
                loadFarmer()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    exception.message,
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
                        ?: "Local Farmer"

                val address =
                    document.getString("address")
                        ?: ""

                findViewById<TextView>(
                    R.id.tvDetailsFarmer
                ).text =
                    "🌱 $farmerName"

                findViewById<TextView>(
                    R.id.tvDetailsLocation
                ).text =
                    if (address.isBlank()) {
                        "📍 Location available"
                    } else {
                        "📍 $address"
                    }
            }
    }

    private fun updateQuantityText() {

        tvQuantity.text =
            "$selectedQuantity $unit"
    }

    private fun openOrderSummary() {

        if (availableQuantity <= 0) {

            Toast.makeText(
                this,
                "Product is currently out of stock",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(
                this,
                OrderSummaryActivity::class.java
            )

        intent.putExtra(
            "PRODUCT_ID",
            productId
        )

        intent.putExtra(
            "QUANTITY",
            selectedQuantity
        )

        startActivity(intent)
    }

    private fun productEmoji(
        name: String,
        category: String
    ): String {

        val value =
            name.lowercase()

        return when {

            "tomato" in value -> "🍅"
            "carrot" in value -> "🥕"
            "cucumber" in value -> "🥒"
            "onion" in value -> "🧅"

            category.equals(
                "Fruits",
                true
            ) -> "🍎"

            category.equals(
                "Rice",
                true
            ) -> "🌾"

            category.equals(
                "Spices",
                true
            ) -> "🌶️"

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
                "%.2f",
                value
            )
        }
    }
}